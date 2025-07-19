package com.corner.ui.nav.vm

import SiteViewModel
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Filter
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.bean.Value
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.home
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.database.Db
import com.corner.ui.nav.BaseViewModel
import com.corner.ui.nav.data.VideoScreenState
import com.corner.util.isEmpty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException


class VideoViewModel : BaseViewModel() {
    private val _state = MutableStateFlow(VideoScreenState())
    val state: StateFlow<VideoScreenState> = _state

    private var promptJob: Job? = null

    var isLoading = AtomicBoolean(false)


    init {
        scope.launch {
            GlobalAppState.home.collect {
                homeLoad()
            }
        }
        scope.launch {
            GlobalAppState.clear.collect {
                log.debug("清空")
                clear()
            }
        }

        scope.launch {
            GlobalAppState.hotList.collect {
                if (it.isEmpty()) return@collect
                searchBarPrompt()
            }
        }
    }

    fun clear() {
        _state.update {
            it.copy(
                homeVodResult = mutableSetOf(),
                homeLoaded = false, classList = mutableSetOf(), filtersMap = mutableMapOf(),
                currentClass = null, currentFilters = listOf(), dirPaths = mutableListOf(),
                page = AtomicInteger(1), isRunning = false, prompt = ""
            )
        }
    }

    fun homeLoad() {
        val home = GlobalAppState.home
        if (isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            showProgress()
            try {
                if (!_state.value.homeLoaded) {
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
                        _state.value.page.addAndGet(1)
                        list = rst.list.toMutableSet()
                    }
                    val currentClass = classList.firstOrNull()
                    _state.value.homeLoaded = true
                    _state.update {
                        it.copy(
                            homeVodResult = list,
                            currentClass = currentClass,
                            classList = classList,
                            filtersMap = filtersMap,
                            currentFilters = getFilters(currentClass!!)
                        )
                    }
//                    _state.update { it.copy(currentFilters = getFilters(currentClass!!)) }
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

    fun clickFolder(vod: Vod) {
        showProgress()
        SiteViewModel.viewModelScope.launch {
            val result = loadCate(vod.vodId)
            _state.update {
                it.copy(
                    homeVodResult = result.list.toMutableSet(),
                    dirPaths = mutableListOf<String>().apply { addAll(vod.vodId.split("/")) })
            }
        }.invokeOnCompletion {
            hideProgress()
        }
    }

    fun loadMore() {
        if (state.value.currentClass == null || state.value.currentClass?.typeId == "home") return
        if ((state.value.currentClass?.failTime ?: 0) >= 2) return
        showProgress()
        if (isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            try {
                val rst = loadCate(state.value.currentClass?.typeId ?: "")
                if (!rst.isSuccess || rst.list.isEmpty()) {
                    state.value.currentClass?.failTime = state.value.currentClass?.failTime!! + 1
                    return@launch
                }
                _state.value.page.addAndGet(1)
                val list = rst.list
                // 有的源不支持分页 每次请求返回相同的数据
                if (state.value.homeVodResult.map { it.vodId }.containsAll(list.map { it.vodId })) {
                    state.value.currentClass?.failTime = state.value.currentClass?.failTime!! + 1
                    return@launch
                }
                state.value.homeVodResult.addAll(list)
                _state.update { it.copy(homeVodResult = state.value.homeVodResult) }
            } finally {
            }
        }.invokeOnCompletion {
            isLoading.set(false)
            hideProgress()
        }
    }

    fun loadCate(cate: String): Result {
        val extend = HashMap<String, String>()
        state.value.currentFilters.forEach {
            if (it.key!!.isNotBlank() && it.init.isNotBlank()) {
                extend[it.key] = it.init
            }
        }
        val rst = SiteViewModel.categoryContent(
            home.value.key,
            cate,
            _state.value.page.toString(),
            extend.isNotEmpty(),
            extend
        )
        return rst
    }

    fun chooseCate(cate: String) {
        if (isLoading.get()) return
        isLoading.set(true)
        SiteViewModel.viewModelScope.launch {
            state.value.page.set(1)
            val result = loadCate(cate)
            _state.update { it.copy(homeVodResult = result.list.toMutableSet(), currentFilters = it.currentFilters) }
        }.invokeOnCompletion {
            isLoading.set(false)
        }
    }

    fun getFilters(type: Type): List<Filter> {
        val filters = state.value.filtersMap[type.typeId] ?: return listOf()
        // todo 这里可有多个Filter 需要修改页面 可以显示多个Filter
        return filters
    }

    /*
    * 错误处理，关闭应用时处理线程时忽略抛出的异常
    * */
    fun searchBarPrompt() {
        promptJob?.cancel()
        promptJob = SiteViewModel.viewModelScope.launch {
            try {
                if (state.value.isRunning) return@launch
                _state.update { it.copy(isRunning = true) }
                delay(1500) // 等待获取热门数据列表
                var idx = 0

                while (true) {
                    delay(2000)
                    if (idx >= GlobalAppState.hotList.value.size) idx = 0
                    _state.update { it.copy(prompt = GlobalAppState.hotList.value[idx++].title) }
                }
            } catch (e: CancellationException) {
                // 正常取消不记录错误
                _state.update { it.copy(isRunning = false) }
            } catch (e: Exception) {
                log.error("滚动提示异常", e)
                _state.update { it.copy(isRunning = false) }
            }
        }
        promptJob?.invokeOnCompletion {
            println("scroll invoke complete")
            state.value.isRunning = false
        }
    }

    fun changeSite(action: () -> Site) {
        scope.launch {
            val site = action()
            Db.Site.update(site.toDbSite(ApiConfig.api.cfg?.id ?: 0L))
            ApiConfig.apiFlow.update { api ->
                api.copy(sites = api.sites.apply {
                    first { site.key == it.key }.run {
                        changeable = site.changeable
                        searchable = site.searchable
                    }
                }, ref = api.ref + 1)
            }
        }

    }

    fun setClassData() {
        _state.update { it.copy(homeVodResult = SiteViewModel.result.value.list.toMutableSet()) }
        _state.value.page.set(1)
    }

    fun chooseFilter(f: Filter, it: Value) {
        f.init = it.v ?: ""
        f.value?.filter { i -> i.n != it.n }?.map { t -> t.selected = false }
        it.selected = true
        _state.update { it.copy(currentFilters = state.value.currentFilters, ref = state.value.ref++) }
    }

    fun chooseClass(type: Type, onClick: () -> Unit) {
        if (isLoading.get()) return
        isLoading.set(true)
        scope.launch {
            showProgress()
            for (tp in state.value.classList) {
                tp.selected = type.typeId == tp.typeId
            }
            _state.update {
                it.copy(
                    currentClass = type,
                    classList = state.value.classList,
                    currentFilters = getFilters(type),
                    dirPaths = mutableListOf()
                )
            }
            SiteViewModel.cancelAll()
            if (type.typeId == "home") {
                SiteViewModel.homeContent()
            } else {
                state.value.page.set(1)
                val result = SiteViewModel.categoryContent(
                    GlobalAppState.home.value.key,
                    type.typeId,
                    state.value.page.get().toString(),
                    false,
                    HashMap()
                )
                if (!result.isSuccess) {
                    state.value.currentClass?.failTime?.plus(1)
                }
            }
        }.invokeOnCompletion {
            onClick()
            isLoading.set(false)
            hideProgress()
        }
    }
}