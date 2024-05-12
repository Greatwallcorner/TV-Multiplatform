package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.corner.catvodcore.bean.Filter
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.bean.getFirstOrEmpty
import com.corner.catvodcore.viewmodel.GlobalModel
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

class DefaultVideoComponent(componentContext: ComponentContext):VideoComponent, ComponentContext by componentContext, BackHandlerOwner {

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
            if(it.isEmpty()) return@observe
            searchBarPrompt()
        }

        lifecycle.subscribe(callbacks = object :Lifecycle.Callbacks{
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
        _model.update { it.copy(homeVodResult = mutableSetOf(),
            homeLoaded = false, classList = mutableSetOf(), filtersMap = mutableMapOf(),
            currentClass = null, currentFilter = Filter.ALL,
            page = AtomicInteger(1),isRunning = false, prompt = ""
        ) }
    }

    override fun homeLoad() {
        val home = GlobalModel.home
        if(isLoading.get()) return
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
                    if(home.value.categories.isNotEmpty()){
                        val iterator = classList.iterator()
                        while(iterator.hasNext()){
                            val next = iterator.next()
                            if(!home.value.categories.contains(next.typeName)) iterator.remove()
                        }
                    }

                    // 有首页内容
                    if (list.isNotEmpty()) {
                        classList = (mutableSetOf(Type.home()) + classList).toMutableSet()
                    } else {
                        if (classList.isEmpty()) return@launch
                        val types = classList.firstOrNull()
                        types?.selected = true
                        val rst = SiteViewModel.categoryContent(
                            home.value.key,
                            types?.typeId ?: "",
                            _model.value.page.toString(),
                            false,
                            HashMap()
                        )
                        if(!rst.isSuccess){
                            return@launch
                        }
                        _model.value.page.addAndGet(1)
                        list = rst.list.toMutableSet()
                    }
                    val currentClass = classList.firstOrNull()
                    _model.value.homeLoaded = true
                    _model.update { it.copy(homeVodResult = list, currentClass = currentClass, classList = classList, filtersMap = filtersMap) }
                }
            } catch (e: Exception) {
                log.error("homeLoad", e)
            } finally {
            }
        }.invokeOnCompletion {
            hideProgress()
            isLoading.set(false)
        }
    }

    override fun loadMore() {
        if (model.value.currentClass == null || model.value.currentClass?.typeId == "home") return
        if((model.value.currentClass?.failTime ?: 0) >= 2) return
        showProgress()
        if(isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            try {
                val extend = HashMap<String,String>()
                extend[model.value.currentFilter.key ?: ""] = model.value.currentFilter.init
                val rst = SiteViewModel.categoryContent(
                    GlobalModel.home.value.key,
                    model.value.currentClass?.typeId ?: "",
                    model.value.page.addAndGet(1).toString(),
                    model.value.currentFilter.init.isNotBlank(),
                    extend
                )
                if(!rst.isSuccess || rst.list.isEmpty()){
                    model.value.currentClass?.failTime?.plus(1)
                    return@launch
                }
                val list = rst.list
                if (list.isNotEmpty()) {
                    val vodList = model.value.homeVodResult.toMutableList()
                    vodList.addAll(list)
                    model.update { it.copy(homeVodResult = vodList.toSet().toMutableSet()) }
                }
            } finally {
            }
        }.invokeOnCompletion {
            isLoading.set(false)
            hideProgress()
        }
    }

    override fun chooseCate(cate:String) {
        if(isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            try {
                model.value.page.set(1)
                val extend = HashMap<String, String>()
                extend[model.value.currentFilter.key ?: ""] = cate
                val result = SiteViewModel.categoryContent(
                    GlobalModel.home.value.key,
                    cate,
                    model.value.page.toString(),
                    model.value.currentFilter.init.isNotBlank(),
                    extend
                )
                model.update { it.copy(homeVodResult = result.list.toMutableSet()) }
            } finally {

            }
        }.invokeOnCompletion {
            isLoading.set(false)
        }
    }

    fun getFilters(type:Type):Filter{
        val filters = model.value.filtersMap[type.typeId] ?: return Filter.ALL
        return filters.getFirstOrEmpty()
    }

    fun searchBarPrompt(){
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