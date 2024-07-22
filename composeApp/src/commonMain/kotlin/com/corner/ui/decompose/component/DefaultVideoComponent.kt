package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Filter
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.catvodcore.viewmodel.GlobalModel.home
import com.corner.ui.decompose.VideoComponent
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import com.corner.util.isEmpty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class DefaultVideoComponent(componentContext: ComponentContext) : VideoComponent, ComponentContext by componentContext,
    BackHandlerOwner {

    private val _log = LoggerFactory.getLogger("Video")

    override var log: Logger = _log

    private val _model = MutableValue(VideoComponent.Model())

    override var model: MutableValue<VideoComponent.Model> = _model

    private var promptJob: Job? = null

    var isLoading = AtomicBoolean(false)

    init {
        GlobalModel.home.observe {
            homeLoad()
        }
        GlobalModel.clear.observe {
            log.debug("清空")
            clear()
        }
        GlobalModel.hotList.observe {
            if (it.isEmpty()) return@observe
            searchBarPrompt()
        }

        lifecycle.subscribe(callbacks = object : Lifecycle.Callbacks {
            override fun onCreate() {
                println("onCreate")
            }

            override fun onStart() {
                println("onStart")
//                searchBarPrompt()
//                homeLoad()
            }

            override fun onDestroy() {
                println("onDestroy")
            }

            override fun onPause() {
                println("onPause")
            }

            override fun onResume() {
                println("onResume")
            }

            override fun onStop() {
                println("onStop")
//                promptJob?.cancel()
            }
        })
    }

    override fun clear() {
        _model.update {
            it.copy(
                homeVodResult = mutableSetOf(),
                homeLoaded = false, classList = mutableSetOf(), filtersMap = mutableMapOf(),
                currentClass = null, currentFilters = listOf(),
                page = AtomicInteger(1), isRunning = false, prompt = ""
            )
        }
    }

    override fun homeLoad() {
        val home = GlobalModel.home
        if (isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            showProgress()
            try {
                if (!_model.value.homeLoaded) {
                    if (home.value.isEmpty()) return@launch
                    var list = SiteViewModel.homeContent().list.toMutableSet()
                    var classList = SiteViewModel.result.value.types.toMutableSet()
                    val filtersMap = SiteViewModel.result.value.filters
                    // 只保留site中配置的分类
                    if (home.value.categories.isNotEmpty()) {
                        val iterator = classList.iterator()
                        while (iterator.hasNext()) {
                            val next = iterator.next()
                            if (!home.value.categories.contains(next.typeName)) iterator.remove()
                        }
                    }

                    // 有首页内容
                    if (list.isNotEmpty()) {
                        classList = (mutableSetOf(Type.home()) + classList).toMutableSet()
                    } else {
                        if (classList.isEmpty()) return@launch
                        val types = classList.firstOrNull()
                        types?.selected = true
                        val rst = loadCate(types?.typeId ?: "")
                        if (!rst.isSuccess) {
                            return@launch
                        }
                        _model.value.page.addAndGet(1)
                        list = rst.list.toMutableSet()
                    }
                    val currentClass = classList.firstOrNull()
                    _model.value.homeLoaded = true
                    _model.update {
                        it.copy(
                            homeVodResult = list,
                            currentClass = currentClass,
                            classList = classList,
                            filtersMap = filtersMap
                        )
                    }
                    _model.update { it.copy(currentFilters = getFilters(currentClass!!)) }
                }
            } catch (e: Exception) {
                log.error("homeLoad", e)
            } finally {
                hideProgress()
            }
        }.invokeOnCompletion {
            isLoading.set(false)
        }
    }

    override fun clickFolder(vod: Vod) {
        showProgress()
        SiteViewModel.viewModelScope.launch {
            val result = loadCate(vod.vodId)
            model.update { it.copy(homeVodResult = result.list.toMutableSet()) }
        }.invokeOnCompletion {
            hideProgress()
        }
    }

    override fun loadMore() {
        if (model.value.currentClass == null || model.value.currentClass?.typeId == "home") return
        if ((model.value.currentClass?.failTime ?: 0) >= 2) return
        showProgress()
        if (isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            try {
                val rst = loadCate(model.value.currentClass?.typeId ?: "")
                if (!rst.isSuccess || rst.list.isEmpty()) {
                    model.value.currentClass?.failTime = model.value.currentClass?.failTime!! + 1
                    return@launch
                }
                _model.value.page.addAndGet(1)
                val list = rst.list
                // 有的源不支持分页 每次请求返回相同的数据
                if (model.value.homeVodResult.map { it.vodId }.containsAll(list.map { it.vodId })) {
                    model.value.currentClass?.failTime = model.value.currentClass?.failTime!! + 1
                    return@launch
                }
                model.value.homeVodResult.addAll(list)
                model.update { it.copy(homeVodResult = model.value.homeVodResult) }
            } finally {
            }
        }.invokeOnCompletion {
            isLoading.set(false)
            hideProgress()
        }
    }

    override fun loadCate(cate: String):Result {
        val extend = HashMap<String, String>()
        model.value.currentFilters.forEach {
            if (it.key!!.isNotBlank() && it.init.isNotBlank()) {
                extend[it.key] = it.init
            }
        }
        val rst = SiteViewModel.categoryContent(
            home.value.key,
            cate,
            _model.value.page.toString(),
            extend.isNotEmpty(),
            extend
        )
        return rst
    }

    override fun chooseCate(cate: String) {
        if (isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            model.value.page.set(1)
            val result = loadCate(cate)
            model.update { it.copy(homeVodResult = result.list.toMutableSet(), currentFilters = it.currentFilters) }
        }.invokeOnCompletion {
            isLoading.set(false)
        }
    }

    fun getFilters(type: Type): List<Filter> {
        val filters = model.value.filtersMap[type.typeId] ?: return listOf()
        // todo 这里可有多个Filter 需要修改页面 可以显示多个Filter
        return filters
    }

    fun searchBarPrompt() {
        promptJob = SiteViewModel.viewModelScope.launch {
            if (model.value.isRunning) return@launch
            model.update { it.copy(isRunning = true) }
            delay(1500) // 等待获取热门数据列表
            var idx = 0

            while (true) {
                delay(2000)
                if (idx >= GlobalModel.hotList.value.size) idx = 0
                model.update { it.copy(prompt = GlobalModel.hotList.value[idx++].title) }
            }
        }
        promptJob?.invokeOnCompletion {
            println("scroll invoke complete")
            model.value.isRunning = false
        }
    }
}