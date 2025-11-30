package com.corner.ui.nav.vm

import SiteViewModel
import com.corner.ui.scene.SnackBar
import androidx.compose.runtime.*
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.bean.enums.PlayerType
import com.corner.bean.getPlayerSetting
import com.corner.catvodcore.bean.Vod
import com.corner.catvodcore.bean.Vod.Companion.getEpisode
import com.corner.catvodcore.bean.Vod.Companion.getPage
import com.corner.catvodcore.bean.Vod.Companion.isEmpty
import com.corner.catvodcore.bean.*
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.DetailFromPage
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.database.Db
import com.corner.database.entity.History
import com.corner.ui.nav.BaseViewModel
import com.corner.ui.nav.data.DetailScreenState
import com.corner.ui.onUserSelectEpisode
import com.corner.ui.player.PlayState
import com.corner.ui.player.PlayerLifecycleManager
import com.corner.ui.player.PlayerLifecycleState.*
import com.corner.ui.player.vlcj.VlcJInit
import com.corner.ui.player.vlcj.VlcjFrameController
import com.corner.util.Constants
import com.corner.util.cancelAll
import com.corner.util.play.Play
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.CopyOnWriteArrayList


class DetailViewModel : BaseViewModel() {
    private val _state = MutableStateFlow(DetailScreenState())
    val state: StateFlow<DetailScreenState> = _state
    private var supervisor = SupervisorJob()
    private val searchScope = CoroutineScope(Dispatchers.Default + supervisor)
    private val lock = Any()

    @Volatile
    private var launched = false
    private var currentSiteKey = MutableStateFlow("")
    private val jobList = mutableListOf<Job>()
    private var fromSearchLoadJob: Job = Job()
    val controller: VlcjFrameController = VlcjFrameController(
        this
    ).apply { VlcJInit.setController(this) }
    val lifecycleManager: PlayerLifecycleManager = PlayerLifecycleManager(controller) //生命周期管理器
    var currentSelectedEpNumber by mutableStateOf(1) //用于记录当前选中的剧集编号，默认第1集
    val currentEpisodeIndex: Int
        get() = currentSelectedEpNumber
    val isLastEpisode: Boolean
        get() {
            val detail = _state.value.detail
            val totalEpisodes = detail.currentFlag.episodes.size
            // 直接在完整列表中查找激活的剧集
            val currentEp = detail.currentFlag.episodes.find { it.activated }
            if (currentEp != null) {
                val currentIndex = detail.currentFlag.episodes.indexOf(currentEp)
                return currentIndex >= totalEpisodes - 1
            }
            return false
        }
    private val _currentFlagName = MutableStateFlow("")
    val currentFlagName: StateFlow<String> = _currentFlagName

    var controllerHistory: History? = null

    val vmPlayerType = SettingStore.getSettingItem(SettingType.PLAYER.id)
        .getPlayerSetting(_state.value.detail.site?.playerType)

    /**
     * 更新历史记录信息。
     * 仅当详情页状态中的站点 key 不为空时，才会在协程中更新历史记录，
     * 并将历史记录的创建时间更新为当前系统时间。
     *
     * @param it 要更新的历史记录对象，包含了需要更新的历史记录信息。
     */
    fun updateHistory(it: History) {
        // 检查详情页状态中的站点 key 是否不为空
        if (StringUtils.isNotBlank(state.value.detail.site?.key)) {
            // 在协程作用域中启动一个协程来更新历史记录
            scope.launch {
                try {
                    // 复制历史记录对象，并将创建时间更新为当前系统时间的毫秒数，然后进行更新操作
                    Db.History.update(it.copy(createTime = Clock.System.now().toEpochMilliseconds()))
                } catch (e: Exception) {
                    log.error("历史记录更新失败", e)
                }
            }
        }
    }

    //下一集锁
    private val nextEpisodeLock = Object()


    init {
        // 监控播放器播放状态变化
        scope.launch {
            controller.state.collect { playerState ->
                when (playerState.state) {
                    PlayState.ERROR -> {
                        // 处理播放错误
                        log.error("播放错误: ${playerState.msg}")
                        // 根据当前播放器状态进行相应的处理
                        when (lifecycleManager.lifecycleState.value) {
                            Playing -> {
                                // 如果正在播放，先停止再结束
                                lifecycleManager.stop()
                                lifecycleManager.ended()
                            }

                            Loading, Ready -> {
                                // 如果在加载或就绪状态，直接结束
                                lifecycleManager.ended()
                            }

                            Paused -> {
                                // 如果已暂停，直接结束
                                lifecycleManager.ended()
                            }

                            else -> {
                                // 其他状态也直接结束
                                lifecycleManager.ended()
                            }
                        }
                    }

                    PlayState.BUFFERING -> {
                        _state.update { it.copy(isBuffering = true) }
                    }

                    else -> {
                        _state.update { it.copy(isBuffering = false) }
                    }
                }
            }
        }
    }

    /** 获取视频信息，并更新当前站点key
     * */
    private fun loadChooseVod(): Vod {
        // 获取当前选中的视频信息
        val chooseVod = getChooseVod()
        // 更新状态流中的详情信息为当前选中的视频信息
        _state.update { it.copy(detail = chooseVod) }
        // 更新当前站点的 key
        currentSiteKey.value = chooseVod.site?.key ?: ""
        return chooseVod
    }

    /**
     * 加载**搜索**详情页信息。
     *
     * @param chooseVod 要加载的详情页信息。
     * @param list 快速搜索结果列表。
     */
    private fun loadSearchResult(chooseVod: Vod, list: MutableList<Vod>) {
        // 更新状态流中的快速搜索结果和详情信息
        _state.update {
            it.copy(
                detail = chooseVod,
                quickSearchResult = CopyOnWriteArrayList(list)
            )
        }
        // 在 SiteViewModel 的协程作用域中启动一个新协程
        fromSearchLoadJob = SiteViewModel.viewModelScope.launch {
            // 若快速搜索结果不为空，则加载详情信息
            if (_state.value.quickSearchResult.isNotEmpty()) _state.value.detail.let { loadDetail(it) }
        }
    }

    /**
     * 加载详情页信息。
     *
     * @param dt 要加载的详情页信息。
     */
    private fun loadVodDetail(dt: Result) {
        // 获取详情列表中的第一个元素
        var detail = dt.list[0]
        // 复制详情信息并更新子剧集信息
        detail =
            detail.copy(subEpisode = detail.currentFlag.episodes.getPage(detail.currentTabIndex))
        // 若当前选中视频的备注信息不为空
        if (StringUtils.isNotBlank(getChooseVod().vodRemarks)) {
            // 遍历子剧集列表
            for (it: Episode in detail.subEpisode) {
                // 若子剧集名称与备注信息相同，则将该子剧集标记为激活状态
                if (it.name == getChooseVod().vodRemarks) {
                    it.activated = true
                    break
                }
            }
        }
        // 更新详情信息的站点信息
        detail.site = getChooseVod().site
        // 更新状态流中的详情信息
        _state.update { it.copy(detail = detail) }
        _currentFlagName.value = detail.currentFlag.flag.toString()
    }

    /**
     * 加载详情页并根据不同来源执行相应操作。
     * 此方法会显示加载进度，初始化播放器控制器，根据详情页来源（搜索页或其他）
     * 加载不同的数据，最后隐藏加载进度。
     */
    suspend fun load() {
        if (vmPlayerType.first() == PlayerType.Innie.id) {
            lifecycleManager.initializeSync()
        }
        val chooseVod = loadChooseVod()

        try {
            _state.update { it.copy(isLoading = true) }
            // 在 SiteViewModel 的协程作用域中启动一个协程
            SiteViewModel.viewModelScope.launch {
                // 检查详情页是否来自搜索页
                if (GlobalAppState.detailFrom == DetailFromPage.SEARCH) {
                    // 获取搜索结果中的激活列表
                    val list = SiteViewModel.getSearchResultActive().list
                    loadSearchResult(chooseVod, list)
                } else {
                    val dt = SiteViewModel.detailContent(chooseVod.site?.key ?: "", chooseVod.vodId)
                    if (chooseVod.vodId.isBlank()) return@launch
                    if (dt == null || dt.detailIsEmpty()) {
                        quickSearch()
                    } else {
                        loadVodDetail(dt)

                        if (vmPlayerType.first() == PlayerType.Innie.id) {
                            lifecycleManager.transitionTo(Loading) {
                                lifecycleManager.loading().onFailure { log.warn("初始化内部播放器失败!") }
                            }
                        }

                        startPlay()//load加载完成后启动播放流程
                    }
                }
            }.invokeOnCompletion { _state.update { it.copy(isLoading = false) } }
        } catch (e: Exception) {
            log.error("加载详情失败", e)
        }
    }

    /**
     * 获取全局应用状态中当前选中的视频对象。
     * 该方法从 GlobalAppState 中获取当前选中的视频信息，
     * 并将其作为 Vod 类型的对象返回，供其他方法使用。
     *
     * @return 当前选中的视频对象，类型为 Vod。
     */
    private fun getChooseVod(): Vod {
        // 从 GlobalAppState 中获取当前选中的视频对象并返回
        return GlobalAppState.chooseVod.value
    }

    ///////////////////////////////////////////////////////////////////////////
    //--------------------------quick Search Start---------------------------//
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 执行**快速搜索**操作，从可切换的站点中搜索视频信息。
     *
     * 该方法会并发地从多个站点进行搜索，限制同时执行的搜索任务数量，
     *
     * 并在搜索完成后更新快速搜索结果。若搜索到有效结果，会加载首个结果的详情；
     *
     * 若未搜索到结果，则提示用户暂无线路数据。
     *
     */
    fun quickSearch() {
        searchScope.launch {
            _state.update { it.copy(isLoading = true, isBuffering = false) }
            // 筛选出可切换的站点列表，并打乱顺序
            val quickSearchSites = ApiConfig.api.sites.filter { it.changeable == 1 }.shuffled()
            val totalSites = quickSearchSites.size  // 获取总站点数
            var completedCount = 0  // 已完成计数器

            log.debug("开始执行快搜 sites:{}", quickSearchSites.map { it.name }.toString())

            postQuickSearchProgress(0, totalSites)  // 发送开始搜索消息
            val semaphore = Semaphore(2)            // 创建一个信号量，限制同时执行的搜索任务数量为 2

            quickSearchSites.forEach {      // 遍历可搜索的站点列表
                val job = launch {          // 为每个站点启动一个新的协程进行搜索
                    semaphore.acquire()     // 获取信号量许可，若没有可用许可则挂起协程
                    try {
                        withTimeout(2500L) {// 设置超时时间为 2500 毫秒，在超时时间内执行搜索操作
                            SiteViewModel.searchContent(it, getChooseVod().vodName ?: "", true)
                            log.debug("{}完成搜索", it.name) // 记录该站点搜索完成的日志
                        }
                    } catch (e: Exception) {
                        log.error("搜索站点 {} 时发生异常: {}", it.name, e.message)
                    } finally {
                        semaphore.release() // 释放信号量许可，允许其他协程获取许可
                    }
                }

                // 为每个搜索任务添加完成回调
                job.invokeOnCompletion { throwable ->
                    completedCount++
                    if (throwable != null) {
                        // 若协程执行过程中出现异常，记录错误日志
                        log.error("quickSearch 协程执行异常 msg:{}", throwable.message)
                    }

                    // 更新进度显示，包含当前完成的站点名称
                    val currentSiteName = if (completedCount <= quickSearchSites.size) {
                        quickSearchSites.getOrNull(completedCount - 1)?.name ?: "未知"
                    } else "完成"
                    postQuickSearchProgress(completedCount, totalSites, currentSiteName)

                    // 更新状态流，将搜索结果添加到快速搜索结果列表中
                    _state.update { state ->
                        val list = CopyOnWriteArrayList<Vod>()
                        // 将搜索结果添加到列表中，避免重复元素
                        list.addAllAbsent(SiteViewModel.quickSearch.value[0].list)
                        state.copy(
                            quickSearchResult = list,
                        )
                    }
                    if (throwable == null) {
                        // 若协程正常完成，记录完成日志并输出结果列表大小
                        log.debug(
                            "job执行完毕 {}/{} result size:{}",
                            completedCount,
                            totalSites,
                            _state.value.quickSearchResult.size
                        )
                    }

                    // 使用同步锁确保线程安全
                    synchronized(lock) {
                        try {
                            // 检查状态是否仍然有效
                            if (!_state.value.quickSearchResult.isNullOrEmpty() &&
                                _state.value.detail.isEmpty() &&
                                !launched &&
                                isActive
                            ) {  // 检查协程是否仍活跃
                                // 记录开始加载详情的日志
                                log.info("开始加载 详情")
                                // 标记已启动加载详情操作
                                launched = true
                                // 加载快速搜索结果中的第一个视频详情
                                loadDetail(_state.value.quickSearchResult[0])
                            }
                        } catch (e: Exception) {
                            log.error("处理搜索完成时发生异常: {}", e.message)
                        }
                    }
                }
                // 将搜索任务添加到任务列表中
                jobList.add(job)
            }
            // 等待所有搜索任务完成
            try {
                jobList.joinAll()
            } catch (e: Exception) {
                log.error("等待搜索任务完成时发生异常: {}", e.message)
            }
            // 若快速搜索结果为空
            if (_state.value.quickSearchResult.isEmpty()) {
                // 更新状态流，将详情信息设置为全局选中的视频信息
                _state.update { it.copy(detail = GlobalAppState.chooseVod.value, isLoading = false) }
                // 提示用户暂无线路数据
                SnackBar.postMsg("暂无线路数据", type = SnackBar.MessageType.WARNING)
            }
        }.invokeOnCompletion {
            // 统一关闭加载指示器
            _state.update { it.copy(isLoading = false) }
            try {
                // 所有搜索任务完成后，更新状态流，标记搜索结束
                _state.update { it.copy() }
            } catch (e: Exception) {
                log.error("搜索完成回调时发生异常: {}", e.message)
            }
        }
    }


    /**
     * 加载**快速搜索**出的视频的的详细信息。
     * 如果视频站点 key 为空，会尝试加载下一个视频。
     * 若获取详情失败或详情为空，也会尝试加载下一个视频。
     * 若详情有效，则设置详情信息并清理相关协程任务。
     *
     * @param vod 要加载详情的视频对象
     */
    fun loadDetail(vod: Vod) {
        // 记录开始加载视频详情的日志，包含视频名称、ID 和站点信息
        log.info("加载详情 <${vod.vodName}> <${vod.vodId}> site:<${vod.site}>")
        try {
            _state.update { it.copy(isLoading = true) }
            // 获取视频对应的站点 key，使用安全调用符处理可能的空值
            val siteKey = vod.site?.key
            // 若站点 key 为空，记录错误日志并尝试加载下一个视频，然后结束当前函数
            if (siteKey == null) {
                log.error("视频站点 key 为空，无法加载详情")
                SnackBar.postMsg("视频站点key为空,将自动切换下一个站源剧集...", type = SnackBar.MessageType.INFO)

                _state.update { it.copy(isLoading = false) }

                nextSite(vod)
                return
            }
            // 尝试获取视频详情信息，捕获可能出现的异常
            val dt = try {
                SiteViewModel.detailContent(siteKey, vod.vodId)
            } catch (e: Exception) {
                // 若出现异常，记录错误日志并返回 null
                log.error("获取视频详情信息时发生异常", e)
                null
            }
            // 若获取的详情信息为空或详情本身为空，记录日志并尝试加载下一个视频
            if (dt == null || dt.detailIsEmpty()) {
                log.info("请求详情为空 加载下一个站源数据")
                SnackBar.postMsg("请求详情为空 加载下一个站源数据", type = SnackBar.MessageType.INFO)

                _state.update { it.copy(isLoading = false) }

                nextSite(vod)
            } else {
                // 从详情列表中取出第一个元素
                val first = dt.list[0]
                // 记录加载详情完成的日志
                log.info("加载详情完成 ${first.toString().take(50)}...")
                // 为详情对象设置站点信息
                first.site = vod.site
                // 若详情对象为空，尝试加载下一个视频
                // TODO 设置加载最大深度
                if (first.isEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                    nextSite(vod)
                } else {
                    _state.update { it.copy(isLoading = false) }
                    // 若详情对象有效，设置详情信息
                    setDetail(first)
                    log.debug("切换线路，新的线路标识: {}", first.currentFlag.flag)
                    _currentFlagName.value = first.currentFlag.flag.toString()
                    // 取消 supervisor 协程的所有子协程
                    supervisor.cancelChildren()
                    // 取消 jobList 中的所有协程任务并清空列表
                    jobList.cancelAll().clear()
                }
            }
        } finally {
            // 将 launched 标志置为 false
            launched = false
        }
    }

    /**
     * 尝试从**快速搜索**结果中加载下一个视频的详情。
     * 如果提供了上一个视频对象，会将其从快速搜索结果列表中移除，
     * 然后尝试加载剩余结果列表中的第一个视频详情。
     *
     * @param lastVod 上一个尝试加载详情但失败的视频对象，可为 null。
     */
    fun nextSite(lastVod: Vod?) {

        _state.update { it.copy(isLoading = true) }

        // 检查快速搜索结果列表是否为空
        if (_state.value.quickSearchResult.isEmpty()) {
            // 若为空，记录警告日志并结束函数
            log.warn("快搜结果为空,无法加载下一个视频")
            _state.update { it.copy(isLoading = false) }
            SnackBar.postMsg("暂无更多视频", type = SnackBar.MessageType.WARNING)
            return
        }
        // 获取当前的快速搜索结果列表
        val list = _state.value.quickSearchResult
        // 检查上一个视频对象是否不为空
        if (lastVod != null) {
            // 若不为空，尝试从快速搜索结果列表中移除该视频对象
            val remove = list.remove(lastVod)
            // 记录移除操作的结果
            log.debug("remove last vod result:$remove")
        }
        // 更新状态流中的快速搜索结果列表
        _state.update { it.copy(quickSearchResult = list, isLoading = false) }
        // 检查更新后的快速搜索结果列表是否不为空
        if (_state.value.quickSearchResult.isNotEmpty()) {
            // 若不为空，加载列表中第一个视频的详情
            loadDetail(_state.value.quickSearchResult[0])
        }
    }

    /**
     * 设置**快速搜索**出的视频详情信息并准备播放新视频。
     * 若当前站点 key 与传入视频的站点 key 不一致，会提示用户正在切换站源。
     * 接着更新状态流中的详情信息，强制停止当前播放的视频，最后启动新视频的播放。
     *
     * @param vod 要设置的视频详情对象，包含视频的详细信息。
     */
    private fun setDetail(vod: Vod) {
        // 检查当前站点 key 是否与传入视频的站点 key 不同
        if (currentSiteKey.value != vod.site?.key) {
            // 若不同，提示用户正在切换站源至传入视频所在的站点
            SnackBar.postMsg("正在切换站源至 [${vod.site!!.name}]", type = SnackBar.MessageType.INFO)
        }
        // 更新状态流中的详情信息
        _state.update {
            it.copy(
                // 复制传入的视频对象，并更新其子剧集信息
                detail = vod.copy(
                    // 从视频的第一个标志位获取对应页的剧集列表并转为可变列表
                    subEpisode = vod.vodFlags.first().episodes.getPage(vod.currentTabIndex).toMutableList()
                ),
                isLoading = true
            )
        }

        // 获取第一个剧集作为默认选中剧集
        val firstEpisode = vod.vodFlags.first().episodes.firstOrNull()
        if (firstEpisode != null) {
            // 使用统一的方法更新剧集激活状态和当前选中的剧集信息
            updateEpisodeActivation(firstEpisode)
        } else {
            // 如果没有剧集，至少更新currentEp为null
            _state.update { it.copy(currentEp = null) }
        }

        // 在开始播放新视频前，强制停止当前正在播放的视频
        scope.launch {
            when (
                // 如果正在播放，先暂停再停止
                val currentState = lifecycleManager.lifecycleState.value
            ) {
                Playing -> {
                    log.debug("setDetail - 正在播放视频，需要停止播放")
                    lifecycleManager.stop()
                        .onSuccess {
                            lifecycleManager.ended()
                                .onSuccess { startPlay() }
                        }
                        .onFailure {
                            log.error("停止播放失败，强行播放视频...")
                            startPlay()
                        }
                }
                // 如果已暂停，直接停止
                Paused -> {
                    log.debug("setDetail - 已暂停，直接停止")
                    lifecycleManager.ended()
                        .onSuccess { startPlay() }
                        .onFailure { startPlay() }
                }
                // 如果已结束，直接转换到loading
                Ended -> {
                    log.debug("setDetail - 已结束，直接转换到loading")
                    startPlay()
                }
                //如果初始化完成，直接转换到loading
                Initialized -> {
                    log.debug("setDetail - 正在初始化，直接转换到loading")
                    startPlay()
                }
                // 其他状态，尝试停止后转换到loading
                else -> {
                    log.debug("setDetail - 其他状态，尝试停止后转换到loading,当前状态:{}", currentState)
                    lifecycleManager.ended()
                        .onSuccess { startPlay() }
                        .onFailure { startPlay() }
                }
            }
        }.invokeOnCompletion { _state.update { it.copy(isLoading = false) } }
    }

    /**
     * 发布**快速搜索**进度消息。
     *
     * 该方法用于在搜索过程中更新搜索进度消息，包含当前已完成的搜索任务数量、总任务数量，
     *
     * 以及当前正在搜索的站点名称（可选）。
     *
     * @param current 当前已完成的搜索任务数量。
     * @param total 总搜索任务数量。
     * @param currentSite 当前正在搜索的站点名称，默认为空字符串。
     */
    fun postQuickSearchProgress(current: Int, total: Int, currentSite: String = "") {
        val message = if (currentSite.isNotEmpty()) {
            "搜索进度: $current/$total - $currentSite"
        } else {
            "搜索进度: $current/$total"
        }
        SnackBar.postMsg(message, priority = 1, type = SnackBar.MessageType.INFO, key = "quick_search_progress")
    }

    //////////////////////////////////////////////////////////////////////////////
    //------------------------------quick Search END----------------------------//
    //////////////////////////////////////////////////////////////////////////////

    /**
     * 清理详情页相关资源和状态。
     * 可选择是否释放播放器控制器资源，默认会释放。
     *
     * @param releaseController 是否释放播放器控制器资源，默认为 true。
     */
    fun clear(releaseController: Boolean = true, onComplete: () -> Unit = {}) {
        log.debug("----------开始清理详情页资源----------")

        // 创建一个延迟2秒显示进度条的任务
        var progressJob: Job? = null

        scope.launch(Dispatchers.IO) {
            try {
                // 延迟2秒后显示进度条
                progressJob = launch {
                    delay(2000L)
                    SnackBar.postMsg("播放器等资源清理异常缓慢，请耐心等待...", type = SnackBar.MessageType.WARNING)
                    showProgress()
                }

                log.debug("<清理资源>当前播放器类型:${vmPlayerType.first()}，手动放弃清理播放器资源:{${!releaseController}}")

                if (releaseController && vmPlayerType.first() == PlayerType.Innie.id) {
                    // 使用串行清理，避免并发问题
                    // 特殊状态转换，将关闭视频放在了特殊清理方法中，且直接状态转换playing->cleanup
                    try {
                        lifecycleManager.cleanup()
                            .onSuccess {
                                // 确保清理完成后再释放
                                lifecycleManager.release()
                                    .onSuccess { log.debug("生命周期释放完成") }
                                    .onFailure { e -> log.error("生命周期释放失败", e) }
                            }
                            .onFailure { e -> log.error("生命周期清理失败", e) }
                    } catch (e: Exception) {
                        log.error("清理播放器时发生异常", e)
                    }
                }

                // 清理协程任务
                jobList.forEach {
                    try {
                        it.cancel("detail clear")
                    } catch (e: Exception) {
                        log.warn("取消协程任务时出错", e)
                    }
                }
                jobList.clear()

                _state.update { it.copy() } // 重置状态
                SiteViewModel.clearQuickSearch()
                launched = false

                //supervisor.cancelChildren() // 取消所有子任务

                log.debug("----------清理详情页资源完成----------")

                // 回调到主线程
                withContext(Dispatchers.Swing) {
                    onComplete()
                }
            } catch (e: Exception) {
                log.error("----------清理过程中出错----------", e)
            } finally {
                // 取消进度条显示任务
                progressJob?.cancel()
                hideProgress()
            }
        }
    }

    /**
     * 负责加载并开始播放一个已经获取到的播放结果 (Result)。它接收 result (包含 URL 和其他信息) 和一个 needAutoPlay 标志。
     * 注意，不会根据历史记录设置播放速度和位置。
     *
     * @param result 播放结果对象，可能为 null。
     */
    fun play(result: Result?) {
        if (result == null || result.playResultIsEmpty()) {
            SnackBar.postMsg("加载内容失败，尝试切换线路", type = SnackBar.MessageType.WARNING)
            nextFlag()
            return
        }

        scope.launch {
            try {
                // 更新播放状态
                _state.update {
                    it.copy(
                        currentPlayUrl = result.url.v(),
                        playResult = result,
                        isDLNA = false                    //重置DLNA状态
                    )
                }
                prepareForPlayback(result)
            } catch (e: Exception) {
                log.error("播放器初始化失败", e)
                SnackBar.postMsg("播放器初始化失败: ${e.message}", type = SnackBar.MessageType.ERROR)
                _state.update { it.copy() }
            }
        }
    }


    /**
     * 根据播放器当前状态，判断是否需要停止播放，并转换到准备就绪状态。
     * 如果当前状态允许转换到准备就绪状态，则转换并加载播放链接。
     *
     * @param result 播放结果对象。
     */
    private suspend fun prepareForPlayback(result: Result) {
        log.debug("<prepareForPlayback> -- 开始处理播放器状态")
        log.debug("<prepareForPlayback> -- 当前状态为: {}", lifecycleManager.lifecycleState.value)

        // 统一确保播放器处于 Ready 状态
        val success = when (lifecycleManager.lifecycleState.value) {
            Ready -> {
                playInitPlayer(result)
                return
            }

            Playing -> {
                log.debug("<prepareForPlayback> -- 当前状态为playing，需要状态转换")
                lifecycleManager.stop().isSuccess &&
                        lifecycleManager.ended().isSuccess &&
                        lifecycleManager.ready().isSuccess
            }

            Loading -> {
                lifecycleManager.ready().isSuccess
            }

            Ended -> {
                lifecycleManager.ready().isSuccess
            }

            Ended_Async -> {
                lifecycleManager.ready().isSuccess
            }

            Error -> {
                try {
                    // 清理资源
                    val cleanupSuccess = lifecycleManager.cleanup().isSuccess
                    if (!cleanupSuccess) {
                        log.warn("清理资源失败")
                    }

                    // 重新初始化
                    val initSuccess = lifecycleManager.initializeSync().isSuccess
                    if (!initSuccess) {
                        log.error("重新初始化失败")
                        SnackBar.postMsg("播放器初始化失败", type = SnackBar.MessageType.ERROR)
                        false
                    } else {
                        // 进入加载状态
                        val loadingSuccess = lifecycleManager.loading().isSuccess
                        if (!loadingSuccess) {
                            log.error("播放器加载失败")
                            SnackBar.postMsg("播放器加载失败", type = SnackBar.MessageType.ERROR)
                            false
                        } else {
                            // 进入就绪状态
                            val readySuccess = lifecycleManager.ready().isSuccess
                            if (!readySuccess) {
                                log.error("播放器准备就绪失败")
                                SnackBar.postMsg("播放器准备就绪失败", type = SnackBar.MessageType.ERROR)
                                false
                            } else {
                                log.debug("错误状态恢复成功")
                                true
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("错误状态恢复过程中发生异常", e)
                    SnackBar.postMsg("播放器恢复失败: ${e.message}", type = SnackBar.MessageType.ERROR)
                    false
                }
            }

            else -> {
                log.debug("<prepareForPlayback> -- 当前状态为{}，转换到ready状态", lifecycleManager.lifecycleState.value)
                lifecycleManager.ended().isSuccess &&
                        lifecycleManager.ready().isSuccess
            }
        }

        if (success) {
            playInitPlayer(result)
        } else {
            log.error("<prepareForPlayback> -- 状态转换失败，无法进入 Ready 状态")
            SnackBar.postMsg("播放器状态错误，无法准备播放", type = SnackBar.MessageType.ERROR)
        }
    }


    /**
     * 初始化播放器并加载视频。
     * 是**play函数**专用的私有方法
     * 该方法会根据提供的视频结果和自动播放标志初始化播放器，
     * 并在播放器准备就绪后开始播放视频。
     *
     * @param result 视频结果对象，包含视频的 URL 和其他相关信息。
     */
    private suspend fun playInitPlayer(result: Result) {
        _state.update { it.copy(isLoading = false, isBuffering = false) }
        log.debug("<playInitPlayer> -- 设置播放链接")
        // 检查当前状态，避免重复加载
        if (lifecycleManager.lifecycleState.value != Ready) {
            log.error("<playInitPlayer> -- 播放器状态不正确: {},播放器检查失败！", lifecycleManager.lifecycleState.value)
            return
        }

        try {
            controller.load(result.url.v())// playEP加载播放流程
        } catch (e: Exception) {
            log.error("<playInitPlayer> -- 加载或播放 URL 失败: ${result.url.v()}", e)
            // 可能需要通知 UI 或回滚状态
            SnackBar.postMsg("加载播放链接失败: ${e.message}", type = SnackBar.MessageType.ERROR)
            return // 退出函数，不尝试收集 playerReady
        }

        // 为 collect 添加超时，防止无限期挂起
        try {
            withTimeout(30000) { // 30秒超时
                log.info("<playInitPlayer> -- 播放器加载完成，开始转换状态")
                lifecycleManager.transitionTo(Playing) {
                    lifecycleManager.start()
                        .onFailure {
                            log.error("<playInitPlayer> -- 播放器状态转换 Playing 失败", it)
                            SnackBar.postMsg(
                                "播放器状态转换失败: ${it.message}",
                                type = SnackBar.MessageType.ERROR
                            )
                        }
                }.onFailure { e ->
                    log.error("<playInitPlayer> -- 播放器就绪失败", e)
                    SnackBar.postMsg(
                        "播放器就绪失败: ${e.message}",
                        type = SnackBar.MessageType.ERROR
                    )
                }

            }
        } catch (e: TimeoutCancellationException) {
            log.error("<playInitPlayer> -- 等待播放器就绪超时:{\n$e\n}")
            SnackBar.postMsg("播放器加载超时", type = SnackBar.MessageType.ERROR)
            lifecycleManager.ended() // 或者尝试结束当前加载
        } catch (e: Exception) {
            log.error("<playInitPlayer> -- 等待播放器就绪时发生错误", e)
            SnackBar.postMsg("播放器准备就绪时发生错误: ${e.message}", type = SnackBar.MessageType.ERROR)
            lifecycleManager.ended() // 或者尝试结束当前加载
        }
    }

    /**
     * 播放指定视频的指定剧集。
     * 该方法会根据剧集的 URL 获取播放结果，根据播放结果和播放器设置执行不同操作。
     * 主要职责包括：
     * 1. 获取剧集的播放链接
     * 2. 处理特殊链接和无效播放结果
     * 3. 更新播放历史记录和剧集激活状态
     * 4. 根据播放器类型（内置/外部）执行相应的播放操作
     * 5. 管理播放状态转换和UI状态更新
     *
     * @param detail 视频详情对象，包含视频的基本信息和站点信息。
     * @param ep 要播放的剧集对象，包含剧集的名称和 URL 等信息。
     */
    private fun playEp(detail: Vod, ep: Episode) {
        _state.update { it.copy(isBuffering = true) } //开始缓冲流程
        onUserSelectEpisode()// 用户选择剧集，不自动加载url，以防竟态
        val result = SiteViewModel.playerContent(
            detail.site?.key ?: "",
            detail.currentFlag.flag ?: "",
            ep.url
        )

        currentSelectedEpNumber = ep.number

        if (isSpecialVideoLink(ep)) {
            _state.update { it.copy(isBuffering = false) }
            return
        }

        if (result == null || result.playResultIsEmpty()) {
            _state.update { it.copy(isBuffering = false) }
            log.warn("播放结果为空,无法播放")
            nextFlag()
            return
        }

        _state.update { it.copy(currentUrl = result.url) }
        // 将当前剧集URL保存到历史记录中
        controller.doWithHistory { history ->
            history.copy(
                episodeUrl = ep.url,
                vodRemarks = ep.name,
                position = history.position ?: 0L
            )
        }

        // 将当前剧集标记为激活状态，更新UI显示
        updateEpisodeActivation(ep)

        //获取播放器设置
        val playerType =
            SettingStore.getSettingItem(SettingType.PLAYER.id).getPlayerSetting(detail.site?.playerType)
        log.debug("播放器类型: {}", playerType)
        // 根据播放器类型执行不同的播放操作
        when (playerType.first()) {
            PlayerType.Innie.id -> {
                scope.launch {
                    log.debug("<play>当前播放器状态{}", lifecycleManager.lifecycleState.value)
                    play(result)
                }
            }

            PlayerType.Outie.id -> {
                scope.launch {
                    SnackBar.postMsg("即将播放" + ": ${ep.name}", type = SnackBar.MessageType.INFO)
                    _state.update { it.copy(isBuffering = false) }
                    Play.start(result.url.v(), ep.name)
                }
            }
        }
    }


    /**
     * 检测处理play()方法中的特殊链接
     * @param ep 当前剧集对象
     * */
    private fun isSpecialVideoLink(ep: Episode): Boolean {
        if (Utils.isDownloadLink(ep.url)) {
            log.info("播放链接为下载链接,驳回播放请求")
            SnackBar.postMsg("播放链接为下载链接,无法播放", type = SnackBar.MessageType.WARNING)
            return true
        }

        val isSpecialLink = SiteViewModel.state.value.isSpecialVideoLink
        log.debug("特殊链接: $isSpecialLink")
        if (isSpecialLink) {
            log.debug("检测到特殊链接，驳回播放请求")
            updateEpisodeActivation(ep) // 更新剧集激活状态，将当前剧集标记为激活
            updateHistoryWithNewEpisode(ep) // 更新历史记录
            return true
        }

        return false
    }

    /**
     * 查询获取历史记录
     */
    private suspend fun handlePlaybackHistory(detail: Vod): Episode? {
        val historyKey = Utils.getHistoryKey(detail.site?.key!!, detail.vodId)
        log.debug("<查询历史记录>Key: {}", historyKey)

        // 查询历史记录
        val history = Db.History.findHistory(historyKey)
        controllerHistory = history

        // 处理历史记录并获取应该播放的剧集
        val findEp = processHistory(history, detail)

        // 如果processHistory没有创建新的历史记录，使用查询到的历史记录
        // 如果创建了新的历史记录，等待其完成并获取最新的历史记录
        if (controller.getControllerHistory() == null && controllerHistory != null) {
            val history = controllerHistory
            if (history != null) {
                controller.setControllerHistory(history)
            }
        } else if (controller.getControllerHistory() != null) {
            controllerHistory = controller.getControllerHistory()
        }

        return findEp
    }

    /**
     * 启动视频播放流程。
     * 该方法会检查播放器控制器状态、视频详情信息以及历史记录，
     * 根据不同情况初始化历史记录、设置播放起始和结束时间，
     * 最后调用 [playEp] 方法开始播放视频。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startPlay() {
        //显示加载状态
        _state.update { it.copy(isLoading = true) }

        val detail = _state.value.detail        // 获取当前视频详情信息

        // 检查视频详情信息是否为空，若为空则不进行播放操作
        if (!detail.isEmpty()) {
            if (controller.isReleased()) {
                // 检查播放器是否已释放
                log.error("播放器已被释放，无法播放!")
                SnackBar.postMsg("播放器已被释放,无法播放!请重启软件或去GITHUB反馈!", type = SnackBar.MessageType.ERROR)
                _state.update { it.copy(isLoading = false, isBuffering = false) }
                return
            } else if (controller.isPlaying()) {
                // 检查是否正在播放
                log.warn("视频播放中,播放请求被驳回")
                SnackBar.postMsg("视频播放中,播放请求被驳回!", type = SnackBar.MessageType.WARNING)
                _state.update { it.copy(isLoading = false, isBuffering = false) }
                return
            } else if (detail.isEmpty()) {
                // 检查视频详情是否为空
                log.error("视频详情信息为空，无法播放!")
                SnackBar.postMsg("视频详情信息为空,无法播放!", type = SnackBar.MessageType.ERROR)
                _state.update { it.copy(isLoading = false, isBuffering = false) }
                return
            }

            scope.launch {
                val findEp = handlePlaybackHistory(detail)
                detail.subEpisode.apply {   // 处理视频的子剧集列表
                    val ep = findEp ?: first()
                    log.debug("<startPlay>开始播放视频:{}", ep.name)
                    _state.update { it.copy(isLoading = false) }

                    playEp(detail, ep) //startPlay调用playEp()方法开始播放视频
                }
            }
        }
    }


    /**
     * 视频历史记录处理。
     * 根据历史记录处理视频播放状态，返回应该播放的剧集
     * @param history 历史记录对象
     * @param detail 视频详情对象
     * @return 应该播放的剧集对象
     */

    private fun processHistory(history: History?, detail: Vod): Episode? {
        var findEp: Episode? = null

        if (history == null) {
            log.debug("[startPlay]未找到历史记录，将创建新的历史记录")
            if (detail.getEpisode() == null && detail.subEpisode.isNotEmpty()) {
                detail.subEpisode.first().activated = true
                findEp = detail.subEpisode.first()
                log.debug("默认激活第一个剧集: {}", detail.subEpisode.first().name)
            }

            scope.launch {
                val newHistory = Db.History.create(
                    detail,
                    detail.currentFlag.flag!!,
                    detail.getEpisode()?.name ?: detail.vodName!!
                )
                log.debug("[StartPlay]创建新历史记录完成: {}", newHistory)
                controller.setControllerHistory(newHistory)
            }
        } else {
            log.debug(
                "[StartPlay]找到历史记录: vodRemarks={}, vodFlag={}, position={}",
                history.vodRemarks, history.vodFlag, history.position
            )

            // 设置视频播放的起始和结束时间（处理null值）
            val opening = history.opening ?: -1
            val ending = history.ending ?: -1
            log.debug(
                "[StartPlay]设置片头片尾时间: opening={}, ending={}",
                opening, ending
            )
            controller.setStartEnd(opening, ending)

            // 根据历史记录查找要播放的剧集 - 优先使用历史记录的剧集名称
            log.debug(
                "[StartPlay]根据历史记录查找剧集: vodRemarks={}, currentEpNumber={}",
                history.vodRemarks, currentEpisodeIndex
            )

            // 确保历史记录中的线路在当前vodFlags中存在并激活
            val historyFlag = detail.vodFlags.find { it.flag == history.vodFlag }
            if (historyFlag != null) {
                // 激活历史记录中的线路
                for (flag in detail.vodFlags) {
                    flag.activated = flag.flag == history.vodFlag
                }
                detail.currentFlag = historyFlag
                _currentFlagName.value = historyFlag.flag.toString()
                log.debug("[StartPlay]根据历史记录激活线路: {}", historyFlag.flag)
            }

            // 优先查找历史记录中指定的剧集
            findEp = detail.currentFlag.episodes.find { it.name == history.vodRemarks }

            // 如果没找到匹配的剧集名称，再使用原来的查找方法
            if (findEp == null) {
                findEp = detail.findAndSetEpByName(history, currentEpisodeIndex)
            } else {
                // 找到匹配的剧集后，更新UI状态
                updateEpisodeActivation(findEp)
            }

            log.debug("[StartPlay]根据历史记录查找剧集结果: {}", findEp?.name ?: "未找到")

            // 更新状态流中的视频详情信息
            _state.update { it.copy(detail = detail) }

            // 确保控制器历史记录是最新的
            controller.setControllerHistory(history)
        }

        return findEp
    }

    /**
     * 返回下一集的链接，并更新详情页状态。
     * 1. 查找当前激活的剧集，计算下一集的索引。
     * 2. 如果当前分组已播完最后一集，自动切换到下一分组。
     * 3. 如果没有更多剧集，返回 null。
     */
    fun getNextEpisodeUrl(): String? {
        synchronized(nextEpisodeLock) {
            val currentDetail = _state.value.detail
            val currentEp = currentDetail.subEpisode.find { it.activated }
            var nextTabIndex = currentDetail.currentTabIndex

            // 如果没有激活的剧集，从当前分组的第一个开始
            if (currentEp == null) {
                val firstEp = currentDetail.subEpisode.firstOrNull() ?: return null
                log.debug("没有激活的剧集，从当前分组的第一个开始更新ui激活剧集{}", firstEp.name)
                updateEpisodeActivation(firstEp)

                // 调用 updateEpisodeUI 刷新页面
                updateEpisodeUI(firstEp)

                return decryptUrl(firstEp.url)
            }

            val currentIndex = currentDetail.subEpisode.indexOf(currentEp)
            val nextIndex = currentIndex + 1

            // 处理分组切换
            if (nextIndex >= currentDetail.subEpisode.size) {
                nextTabIndex++

                // 计算总分组数
                val totalEpisodes = currentDetail.currentFlag.episodes.size
                val totalPages = (totalEpisodes + Constants.EpSize - 1) / Constants.EpSize

                // 检查是否有更多分组
                if (nextTabIndex >= totalPages) {
                    return null // 没有更多剧集
                }

                // 切换到下一分组
                val start = nextTabIndex * Constants.EpSize
                val end = minOf(start + Constants.EpSize, totalEpisodes)
                val newSubEpisodes = currentDetail.currentFlag.episodes.subList(start, end)

                val newFirstEp = newSubEpisodes.firstOrNull() ?: return null

                updateEpisodeActivation(newFirstEp, nextTabIndex, newSubEpisodes)

                updateEpisodeUI(newFirstEp)

                return decryptUrl(newFirstEp.url)
            }

            // 正常切换到下一集
            val nextEp = currentDetail.subEpisode[nextIndex]
            log.debug("切换下一集更新ui激活剧集{}", nextEp.name)
            updateEpisodeActivation(nextEp)
            //在数据库中更新数据
            updateHistoryWithNewEpisode(nextEp)
            updateEpisodeUI(nextEp)
            return decryptUrl(nextEp.url)
        }
    }

    // 轻量级UI更新方法
    private fun updateEpisodeUI(episode: Episode) {
        _state.update { state ->
            val updatedSubEpisodes = state.detail.subEpisode.map { ep ->
                ep.copy(activated = ep == episode)
            }.toMutableList()

            state.copy(
                detail = state.detail.copy(subEpisode = updatedSubEpisodes),
                currentEp = episode
            )
        }
    }


    /**
     * 更新剧集中激活状态和当前选中的剧集信息。
     * 该方法会根据传入的参数更新视频详情中的剧集激活状态，
     * 并可选择更新当前的标签索引和剧集列表。
     *
     * @param activeEp 要激活的剧集对象，会将该剧集标记为激活状态。
     * @param newTabIndex 可选参数，新的标签索引。若不为 null，则更新视频详情中的当前标签索引。
     * @param newSubEpisodes 可选参数，新的剧集列表。若不为 null，则使用该列表更新视频详情中的子剧集列表。
     */
    private fun updateEpisodeActivation(
        activeEp: Episode,
        newTabIndex: Int? = null,
        newSubEpisodes: List<Episode>? = null
    ) {
        // 更新当前选中的剧集编号
        currentSelectedEpNumber = activeEp.number
        // 更新状态流中的状态
        _state.update { state ->
            // 先清除所有剧集的激活状态
            state.detail.currentFlag.episodes.forEach { episode ->
                episode.activated = false
            }

            // 激活指定的剧集
            activeEp.activated = true

            // 根据 newSubEpisodes 是否为 null 来决定使用哪个剧集列表更新激活状态
            val updatedSubEpisodes = newSubEpisodes?.map { ep ->
                // 若 newSubEpisodes 不为 null，遍历该列表，将与 activeEp 相同URL的剧集标记为激活状态
                ep.copy(activated = ep.url == activeEp.url)
            }?.toMutableList() ?: state.detail.subEpisode.map { ep ->
                // 若 newSubEpisodes 为 null，遍历当前视频详情中的子剧集列表，将与 activeEp 相同URL的剧集标记为激活状态
                ep.copy(activated = ep.url == activeEp.url)
            }.toMutableList()

            // 返回更新后的状态
            state.copy(
                detail = state.detail.copy(
                    // 若 newTabIndex 不为 null，则更新当前标签索引，否则保持原索引不变
                    currentTabIndex = newTabIndex ?: state.detail.currentTabIndex,
                    // 使用更新后的剧集列表更新视频详情中的子剧集列表
                    subEpisode = updatedSubEpisodes
                ),
                // 更新当前选中的剧集为 activeEp
                currentEp = activeEp,
            )
        }
    }


    /**
     * 根据新选中的剧集更新播放历史记录。
     * 该方法会在协程中检查是否存在已有的历史记录，若存在则更新，不存在则创建新的历史记录。
     * 最终将更新或创建后的历史记录设置到控制器中。
     * @param ep 新选中的剧集对象，包含剧集的名称和 URL 等信息。
     */
    private fun updateHistoryWithNewEpisode(ep: Episode) {
        val currentDetail = _state.value.detail             // 获取当前视频详情信息
        log.debug("开始更新历史记录，当前选中剧集: {}", ep.name)
        scope.launch {
            val existingHistory = controller.getControllerHistory()// 从控制器中获取已有的历史记录
            if (existingHistory != null) {
                log.debug("检测到已有历史记录，准备更新...")
            } else {
                log.debug("未检测到历史记录，准备创建新的历史记录...")
            }
            // 根据是否存在已有历史记录，决定是更新还是创建新的历史记录
            val history = existingHistory?.copy(
                episodeUrl = ep.url,                        // 更新剧集 URL
                vodRemarks = ep.name,                       // 更新剧集备注信息
                vodFlag = currentDetail.currentFlag.flag,   // 更新视频标识
                position = 0L                               // 重置播放位置为 0
            ) ?: run {
                // 构建新历史记录的唯一 key
                val key =
                    "${currentDetail.site?.key}${Db.SYMBOL}${currentDetail.vodId}${Db.SYMBOL}${ApiConfig.api.cfg?.id}"
                log.debug("新历史记录 key: {}", key)           // 记录新历史记录的 key
                History(                                     // 构建历史记录对象
                    key = key,                               // 历史记录 key
                    vodPic = currentDetail.vodPic ?: "",     // 设置视频封面图，若为空则使用空字符串
                    vodName = currentDetail.vodName!!,       // 设置视频名称
                    vodFlag = currentDetail.currentFlag.flag,// 设置视频标识
                    vodRemarks = ep.name,                    // 设置剧集备注信息
                    episodeUrl = ep.url,                     // 设置剧集 URL
                    cid = ApiConfig.api.cfg?.id!!,           // 设置分类 ID
                    createTime = System.currentTimeMillis(), // 设置历史记录创建时间为当前系统时间
                    position = 0L                            // 初始化播放位置为 0
                )
            }
            log.debug("即将设置更新后的历史记录: {}", history)
            controller.setControllerHistory(history)         // 将更新或创建后的历史记录设置到控制器中
            log.debug("历史记录更新完成")
        }
    }


    /**
     * 根据传入的 URL 进行解密处理，获取解密后的 URL 字符串。
     * 该方法会调用 SiteViewModel 的 playerContent 方法，结合当前视频详情的站点 key 和当前选中的视频标识，
     * 获取播放内容信息，最终返回解密后的 URL 字符串。若过程中出现空值，则返回 null。
     * @param url 需要进行解密处理的原始 URL 字符串。
     * @return 解密后的 URL 字符串，若获取失败则返回 null。
     */
    private fun decryptUrl(url: String): String? {
        // 调用 SiteViewModel 的 playerContent 方法，传入当前视频详情的站点 key、当前选中的视频标识和原始 URL
        // 获取播放内容信息，若结果不为空，则进一步获取其 URL 并调用 v() 方法，最终返回解密后的 URL 字符串
        // 若过程中任意环节出现空值，则返回 null
        return SiteViewModel.playerContent(
            _state.value.detail.site?.key ?: "",
            _state.value.detail.currentFlag.flag ?: "",
            url
        )?.url?.v()
    }


    /**
     * 尝试播放下一集视频。
     * 该方法会根据当前激活的剧集，计算出下一集的索引，
     * 若当前分组播放完毕则切换到下一个分组，
     * 若没有更多剧集则提示用户，
     * 最后调用 `playEp` 方法播放下一集。
     */
    fun nextEP() {
        log.info("加载下一集")
        val detail = _state.value.detail
        var nextIndex: Int
        var currentIndex: Int
        val currentEp = detail.subEpisode.find { it.activated }
        val totalEpisodes = detail.currentFlag.episodes.size

        log.debug("当前激活的剧集: {}", currentEp)
        controller.doWithHistory { it.copy(position = 0) }

        if (currentEp != null) {
            currentIndex = detail.subEpisode.indexOf(currentEp)
            nextIndex = currentIndex + 1
            currentSelectedEpNumber = currentEp.number
        } else {
            log.debug("当前没有激活的剧集")
            SnackBar.postMsg("当前没有激活的剧集", type = SnackBar.MessageType.WARNING)
            return
        }
        // 若总剧集数量小于等于下一集的索引，说明没有更多剧集了
        if (totalEpisodes <= nextIndex) {
            SnackBar.postMsg("没有更多剧集", type = SnackBar.MessageType.INFO)
            return
        }
        if (currentIndex >= Constants.EpSize - 1) {// 若当前剧集索引达到或超过每个分组的剧集数量上限
            log.info("当前分组播放完毕 下一个分组")
            // 检查是否还有下一个分组
            val nextTabIndex = detail.currentTabIndex + 1
            val totalPages = (totalEpisodes + Constants.EpSize - 1) / Constants.EpSize
            if (nextTabIndex >= totalPages) {
                return
            }
            // 更新状态流中的视频详情信息，切换到下一个分组的子剧集列表
            _state.update {
                it.copy(
                    detail = detail.copy(
                        subEpisode = detail.currentFlag.episodes.getPage(nextTabIndex),
                        currentTabIndex = nextTabIndex
                    ),
                    isLoading = false,
                    isBuffering = false
                )
            }
            // 更新 currentSelectedEpNumber 为新分组的第一集
            val newFirstEp = detail.currentFlag.episodes[nextTabIndex * Constants.EpSize]
            currentSelectedEpNumber = newFirstEp.number
            playEp(detail, newFirstEp)// 切换分组后播放下一集,调用playEp
            return
        }
        val nextEp = detail.subEpisode[nextIndex]
        currentSelectedEpNumber = nextEp.number
        playEp(detail, detail.subEpisode[nextIndex])// 播放下一集
    }

    /**
     * 尝试切换到下一个视频播放线路。
     *
     * 该方法会查找下一个可用的播放线路，若存在则切换到该线路并播放相应剧集；
     *
     * 若下一个线路为空，提示用户没有更多线路；
     *
     * 若下一个线路有效但为空，清空视频 ID 并执行快速搜索。
     */
    fun nextFlag() {
        _state.update { it.copy(isLoading = true, isBuffering = false) }
        log.info("nextFlag")

        if (_state.value.detail.vodFlags.size > 1) {// 检查是否有多余的线路可以切换
            SnackBar.postMsg("加载数据失败，尝试切换线路", type = SnackBar.MessageType.WARNING)
        } else {
            _state.update { it.copy(isLoading = false, isBuffering = false) }
            SnackBar.postMsg("加载数据失败", type = SnackBar.MessageType.ERROR)
            return
        }

        // 获取当前视频详情信息的副本
        val detail = _state.value.detail.copy()

        val nextFlag = _state.value.detail.nextFlag()   // 获取下一个可用线路
        if (nextFlag == null) {                         // 下一个可用线路为空
            log.info("没有更多线路可切换，当前线路数: {}", _state.value.detail.vodFlags.size)
            SnackBar.postMsg("没有更多线路", type = SnackBar.MessageType.WARNING)
            _state.update { it.copy(detail = it.detail.copy(), isLoading = false, isBuffering = false) }
            return
        }

        // 更新当前线路（Vod.nextFlag() 已经处理了激活状态的更新）
        detail.currentFlag = nextFlag
        _currentFlagName.value = nextFlag.flag.toString()

        // 如果当前播放线路为空
        if (detail.currentFlag.isEmpty()) {
            // 清空视频 ID，以便快速搜索时重新加载详情
            detail.vodId = ""
            // 执行快速搜索操作
            quickSearch()
            // 结束当前方法
            return
        }

        // 更新子剧集列表为当前播放线路对应页的剧集
        detail.subEpisode = detail.currentFlag.episodes.getPage(_state.value.detail.currentTabIndex).toMutableList()
        // 更新控制器的历史记录，记录当前播放线路标识
        controller.doWithHistory { it.copy(vodFlag = detail.currentFlag.flag) }
        // 将全局应用状态中选中的视频信息更新为当前视频详情信息
        GlobalAppState.chooseVod.value = detail.copy()
        // 更新状态流中的视频详情信息
        _state.update { it.copy(detail = detail, isLoading = false, isBuffering = false) }
        // 提示用户已切换至新的播放线路
        SnackBar.postMsg("切换至线路[${detail.currentFlag.flag}]", type = SnackBar.MessageType.INFO)
        // 根据控制器的历史记录查找对应的剧集
        val findEp = detail.findAndSetEpByName(controller.history.value!!, currentEpisodeIndex)
        // 调用 playEp 方法播放找到的剧集，若未找到则播放子剧集列表中的第一个剧集
        scope.launch {
            delay(500)
            playEp(detail, findEp ?: detail.subEpisode.first())//自动切换线路并播放
        }
    }


    /**
     * 同步视频播放的历史记录。
     * 该方法会从数据库中查找当前视频的历史记录，根据不同情况进行处理：
     * 若历史记录不存在，则创建新的历史记录；
     * 若历史记录存在，且当前选中剧集与历史记录中的剧集不同且历史记录有播放位置，
     * 则重置播放位置为 0。最后将历史记录设置到控制器中，并更新状态流中的相关信息。
     */
    fun syncHistory() {
        // 获取当前视频详情信息
        val detail = _state.value.detail
        log.debug("开始同步历史记录，视频ID: {}, 站点: {}", detail.vodId, detail.site?.key)

        // 在协程作用域中启动一个协程来处理历史记录同步操作
        scope.launch {
            // 根据当前视频的站点 key 和视频 ID 从数据库中查找历史记录
            var history = Db.History.findHistory(Utils.getHistoryKey(detail.site?.key!!, detail.vodId))
            log.debug("数据库查询历史记录结果: {}", if (history == null) "未找到" else "找到记录")

            // 若历史记录不存在
            if (history == null) {
                // 使用当前视频详情信息创建新的历史记录
                log.debug("未找到历史记录，创建新的历史记录")
                val newHistory =
                    Db.History.create(detail, detail.currentFlag.flag!!, detail.getEpisode()?.name ?: detail.vodName!!)
                log.debug("新历史记录创建完成: {}", newHistory)
            } else {
                // 若当前选中剧集名称与历史记录中的剧集名称不同，且历史记录中有播放位置
                log.debug("现有历史记录: vodRemarks={}, position={}", history.vodRemarks, history.position)
                log.debug("当前选中剧集: {}", _state.value.currentEp?.name)

                if (!_state.value.currentEp?.name.equals(history.vodRemarks) && history.position != null) {
                    log.debug(
                        "剧集名称发生变化且有播放位置，重置播放位置: {} -> {}",
                        history.vodRemarks, _state.value.currentEp?.name
                    )
                    // 重置播放位置为 0
                    history = history.copy(position = 0L)
                }

                // 将更新后的历史记录设置到控制器中
                log.debug(
                    "设置控制器历史记录: vodFlag={}, vodRemarks={}, position={}",
                    history.vodFlag, history.vodRemarks, history.position
                )
                controller.setControllerHistory(history)

                // 设置视频播放的起始和结束时间，若未设置则使用默认值 -1
                log.debug(
                    "设置片头片尾时间: opening={}, ending={}",
                    history.opening, history.ending
                )
                controller.setStartEnd(history.opening ?: -1, history.ending ?: -1)

                // 根据历史记录查找对应的剧集
                log.debug(
                    "根据历史记录查找剧集: vodRemarks={}, currentEpNumber={}",
                    history.vodRemarks, currentEpisodeIndex
                )
                val findEp = detail.findAndSetEpByName(history, currentEpisodeIndex)
                log.debug("查找剧集结果: {}", findEp?.name ?: "未找到")

                // 在默认调度器中更新状态流中的信息
                withContext(Dispatchers.Default) {
                    // 更新状态流中的视频详情、当前剧集和当前播放 URL 信息
                    _state.update {
                        it.copy(
                            detail = detail,
                            currentPlayUrl = findEp?.url ?: "",
                            currentEp = findEp,
                            isLoading = false,
                            isBuffering = false
                        )
                    }
                    log.debug("状态更新完成，当前播放URL: {}", findEp?.url ?: "")
                }
            }
        }
    }


    /**
     * 切换剧集选择对话框的显示状态。
     * 调用该方法时，会将当前剧集选择对话框的显示状态取反。
     * 若对话框当前显示，则隐藏；若当前隐藏，则显示。
     */
    fun clickShowEp() {
        // 使用 _state.update 更新状态流的值
        _state.update {
            // 复制当前状态，将 showEpChooserDialog 取反
            it.copy(showEpChooserDialog = !_state.value.showEpChooserDialog, isLoading = false, isBuffering = false)
        }
    }


    /**
     * 切换视频播放线路
     *
     * 当用户选择不同的播放线路时调用此方法。
     * 负责更新线路状态、重置播放器、并根据历史记录自动播放对应剧集。
     *
     * @param detail 当前视频详情对象
     * @param it 用户选择的新线路对象
     */
    fun chooseFlag(detail: Vod, it: Flag) {
        // 在协程作用域中启动异步任务处理线路切换
        scope.launch {
            val oldNumber = currentSelectedEpNumber                 //同步旧的剧集数据，用于更新按钮选中状态
            val newEpisodes = it.episodes                           //获取新的剧集列表
            val newEp = newEpisodes.find { it.number == oldNumber } ?: newEpisodes.firstOrNull() //获取新的剧集对象
            currentSelectedEpNumber = newEp?.number ?: 1 //设置新的剧集编号

            log.debug("chooseFlag -- 切换线路，新的线路标识: ${it.flag},剧集编号${newEp?.number}")

            _currentFlagName.value = it.flag.toString()  //更新线路标识

            _state.update { it.copy(isLoading = true, isBuffering = false) }

            val endedDeferred = async {
                try {
                    withTimeout(3000) { // 3秒超时
                        lifecycleManager.endedAsync()
                    }
                } catch (e: TimeoutCancellationException) {
                    SnackBar.postMsg("关闭媒体超时！请稍后切换线路重试...", type = SnackBar.MessageType.ERROR)
                    log.error("关闭媒体超时,等待播放器缓存完成...")
                    throw e
                }
            }

            try {
                // 添加7秒超时检测，如果切换线路超过7秒则触发切换线路失败
                withTimeout(7000) {
                    // 步骤1: 更新所有线路的激活状态
                    for (vodFlag in detail.vodFlags) {           // 遍历视频的所有可用播放线路
                        if (it.show == vodFlag.show) {           // 匹配用户选择的线路
                            vodFlag.activated = true             // 激活选中的线路
                            detail.currentFlag = vodFlag         // 设置为当前播放线路
                        } else {
                            vodFlag.activated = false            // 取消其他线路的激活状态
                        }
                    }

                    // 步骤2: 计算新的分组索引
                    var newTabIndex = detail.currentTabIndex     // 查找之前选中的剧集在新线路中的位置
                    if (newEp != null) {
                        val newEpisodeIndex = newEpisodes.indexOfFirst { ep -> ep.number == newEp.number }
                        if (newEpisodeIndex != -1) {
                            newTabIndex = newEpisodeIndex / Constants.EpSize  // 计算新剧集所在的分组索引
                        }
                    }

                    // 如果新分组索引超出范围，则设置为最后一组
                    val maxTabIndex = if (newEpisodes.isNotEmpty()) {
                        (newEpisodes.size - 1) / Constants.EpSize
                    } else {
                        0
                    }
                    if (newTabIndex > maxTabIndex) {
                        newTabIndex = maxTabIndex
                    }

                    // 步骤3: 创建更新后的视频详情对象
                    // 复制视频详情，更新当前线路和对应剧集列表
                    val dt = detail.copy(
                        currentFlag = detail.currentFlag,
                        currentTabIndex = newTabIndex,
                        // 根据新的标签页索引获取对应页码的剧集
                        subEpisode = detail.currentFlag.episodes.getPage(newTabIndex).toMutableList()
                    )

                    // 步骤4: 更新观看历史记录
                    // 在历史记录中保存当前选择的线路标识
                    controller.doWithHistory { it.copy(vodFlag = detail.currentFlag.flag) }

                    // 等待 ended 操作完成
                    endedDeferred.await()

                    // 步骤5: 根据历史记录自动播放
                    val history = controller.history.value
                    if (history != null) {
                        // 在历史记录中查找上次观看的剧集数据
                        val findEp = dt.findAndSetEpByName(controller.history.value!!, oldNumber)
                        log.debug("切换线路，新的剧集数据: {}", findEp)
                        if (findEp != null) {
                            playEp(dt, findEp)//手动选择线路后根据历史记录自动播放
                        }
                    }

                    // 步骤6: 更新最终状态
                    _state.update { model ->      // 更新状态流：设置新详情、标记需要播放、取消缓冲状态
                        model.copy(
                            detail = dt,
                            isLoading = false,    // 取消加载状态
                            isBuffering = false,  // 取消缓冲状态
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                // 7秒超时处理：触发切换线路失败
                log.error("切换线路超时(7秒)，切换失败", e)
                SnackBar.postMsg("切换线路超时，切换失败", type = SnackBar.MessageType.ERROR)
                _state.update { it.copy(isLoading = false, isBuffering = false) }
            } catch (e: Exception) {
                // 异常处理：记录错误并提示用户
                log.error("切换线路失败", e)
                SnackBar.postMsg("切换线路失败: ${e.message}", type = SnackBar.MessageType.ERROR)
                _state.update { it.copy(isLoading = false, isBuffering = false) }
            }
        }
    }


    /**
     * 清晰度选择
     * 选择播放级别并更新状态流中的播放 URL 信息。
     * 该方法会根据传入的 URL 对象和播放 URL 字符串，
     * 更新状态流中的当前 URL 和当前播放 URL 信息。
     * 若传入的播放 URL 字符串为 null，则使用空字符串作为当前播放 URL。
     *
     * @param i 可选的 URL 对象，用于更新状态流中的当前 URL。
     * @param v 可选的播放 URL 字符串，用于更新状态流中的当前播放 URL。若为 null，则使用空字符串。
     */
    fun chooseLevel(i: Url?, v: String?) {
        scope.launch {
            _state.update { it.copy(isLoading = true, isBuffering = false) }
            log.debug("切换清晰度，结束播放,当前播放器状态: {}", lifecycleManager.lifecycleState.value)

            try {
                // 确保在状态转换前清理旧资源
                controller.cleanupBeforeQualityChange()

                lifecycleManager.transitionTo(Ended) { lifecycleManager.ended() }
                lifecycleManager.transitionTo(Ready) { lifecycleManager.ready() }

                _state.update {
                    it.copy(
                        currentPlayUrl = v ?: "",
                        currentUrl = i,
                    )
                }
            } catch (e: Exception) {
                log.error("切换清晰度时发生错误", e)
                SnackBar.postMsg("切换清晰度失败: ${e.message}", type = SnackBar.MessageType.ERROR)
            }
        }.invokeOnCompletion {
            _state.update { it.copy(isLoading = false, isBuffering = false) }
        }
    }


    /**
     * 隐藏剧集选择对话框。
     *
     * 该方法会更新状态流中的 `showEpChooserDialog` 字段为 `false`，
     *
     * 以此来控制界面上剧集选择对话框的显示状态，使其隐藏。
     */
    fun showEpChooser() {
        _state.update {
            it.copy(showEpChooserDialog = !it.showEpChooserDialog)
        }
    }

    /**
     * 根据传入的索引切换到对应的剧集分组。
     *
     * 该方法会根据传入的索引计算出新的标签索引，
     *
     * 并获取该标签索引对应的剧集列表，最后更新状态流中的视频详情信息。
     *
     * @param i 用于计算新标签索引的整数，通过该值除以 [Constants.EpSize] 得到新的标签索引。
     */
    fun chooseEpBatch(i: Int) {
        // 获取当前状态中的视频详情信息
        val detail = state.value.detail
        // 记录当前全局激活的剧集URL，用于后续恢复激活状态
        val currentGlobalActiveEpisodeUrl = _state.value.currentEp?.url
        log.debug("批量选择剧集，当前全局激活剧集url: {}", currentGlobalActiveEpisodeUrl)

        // 计算新的标签索引，通过传入的索引除以每个分组的剧集数量得到
        val newTabIndex = i / Constants.EpSize

        // 获取新标签索引对应的剧集列表
        val newSubEpisodes = detail.currentFlag.episodes.getPage(newTabIndex).toMutableList()

        // 恢复之前激活的剧集状态
        if (currentGlobalActiveEpisodeUrl != null) {
            newSubEpisodes.forEach { episode ->
                if (episode.url == currentGlobalActiveEpisodeUrl) {
                    episode.activated = true
                } else {
                    episode.activated = false
                }
            }
        } else if (newSubEpisodes.isNotEmpty()) {
            // 如果之前没有激活的剧集，则默认激活新分组的第一个剧集
            newSubEpisodes[0].activated = true
        }

        val dt = detail.copy(
            currentTabIndex = newTabIndex,
            subEpisode = newSubEpisodes
        )
        // 更新状态流中的视频详情信息
        _state.update { it.copy(detail = dt, isLoading = false, isBuffering = false) }
    }


    val videoLoading = mutableStateOf(false)    //ui状态：视频加载中

    /**
     * 选择指定剧集进行播放操作，根据剧集链接类型和播放器设置执行不同播放逻辑。
     * 在操作开始时标记视频加载中，操作完成后标记加载结束。
     *
     * @param it 要选择播放的剧集对象，包含剧集的名称、URL 等信息。
     * @param openUri 一个函数，用于处理打开特定 URI 的操作，传入参数为 URI 字符串。
     */
    fun chooseEp(it: Episode, openUri: (String) -> Unit) {
        log.debug("切换剧集: {}", it)
        videoLoading.value = true        // ui层面标记视频正在加载

        currentSelectedEpNumber = it.number // 记录当前选中剧集的编号

        scope.launch {
            val isDownloadLink = Utils.isDownloadLink(it.url)
            val currentDetail = _state.value.detail // 获取当前 detail

            // 更新 currentFlag.episodes 的 activated 状态
            val updatedCurrentFlagEpisodes = currentDetail.currentFlag.episodes.map { episode ->
                episode.copy(activated = (episode.url == it.url))
            }.toMutableList()

            // 更新 subEpisode 的 activated 状态
            val updatedSubEpisodes = currentDetail.subEpisode.map { episode ->
                episode.copy(activated = (episode.url == it.url)) // 同样使用 copy
            }.toMutableList()

            // 创建更新后的 detail
            val updatedDetail = currentDetail.copy(
                currentFlag = currentDetail.currentFlag.copy(
                    episodes = updatedCurrentFlagEpisodes
                ),
                subEpisode = updatedSubEpisodes
            )

            for (i in updatedDetail.currentFlag.episodes) { // 遍历更新后的列表
                log.debug("剧集: {}, 选中: {}", i.name, i.activated)
                if (i.activated) {
                    _state.update { model ->
                        val newModel = model.copy(currentEp = i, detail = updatedDetail) // 更新 currentEp 和 detail

                        if (!isDownloadLink) {
                            if (model.currentEp?.name != it.name) {
                                controller.doWithHistory { it.copy(position = 0L) }
                            }
                            controller.doWithHistory {
                                it.copy(episodeUrl = i.url, vodRemarks = i.name)
                            }
                        }
                        newModel
                    }
                    break // 找到并处理了激活的剧集后退出循环
                }
            }
            if (isDownloadLink) {                            // 若当前选中剧集的 URL 是下载链接
                openUri(it.url)                              // 调用 openUri 函数处理下载链接
                return@launch                                // 结束当前协程
            } else {
                val correctDetailForPlay = _state.value.detail // 获取包含正确 activated 状态的最新 detail
                playEp(correctDetailForPlay, it) // 使用最新的 detail 来播放
            }
        }.invokeOnCompletion { videoLoading.value = false }
    }


    /**
     * 设置当前播放的 URL。
     * 该方法会更新状态流中的 `currentPlayUrl` 字段为传入的 URL 字符串。
     *
     * @param string 要设置的播放 URL 字符串。
     */

    private val playerStateLock = Mutex()
    fun setPlayUrl(string: String) {
        _state.update { it.copy(isLoading = true) }

        log.debug("<DLNA> 开始播放")

        if (vmPlayerType.first() == PlayerType.Outie.id) {
            Play.start(string, "LumenTV-DLNA")
            _state.update { it.copy(isLoading = false) }
            return
        }

        scope.launch {
            // 加锁确保状态转换原子性
            playerStateLock.withLock {
                when (lifecycleManager.lifecycleState.value) {// 检查当前状态
                    Idle -> {
                        log.debug("播放器未初始化，开始初始化...")
                        lifecycleManager.initializeSync().onSuccess {
                            proceedToPlay(string)
                        }
                    }

                    Playing -> {
                        log.warn("播放器正在播放，先停止当前播放")
                        lifecycleManager.stop().onSuccess {
                            proceedToPlay(string)
                        }
                    }

                    else -> {
                        log.debug("播放器已初始化，直接播放")
                        proceedToPlay(string)
                    }
                }
            }
        }.invokeOnCompletion { _state.update { it.copy(isLoading = false) } }
    }

    /**
     * DLNA —— 状态检查，并更新URL
     * */
    private suspend fun proceedToPlay(url: String) {
        // 添加状态转换检查
        if (lifecycleManager.canTransitionTo(Loading)) {
            lifecycleManager.loading()
        }

        if (lifecycleManager.canTransitionTo(Ready)) {
            lifecycleManager.ready()
        }

        // 如果已经在播放相同URL，跳过
        if (lifecycleManager.lifecycleState.value == Playing && _state.value.currentPlayUrl == url) {
            log.debug("已经在播放相同URL，跳过")
            return
        }

        lifecycleManager.start().onSuccess {
            _state.update {
                it.copy(currentPlayUrl = url, isDLNA = true)
            }
        }
    }
}