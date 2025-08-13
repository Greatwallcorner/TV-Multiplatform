package com.corner.ui.nav.vm

import SiteViewModel
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class VideoViewModel : BaseViewModel() {
    private val _state = MutableStateFlow(VideoScreenState())
    val state: StateFlow<VideoScreenState> = _state

    private var promptJob: Job? = null

    val isLoading = mutableStateOf(false)

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
    private var loadJob: Job? = null
    fun homeLoad(forceRefresh: Boolean = false) {
        //取消前一个任务
        loadJob?.cancel()
        if (isLoading.value) return

        isLoading.value = true
        log.debug("开始加载主页数据")

        SiteViewModel.viewModelScope.launch {
            try {
                showProgress()

                // 强制刷新时重置状态
                if (forceRefresh) {
                    _state.value.homeLoaded = false
                }

                if (!_state.value.homeLoaded) {
                    val home = GlobalAppState.home.value
                    if (home.isEmpty()) {
                        log.debug("主页配置为空")
                        isLoading.value = false
                        return@launch
                    }

                    // 尝试获取首页内容
                    val homeContent = SiteViewModel.homeContent()
                    var list = homeContent.list.toMutableSet()
                    var classList = SiteViewModel.result.value.types.toMutableSet()

                    // 过滤分类
                    if (home.categories.isNotEmpty()) {
                        classList.removeAll { !home.categories.contains(it.typeName) }
                    }

                    // 如果首页内容为空，尝试加载第一个分类
                    if (list.isEmpty()) {
                        if (classList.isEmpty()) {
                            log.debug("没有可用的分类")
                            SnackBar.postMsg("没有可用的分类,请尝试切换站源或重新加载", type = SnackBar.MessageType.WARNING)
                            isLoading.value = false
                            return@launch
                        }

                        val firstType = classList.first().apply { selected = true }
                        val result = loadCate(firstType.typeId)

                        if (!result.isSuccess || result.list.isEmpty()) {
                            log.debug("加载分类内容失败")
                            SnackBar.postMsg("加载分类内容失败,请尝试切换站源或重新加载", type = SnackBar.MessageType.WARNING)
                            isLoading.value = false
                            return@launch
                        }

                        _state.value.page.addAndGet(1)
                        list = result.list.toMutableSet()
                    } else {
                        classList = (mutableSetOf(Type.home()) + classList).toMutableSet()
                    }

                    // 只有成功获取到数据时才标记为已加载
                    if (list.isNotEmpty()) {
                        _state.update {
                            it.copy(
                                homeVodResult = list,
                                currentClass = classList.firstOrNull(),
                                classList = classList,
                                filtersMap = SiteViewModel.result.value.filters,
                                homeLoaded = true,  // 只有这里设为true
                                currentFilters = getFilters(classList.first())
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("加载失败", e)
                _state.value.homeLoaded = false // 失败时重置状态
            } finally {
                hideProgress()
                isLoading.value = false
            }
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
        if (isLoading.value) return
        isLoading.value = true
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
            isLoading.value = false
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
        if (isLoading.value) return
        isLoading.value = true
        SiteViewModel.viewModelScope.launch {
            state.value.page.set(1)
            val result = loadCate(cate)
            _state.update { it.copy(homeVodResult = result.list.toMutableSet(), currentFilters = it.currentFilters) }
        }.invokeOnCompletion {
            isLoading.value = false
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

                // 等待初始数据加载
                delay(1500)

                var idx = 0
                while (isActive) {  // 使用isActive检查协程状态
                    val hotList = GlobalAppState.hotList.value
                    if (hotList.isEmpty()) {
                        delay(2000)  // 列表为空时等待
                        continue
                    }

                    _state.update {
                        it.copy(prompt = hotList[idx % hotList.size].title)
                    }
                    idx++
                    delay(2000)
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
            _state.update { it.copy(isRunning = false) }
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
        if (isLoading.value) return
        isLoading.value = true
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
            isLoading.value = false
            hideProgress()
        }
    }
}