package com.corner.ui.nav.vm

import SiteViewModel
import androidx.compose.runtime.*
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.bean.enums.PlayerType
import com.corner.bean.getPlayerSetting
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.getPage
import com.corner.catvod.enum.bean.Vod.Companion.isEmpty
import com.corner.catvodcore.bean.*
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.DetailFromPage
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.database.Db
import com.corner.database.entity.History
import com.corner.server.KtorD
import com.corner.ui.nav.BaseViewModel
import com.corner.ui.nav.data.DetailScreenState
import com.corner.ui.nav.data.DialogState.isSpecialVideoLink
import com.corner.ui.player.PlayState
import com.corner.ui.player.PlayerLifecycleManager
import com.corner.ui.player.vlcj.VlcJInit
import com.corner.ui.player.vlcj.VlcjFrameController
import com.corner.ui.scene.SnackBar
import com.corner.util.Constants
import com.corner.util.cancelAll
import com.corner.util.play.Play
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.sync.Semaphore
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


    // 单独创建lifecycleManager，在init块中设置controller
    private var lifecycleManager: PlayerLifecycleManager = PlayerLifecycleManager(controller, scope)

    // 添加生命周期管理器
    val currentSelectedEpUrl = mutableStateOf<String?>(null) // 新增状态记录


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
//                    log.info("历史记录更新成功")
                } catch (e: Exception) {
                    log.error("历史记录更新失败", e)
                }
            }
        }
    }

    //下一集锁
    private val nextEpisodeLock = Object()


    init {
        // 监控播放器状态变化
        scope.launch {
            controller.state.collect { playerState ->
                when (playerState.state) {
                    PlayState.ERROR -> {
                        // 处理播放错误
                        log.error("播放错误: ${playerState.msg}")
                    }

                    PlayState.BUFFERING -> {
                        // 显示缓冲状态
                        _state.update { it.copy(isBuffering = true) }
                    }

                    else -> {
                        _state.update { it.copy(isBuffering = false) }
                    }
                }
            }
        }
    }

    /**
     * 加载详情页数据并根据不同来源执行相应操作。
     * 此方法会显示加载进度，初始化播放器控制器，根据详情页来源（搜索页或其他）
     * 加载不同的数据，最后隐藏加载进度。
     */
    suspend fun load() {
        // 显示加载进度指示器
        showProgress()

        // 初始化播放器控制器
        lifecycleManager.initialize_sync()

        // 获取当前选中的视频信息
        val chooseVod = getChooseVod()
        // 更新状态流中的详情信息为当前选中的视频信息
        _state.update { it.copy(detail = chooseVod, isLoading = true) }
        // 更新当前站点的 key
        currentSiteKey.value = chooseVod.site?.key ?: ""
        try {
            // 在 SiteViewModel 的协程作用域中启动一个协程
            SiteViewModel.viewModelScope.launch {
                // 检查详情页是否来自搜索页
                if (GlobalAppState.detailFrom == DetailFromPage.SEARCH) {
                    // 获取搜索结果中的激活列表
                    val list = SiteViewModel.getSearchResultActive().list
                    // 更新状态流中的快速搜索结果和详情信息
                    _state.update {
                        it.copy(
                            detail = chooseVod,
                            quickSearchResult = CopyOnWriteArrayList(list),
                            isLoading = false
                        )
                    }
                    // 在 SiteViewModel 的协程作用域中启动一个新协程
                    fromSearchLoadJob = SiteViewModel.viewModelScope.launch {
                        // 若快速搜索结果不为空，则加载详情信息
                        if (_state.value.quickSearchResult.isNotEmpty()) _state.value.detail.let { loadDetail(it) }
                    }
                } else {
                    // 更新状态流中的加载状态为正在加载
                    _state.update { it.copy(isLoading = true, isBuffering = false) }
                    // 获取视频详情内容
                    val dt = SiteViewModel.detailContent(chooseVod.site?.key ?: "", chooseVod.vodId)
                    // 更新状态流中的加载状态为加载完成
                    _state.update { it.copy(isLoading = false, isBuffering = false) }
                    // 若视频 ID 为空，则终止当前协程
                    if (chooseVod.vodId.isBlank()) return@launch
                    // 若详情内容为空或详情信息为空，则执行快速搜索
                    if (dt == null || dt.detailIsEmpty()) {
                        quickSearch()
                    } else {
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
                        _state.update { it.copy(detail = detail, isLoading = false) }

                        lifecycleManager.loading()

                        // 开始播放视频
                        startPlay()
                    }
                }
            }
        } finally {
            // 隐藏加载进度指示器
            hideProgress()
        }
    }

    /**
     * 加载指定视频的详细信息。
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
            // 获取视频对应的站点 key，使用安全调用符处理可能的空值
            val siteKey = vod.site?.key
            // 若站点 key 为空，记录错误日志并尝试加载下一个视频，然后结束当前函数
            if (siteKey == null) {
                log.error("视频站点 key 为空，无法加载详情")
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
                SnackBar.postMsg("请求详情为空 加载下一个站源数据")
                nextSite(vod)
            } else {
                // 从详情列表中取出第一个元素
                val first = dt.list[0]
                // 记录加载详情完成的日志
                log.info("加载详情完成 $first")
                // 为详情对象设置站点信息
                first.site = vod.site
                // 若详情对象为空，尝试加载下一个视频
                if (first.isEmpty()) {
                    nextSite(vod)
                } else {
                    // 若详情对象有效，设置详情信息
                    setDetail(first)
                    // 取消 supervisor 协程的所有子协程
                    supervisor.cancelChildren()
                    // 取消 jobList 中的所有协程任务并清空列表
                    jobList.cancelAll().clear()
                }
            }
        } finally {
            // 将 launched 标志置为 false
            launched = false
            hideProgress()
        }
    }


    /**
     * 执行快速搜索操作，从可切换的站点中搜索视频信息。
     * 该方法会并发地从多个站点进行搜索，限制同时执行的搜索任务数量，
     * 并在搜索完成后更新快速搜索结果。若搜索到有效结果，会加载首个结果的详情；
     * 若未搜索到结果，则提示用户暂无线路数据。
     */
    fun quickSearch() {
        // 更新状态流，标记当前正在进行快速搜索
        _state.update { it.copy(isLoading = true, isBuffering = false) }
        // 在搜索协程作用域中启动一个新的协程
        searchScope.launch {
            // 筛选出可切换的站点列表，并打乱顺序
            val quickSearchSites = ApiConfig.api.sites.filter { it.changeable == 1 }.shuffled()
            // 记录开始执行快速搜索的日志，包含参与搜索的站点名称
            log.debug("开始执行快搜 sites:{}", quickSearchSites.map { it.name }.toString())
            // 创建一个信号量，限制同时执行的搜索任务数量为 2
            val semaphore = Semaphore(2)
            // 遍历可搜索的站点列表
            quickSearchSites.forEach {
                // 为每个站点启动一个新的协程进行搜索
                val job = launch() {
                    // 获取信号量许可，若没有可用许可则挂起协程
                    semaphore.acquire()
                    try {
                        // 设置超时时间为 2500 毫秒，在超时时间内执行搜索操作
                        withTimeout(2500L) {
                            // 调用 SiteViewModel 的搜索方法进行搜索
                            SiteViewModel.searchContent(it, getChooseVod().vodName ?: "", true)
                            // 记录该站点搜索完成的日志
                            log.debug("{}完成搜索", it.name)
                        }
                    } finally {
                        // 释放信号量许可，允许其他协程获取许可
                        semaphore.release()
                    }
                }

                // 为每个搜索任务添加完成回调
                job.invokeOnCompletion { it ->
                    if (it != null) {
                        // 若协程执行过程中出现异常，记录错误日志
                        log.error("quickSearch 协程执行异常 msg:{}", it.message)
                    }
                    // 更新状态流，将搜索结果添加到快速搜索结果列表中
                    _state.update {
                        val list = CopyOnWriteArrayList<Vod>()
                        // 将搜索结果添加到列表中，避免重复元素
                        list.addAllAbsent(SiteViewModel.quickSearch.value[0].list)
                        it.copy(
                            quickSearchResult = list,
                        )
                    }
                    if (it == null) {
                        // 若协程正常完成，记录完成日志并输出结果列表大小
                        log.debug("一个job执行完毕 result size:{}", _state.value.quickSearchResult.size)
                    }

                    // 使用同步锁确保线程安全
                    synchronized(lock) {
                        // 若快速搜索结果不为空，详情信息为空，且未启动加载详情操作
                        if (_state.value.quickSearchResult.isNotEmpty() && (_state.value.detail.isEmpty()) && !launched) {
                            // 记录开始加载详情的日志
                            log.info("开始加载 详情")
                            // 标记已启动加载详情操作
                            launched = true
                            // 加载快速搜索结果中的第一个视频详情
                            loadDetail(_state.value.quickSearchResult[0])
                        }
                    }
                }
                // 将搜索任务添加到任务列表中
                jobList.add(job)
            }
            // 等待所有搜索任务完成
            jobList.forEach {
                it.join()
            }
            // 统一关闭加载指示器
            _state.update { it.copy(isLoading = false) }
            // 若快速搜索结果为空
            if (_state.value.quickSearchResult.isEmpty()) {
                // 更新状态流，将详情信息设置为全局选中的视频信息
                _state.update { it.copy(detail = GlobalAppState.chooseVod.value, isLoading = false) }
                // 提示用户暂无线路数据
                SnackBar.postMsg("暂无线路数据")
                hideProgress()
            }
        }.invokeOnCompletion {
            // 所有搜索任务完成后，更新状态流，标记搜索结束
            _state.update { it.copy() }
        }
    }


    /**
     * 尝试从快速搜索结果中加载下一个视频的详情。
     * 如果提供了上一个视频对象，会将其从快速搜索结果列表中移除，
     * 然后尝试加载剩余结果列表中的第一个视频详情。
     *
     * @param lastVod 上一个尝试加载详情但失败的视频对象，可为 null。
     */
    fun nextSite(lastVod: Vod?) {
        // 检查快速搜索结果列表是否为空
        if (_state.value.quickSearchResult.isEmpty()) {
            // 若为空，记录警告日志并结束函数
            log.warn("nextSite 快搜结果为空 返回")
            hideProgress()
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
     * 清理详情页相关资源和状态。
     * 可选择是否释放播放器控制器资源，默认会释放。
     *
     * @param releaseController 是否释放播放器控制器资源，默认为 true。
     */
    /**
     * 改进的清理方法，使用生命周期管理器
     */
    fun clear(releaseController: Boolean = true, onComplete: () -> Unit = {}) {
        log.debug("detail clear - 开始清理")

        // 创建一个延迟2秒显示进度条的任务
        var progressJob: Job? = null

        scope.launch(Dispatchers.IO) {
            try {
                // 延迟2秒后显示进度条
                progressJob = launch {
                    delay(2000L)
                    showProgress()
                }

                if (releaseController) {
                    // 使用串行清理，避免并发问题
                    // 特殊状态转换，将关闭视频放在了特殊清理方法中，且直接状态转换playing->cleanup
                    lifecycleManager.cleanup()
                        .onSuccess {
                            log.debug("生命周期清理完成")
                            // 确保清理完成后再释放
                            lifecycleManager.release()
                                .onSuccess { log.debug("生命周期释放完成") }
                                .onFailure { e -> log.error("生命周期释放失败", e) }
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

                // 重置状态
                _state.update {
                    it.copy()
                }

                SiteViewModel.clearQuickSearch()
                launched = false

                log.debug("detail clear - 清理完成")

                // 回调到主线程
                withContext(Dispatchers.Swing) {
                    onComplete()
                }
            } catch (e: Exception) {
                log.error("清理过程中出错", e)
            } finally {
                // 取消进度条显示任务
                progressJob?.cancel()
                hideProgress()
            }
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

    /**
     * 设置视频详情信息并准备播放新视频。
     * 若当前站点 key 与传入视频的站点 key 不一致，会提示用户正在切换站源。
     * 接着更新状态流中的详情信息，强制停止当前播放的视频，最后启动新视频的播放。
     *
     * @param vod 要设置的视频详情对象，包含视频的详细信息。
     */
    private fun setDetail(vod: Vod) {
        // 检查当前站点 key 是否与传入视频的站点 key 不同
        if (currentSiteKey.value != vod.site?.key) {
            // 若不同，提示用户正在切换站源至传入视频所在的站点
            SnackBar.postMsg("正在切换站源至 [${vod.site!!.name}]")
        }
        // 更新状态流中的详情信息
        _state.update {
            it.copy(
                // 复制传入的视频对象，并更新其子剧集信息
                detail = vod.copy(
                    // 从视频的第一个标志位获取对应页的剧集列表并转为可变列表
                    subEpisode = vod.vodFlags.first().episodes.getPage(vod.currentTabIndex).toMutableList()
                ),
            )
        }
        // 在开始播放新视频前，强制停止当前正在播放的视频
        scope.launch {
            lifecycleManager.stop()
                .onSuccess {
                    log.debug("setDetail - 停止播放成功")
                    lifecycleManager.ended()
                        .onSuccess {
                            log.debug("setDetail - 开始准备播放")
                            // 启动新视频的播放流程
                            startPlay()
                        }
                }
        }
    }


    /**
     * 根据播放结果处理播放逻辑。
     * 若播放结果为空或无效，提示用户并尝试切换到下一个可用线路；
     * 若播放结果有效，更新当前播放的 URL 和播放结果。
     *
     * @param result 播放结果对象，可能为 null。
     */
    fun play(result: Result?) {
        // 检查播放结果是否为空或者播放结果无效
        if (result == null || result.playResultIsEmpty()) {
            // 若为空或无效，提示用户加载内容失败，尝试切换线路
            SnackBar.postMsg("加载内容失败，尝试切换线路")
            // 调用 nextFlag 函数尝试切换到下一个可用线路
            nextFlag()
            // 结束当前函数执行
            return
        }
        // 使用协程处理异步操作
        scope.launch {
            try {
                // 显示加载状态
                _state.update { it.copy(isLoading = true, isBuffering = true) }

                //使用生命周期管理器停止播放
                lifecycleManager.ended()
                    .onSuccess {
                        log.debug("停止播放成功")
                    }
                    .onFailure { e ->
                        log.error("停止播放失败", e)
                    }

                // 更新播放状态
                _state.update {
                    it.copy(
                        currentPlayUrl = result.url.v(),
                        playResult = result,
                        isLoading = false,
                        isBuffering = false
                    )
                }
                //转换播放器状态为ready
                lifecycleManager.ready()
                    .onSuccess {
                        log.debug("转换为ready成功")
                    }
                    .onFailure { e ->
                        log.error("转换为ready失败", e)
                    }

                // 启动播放
                lifecycleManager.start()
                    .onSuccess {
                        log.debug("启动播放成功")
                    }
                    .onFailure { e ->
                        log.error("启动播放失败", e)
                    }
                controller.loadAsync(result.url.v(), 10000)
            } catch (e: TimeoutCancellationException) {
                log.error("播放器初始化超时", e)
                SnackBar.postMsg("播放器初始化超时，请重试")
                _state.update { it.copy() }
            } catch (e: Exception) {
                log.error("播放器初始化失败", e)
                SnackBar.postMsg("播放器初始化失败: ${e.message}")
                _state.update { it.copy() }
            }
        }
    }


        /**
     * 播放指定视频的指定剧集。
     * 该方法会根据剧集的 URL 尝试获取播放结果，根据播放结果和播放器设置执行不同操作。
     * 若为下载链接则直接返回，若播放结果无效则提示用户并切换线路，
     * 若使用内置播放器则更新播放 URL 和当前剧集信息，最后更新剧集激活状态。
     *
     * @param detail 视频详情对象，包含视频的基本信息和站点信息。
     * @param ep 要播放的剧集对象，包含剧集的名称和 URL 等信息。
     */
    private fun playEp(detail: Vod, ep: Episode) {
        // 记录当前选中的剧集 URL，用于后续状态跟踪
        currentSelectedEpUrl.value = ep.url

        // 步骤1: 检查剧集 URL 是否为下载链接
        // 如果是下载链接（如 .mp4, .mkv 等文件），直接返回不播放
        if (Utils.isDownloadLink(ep.url)) return

        // 步骤2: 获取播放内容
        // 通过 SiteViewModel 获取实际的播放地址
        val result = SiteViewModel.playerContent(
            detail.site?.key ?: "",           // 站点标识
            detail.currentFlag.flag ?: "",    // 当前线路标识
            ep.url                            // 剧集原始URL
        )

        // 步骤3: 检查是否为特殊链接
        val isSpecialLink = isSpecialVideoLink
        log.debug("playEp - 特殊链接: $isSpecialLink")

        // 如果是特殊链接（如直播、特殊格式），不通过VLCJ播放器播放
        if (isSpecialLink) {
            log.debug("检测到特殊链接，跳过VLCJ播放器更新")
            // 更新状态为不加载、不缓冲，仅更新UI显示
            _state.update { it.copy(isLoading = false, isBuffering = false) }
            // 更新剧集激活状态，将当前剧集标记为激活
            updateEpisodeActivation(ep)
            // 更新历史记录
            updateHistoryWithNewEpisode(ep)
            return
        }

        // 步骤4: 验证播放结果
        // 检查播放结果是否为空或无效
        if (result == null || result.playResultIsEmpty()) {
            hideProgress()
            // 提示用户加载失败，并尝试切换到下一个线路
            SnackBar.postMsg("加载内容失败，尝试切换线路")
            nextFlag()
            return
        }

        // 步骤5: 启动播放器
        // 在协程中启动播放器状态转换
        scope.launch {
            lifecycleManager.start()
        }

        // 步骤6: 更新播放状态
        // 更新当前播放URL和缓冲状态
        _state.update { it.copy(currentUrl = result.url, isLoading = true, isBuffering = true) }

        // 步骤7: 更新历史记录
        // 将当前剧集URL保存到历史记录中
        controller.doWithHistory { it.copy(episodeUrl = ep.url) }

        // 步骤8: 判断播放器类型
        // 检查是否使用内置播放器
        val internalPlayer = SettingStore.getSettingItem(SettingType.PLAYER).getPlayerSetting(detail.site?.playerType)
            .first() == PlayerType.Innie.id

        // 步骤9: 根据播放器类型更新状态
        if (internalPlayer) {
            // 使用内置播放器：更新播放URL和当前剧集信息
            _state.update {
                it.copy(
                    currentPlayUrl = result.url.v(),  // 实际播放地址
                    currentEp = ep,                     // 当前剧集对象
                    isLoading = false,                  // 取消加载状态
                    isBuffering = false                 // 取消缓冲状态
                )
            }
        }

        // 步骤10: 更新剧集激活状态
        // 将当前剧集标记为激活状态，更新UI显示
        updateEpisodeActivation(ep)

        // 步骤11: 外部播放器提示
        // 如果使用外部播放器，提示用户上次观看的剧集
        if (!internalPlayer) {
            // 提示用户上次看到的剧集名称
            SnackBar.postMsg("上次看到" + ": ${ep.name}")
        }
    }



    /**
     * 启动视频播放流程。
     * 该方法会检查播放器控制器状态、视频详情信息以及历史记录，
     * 根据不同情况初始化历史记录、设置播放起始和结束时间，
     * 最后调用 `playEp` 方法开始播放视频。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startPlay() {

        scope.launch {
            lifecycleManager.ready()
        }

        // 检查视频详情信息是否为空，若为空则不进行播放操作
        if (!_state.value.detail.isEmpty()) {
            // 检查播放器控制器是否已释放，若已释放则记录错误日志并返回
            if (controller.isReleased()) {
                log.error("Controller已释放，无法播放")
                return
            }
            // 检查视频是否正在播放且不需要重新播放，若是则记录日志并返回
            if (controller.isPlaying() && !_state.value.shouldPlay) {
                log.info("视频播放中 返回")
                return
            }
            // 将 shouldPlay 标志置为 false，表示不需要重新播放
            _state.value.shouldPlay = false
            // 记录开始播放的日志
            log.info("startPlay - 准备开始播放视频，正在获取信息...")
            // 获取当前视频详情信息
            val detail = _state.value.detail
            // 用于存储找到的要播放的剧集对象
            var findEp: Episode? = null
            // 再次检查视频详情信息是否为空，若为空则返回
            if (detail.isEmpty()) return
            // 异步从数据库中查找视频的历史记录
            val historyDeferred =
                scope.async { Db.History.findHistory(Utils.getHistoryKey(detail.site?.key!!, detail.vodId)) }
            // 阻塞当前线程，等待历史记录查找完成
            runBlocking {
                historyDeferred.await()
            }
            // 获取异步操作的结果
            var history = historyDeferred.getCompleted()
            // 若历史记录为空，创建新的历史记录并设置到控制器中
            if (history == null) {
                scope.launch {
                    controller.setControllerHistory(
                        Db.History.create(
                            detail,
                            detail.currentFlag.flag!!,
                            detail.vodName ?: ""
                        )
                    )
                }
            } else {
                // 若当前剧集名称与历史记录中的剧集名称不同且历史记录中有播放位置，重置播放位置
                if (_state.value.currentEp != null && !_state.value.currentEp?.name.equals(history.vodRemarks) && history.position != null) {
                    history = history.copy(position = 0L)
                }
                // 设置视频播放的起始和结束时间
                controller.setStartEnd(history.opening ?: -1, history.ending ?: -1)
                // 根据历史记录查找要播放的剧集
                findEp = detail.findAndSetEpByName(history)
                // 更新状态流中的视频详情信息
                _state.update { it.copy(detail = detail, isLoading = false, isBuffering = false) }
            }
            // 再次异步从数据库中查找视频的历史记录
            val findHistoryDeferred = scope.async {
                Db.History.findHistory(
                    Utils.getHistoryKey(detail.site?.key!!, detail.vodId)
                )
            }
            // 阻塞当前线程，等待历史记录查找完成
            runBlocking {
                findHistoryDeferred.await()
            }
            // 获取异步操作的结果
            val findHistory = findHistoryDeferred.getCompleted()
            // 若找到历史记录，将其设置到控制器中
            if (findHistory != null) {
                controller.setControllerHistory(findHistory)
            }
            // 处理视频的子剧集列表
            detail.subEpisode.apply {
                // 若未找到要播放的剧集，则选择第一个剧集
                val ep = findEp ?: first()
//                log.debug("detail is $detail, Ep is $ep")
                hideProgress()
                // 调用 playEp 方法开始播放指定剧集
                log.debug("startPlay - 开始播放视频:{}", ep.name)


                playEp(detail, ep)
            }
        }
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

                // 调用 chooseEp 刷新页面
                chooseEp(firstEp) { _ -> }

                return decryptUrl(firstEp.url)
            }

            val currentIndex = currentDetail.subEpisode.indexOf(currentEp)
            val nextIndex = currentIndex + 1

            // 处理分组切换
            if (nextIndex >= currentDetail.subEpisode.size) {
                nextTabIndex++

                // 计算总分组数 (假设每组 Constants.EpSize 个剧集)
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

                // 调用 chooseEp 刷新页面
                chooseEp(newFirstEp) { _ -> }

                return decryptUrl(newFirstEp.url)
            }

            // 正常切换到下一集
            val nextEp = currentDetail.subEpisode[nextIndex]
            log.debug("切换下一集更新ui激活剧集{}", nextEp.name)
            updateEpisodeActivation(nextEp)
            //在数据库中更新数据
            updateHistoryWithNewEpisode(nextEp)

            // 调用 chooseEp 刷新页面
            chooseEp(nextEp) { _ -> }
            return decryptUrl(nextEp.url)
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
        // 更新状态流中的状态
        _state.update { state ->
            // 根据 newSubEpisodes 是否为 null 来决定使用哪个剧集列表更新激活状态
            val updatedSubEpisodes = newSubEpisodes?.map { ep ->
                // 若 newSubEpisodes 不为 null，遍历该列表，将与 activeEp 相同的剧集标记为激活状态
                ep.copy(activated = ep == activeEp)
            }?.toMutableList() ?: state.detail.subEpisode.map { ep ->
                // 若 newSubEpisodes 为 null，遍历当前视频详情中的子剧集列表，将与 activeEp 相同的剧集标记为激活状态
                ep.copy(activated = ep == activeEp)
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
     *
     * @param ep 新选中的剧集对象，包含剧集的名称和 URL 等信息。
     */
    private fun updateHistoryWithNewEpisode(ep: Episode) {
        // 获取当前视频详情信息
        val currentDetail = _state.value.detail
        // 记录开始更新历史记录的日志，包含当前选中的剧集名称
        log.debug("开始更新历史记录，当前选中剧集: {}", ep.name)
        // 在协程作用域中启动一个协程来处理历史记录更新操作
        scope.launch {
            // 从控制器中获取已有的历史记录
            val existingHistory = controller.getControllerHistory()
            // 若存在已有的历史记录，记录准备更新的日志
            if (existingHistory != null) {
                log.debug("检测到已有历史记录，准备更新...")
            } else {
                // 若不存在历史记录，记录准备创建新历史记录的日志
                log.debug("未检测到历史记录，准备创建新的历史记录...")
            }
            // 根据是否存在已有历史记录，决定是更新还是创建新的历史记录
            val history = existingHistory?.copy(
                // 更新剧集 URL
                episodeUrl = ep.url,
                // 更新剧集备注信息
                vodRemarks = ep.name,
                // 更新视频标识
                vodFlag = currentDetail.currentFlag.flag,
                // 重置播放位置为 0
                position = 0L
            ) ?: run {
                // 构建新历史记录的唯一 key
                val key =
                    "${currentDetail.site?.key}${Db.SYMBOL}${currentDetail.vodId}${Db.SYMBOL}${ApiConfig.api.cfg?.id}"
                // 记录新历史记录的 key
                log.debug("新历史记录 key: {}", key)
                // 创建新的历史记录对象
                History(
                    key = key,
                    // 设置视频封面图，若为空则使用空字符串
                    vodPic = currentDetail.vodPic ?: "",
                    // 设置视频名称
                    vodName = currentDetail.vodName!!,
                    // 设置视频标识
                    vodFlag = currentDetail.currentFlag.flag,
                    // 设置剧集备注信息
                    vodRemarks = ep.name,
                    // 设置剧集 URL
                    episodeUrl = ep.url,
                    // 设置分类 ID
                    cid = ApiConfig.api.cfg?.id!!,
                    // 设置历史记录创建时间为当前系统时间
                    createTime = System.currentTimeMillis(),
                    // 初始化播放位置为 0
                    position = 0L
                )
            }
            // 记录即将设置更新后的历史记录的日志
            log.debug("即将设置更新后的历史记录: {}", history)
            // 将更新或创建后的历史记录设置到控制器中
            controller.setControllerHistory(history)
            // 记录历史记录更新完成的日志
            log.debug("历史记录更新完成")
        }
    }


    /**
     * 根据传入的 URL 进行解密处理，获取解密后的 URL 字符串。
     * 该方法会调用 SiteViewModel 的 playerContent 方法，结合当前视频详情的站点 key 和当前选中的视频标识，
     * 获取播放内容信息，最终返回解密后的 URL 字符串。若过程中出现空值，则返回 null。
     *
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
        // 记录开始播放下一集的日志
        log.info("下一集")
        // 获取当前视频详情信息
        val detail = _state.value.detail
        // 初始化下一集索引为 0
        var nextIndex = 0
        // 初始化当前剧集索引为 0
        var currentIndex = 0
        // 查找当前激活的剧集
        val currentEp = detail.subEpisode.find { it.activated }
        // 更新控制器的历史记录，将播放位置重置为 0
        controller.doWithHistory { it.copy(position = 0) }
        // 若找到了当前激活的剧集
        if (currentEp != null) {
            // 获取当前激活剧集在子剧集列表中的索引
            currentIndex = detail.subEpisode.indexOf(currentEp)
            // 计算下一集的索引
            nextIndex = currentIndex + 1
        }
        // 若当前剧集索引达到或超过每个分组的剧集数量上限
        if (currentIndex >= Constants.EpSize - 1) {
            // 记录当前分组播放完毕，准备切换到下一个分组的日志
            log.info("当前分组播放完毕 下一个分组")
            // 将下一集索引重置为 0
            nextIndex = 0
            // 更新状态流中的视频详情信息，切换到下一个分组的子剧集列表
            _state.update {
                it.copy(
                    detail = detail.copy(subEpisode = detail.currentFlag.episodes.getPage(++detail.currentTabIndex)),
                    isLoading = false,
                    isBuffering = false
                )
            }
        }
        // 获取当前视频标识下的总剧集数量
        val size = detail.currentFlag.episodes.size
        // 若总剧集数量小于等于下一集的索引，说明没有更多剧集了
        if (size <= nextIndex) {
            // 提示用户没有更多剧集了
            SnackBar.postMsg("没有更多了")
            // 结束当前方法
            return
        }
        // 获取下一集的剧集对象，并调用 playEp 方法播放该剧集
        detail.subEpisode.get(nextIndex).let {
            playEp(detail, it)
        }
    }


    /**
     * 尝试切换到下一个视频播放线路。
     * 该方法会查找下一个可用的播放线路，若存在则切换到该线路并播放相应剧集；
     * 若下一个线路为空，提示用户没有更多线路；
     * 若下一个线路有效但为空，清空视频 ID 并执行快速搜索。
     */
    fun nextFlag() {
        hideProgress()
        // 记录开始尝试切换到下一个播放线路的日志
        log.info("nextFlag")
        // 复制当前视频详情信息，避免直接修改原始状态
        var detail = _state.value.detail.copy()
        // 调用 nextFlag 方法获取下一个可用的播放线路
        val nextFlag = _state.value.detail.nextFlag()

        // 若下一个播放线路为空
        if (nextFlag == null) {
            SnackBar.postMsg("没有更多线路")
            log.info("没有更多线路")
            // 添加以下状态更新
            _state.update {
                it.copy(
                    detail = it.detail.copy(),
                    isLoading = false,
                    isBuffering = false
                )
            }
            hideProgress()  // 再次确保隐藏进度条
            return
        }

        // 将下一个播放线路设置为当前播放线路
        detail.currentFlag = nextFlag
        // 若当前播放线路为空
        if (detail.currentFlag.isEmpty()) {
            // 清空视频 ID，以便快速搜索时重新加载详情
            detail.vodId = ""
            // 执行快速搜索操作
            quickSearch()
            // 结束当前方法
            return
        }
        // 复制视频详情信息，更新子剧集列表为当前播放线路对应页的剧集
        detail = detail.copy(subEpisode = detail.currentFlag.episodes.getPage(_state.value.detail.currentTabIndex))
        // 提示用户已切换至新的播放线路
        SnackBar.postMsg("切换至线路[${detail.currentFlag.flag}]")
        // 更新控制器的历史记录，记录当前播放线路标识
        controller.doWithHistory { it.copy(vodFlag = detail.currentFlag.flag) }
        // 将全局应用状态中选中的视频信息更新为当前视频详情信息
        GlobalAppState.chooseVod.value = _state.value.detail
        // 更新状态流中的视频详情信息
        _state.update { it.copy(detail = detail, isLoading = false, isBuffering = false) }
        // 根据控制器的历史记录查找对应的剧集
        val findEp = detail.findAndSetEpByName(controller.history.value!!)
        // 调用 playEp 方法播放找到的剧集，若未找到则播放子剧集列表中的第一个剧集
        playEp(detail, findEp ?: detail.subEpisode.first())
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
        // 在协程作用域中启动一个协程来处理历史记录同步操作
        scope.launch {
            // 根据当前视频的站点 key 和视频 ID 从数据库中查找历史记录
            var history = Db.History.findHistory(Utils.getHistoryKey(detail.site?.key!!, detail.vodId))
            // 若历史记录不存在
            if (history == null) {
                // 使用当前视频详情信息创建新的历史记录
                Db.History.create(detail, detail.currentFlag.flag!!, detail.vodName!!)
            } else {
                // 若当前选中剧集名称与历史记录中的剧集名称不同，且历史记录中有播放位置
                if (!_state.value.currentEp?.name.equals(history.vodRemarks) && history.position != null) {
                    // 重置播放位置为 0
                    history = history.copy(position = 0L)
                }
                // 将更新后的历史记录设置到控制器中
                controller.setControllerHistory(history)
                // 设置视频播放的起始和结束时间，若未设置则使用默认值 -1
                controller.setStartEnd(history.opening ?: -1, history.ending ?: -1)
                // 根据历史记录查找对应的剧集
                val findEp = detail.findAndSetEpByName(history)
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
            log.debug("切换线路，结束播放")
            // 先结束当前播放，避免线路切换时的音视频冲突
            lifecycleManager.ended()

            try {
                // 步骤1: 更新所有线路的激活状态
                // 遍历视频的所有可用播放线路
                for (vodFlag in detail.vodFlags) {
                    // 匹配用户选择的线路
                    if (it.show == vodFlag.show) {
                        // 激活选中的线路
                        it.activated = true
                        // 设置为当前播放线路
                        detail.currentFlag = it
                    } else {
                        // 取消其他线路的激活状态
                        vodFlag.activated = false
                    }
                }

                // 步骤2: 创建更新后的视频详情对象
                // 复制视频详情，更新当前线路和对应剧集列表
                val dt = detail.copy(
                    currentFlag = it,
                    // 根据当前标签页索引获取对应页码的剧集
                    subEpisode = it.episodes.getPage(detail.currentTabIndex).toMutableList()
                )

                // 步骤3: 更新观看历史记录
                // 在历史记录中保存当前选择的线路标识
                controller.doWithHistory { it.copy(vodFlag = detail.currentFlag.flag) }

                // 步骤4: 重置播放器状态
                // 将播放器状态重置为准备就绪
                lifecycleManager.ready()
                    .onSuccess {
                        log.debug("转换为ready成功")
                    }
                    .onFailure { e ->
                        log.error("转换为ready失败", e)
                    }

                // 步骤5: 根据历史记录自动播放
                val history = controller.history.value
                if (history != null) {
                    // 在历史记录中查找上次观看的剧集名称
                    val findEp = detail.findAndSetEpByName(controller.history.value!!)
                    if (findEp != null) {
                        // 找到对应剧集后自动播放
                        playEp(dt, findEp)
                    }
                }

                // 触发UI状态更新（空更新，可能用于触发重组）
                _state.update { it.copy() }


                // 步骤6: 更新最终状态
                // 更新状态流：设置新详情、标记需要播放、取消缓冲状态
                _state.update { model ->
                    model.copy(
                        detail = dt,
                        shouldPlay = true,  // 触发播放器开始播放
                        isBuffering = false, // 取消加载状态
                    )
                }
            } catch (e: Exception) {
                // 异常处理：记录错误并提示用户
                log.error("切换线路失败", e)
                SnackBar.postMsg("切换线路失败: ${e.message}")
                _state.update { it.copy() }
            }
        }
    }


    /**
     * 选择播放级别并更新状态流中的播放 URL 信息。
     * 该方法会根据传入的 URL 对象和播放 URL 字符串，
     * 更新状态流中的当前 URL 和当前播放 URL 信息。
     * 若传入的播放 URL 字符串为 null，则使用空字符串作为当前播放 URL。
     *
     * @param i 可选的 URL 对象，用于更新状态流中的当前 URL。
     * @param v 可选的播放 URL 字符串，用于更新状态流中的当前播放 URL。若为 null，则使用空字符串。
     */
    fun chooseLevel(i: Url?, v: String?) {
        // 使用 _state.update 更新状态流的值
        _state.update {
            // 复制当前状态，更新当前 URL 和当前播放 URL 信息
            it.copy(
                currentPlayUrl = v ?: "",
                currentUrl = i,
            )
        }
    }


    /**
     * 隐藏剧集选择对话框。
     * 该方法会更新状态流中的 `showEpChooserDialog` 字段为 `false`，
     * 以此来控制界面上剧集选择对话框的显示状态，使其隐藏。
     */
// 在showEpChooser方法中添加日志
    fun showEpChooser() {
//        log.debug("Toggling ep chooser dialog")
        _state.update {
//            log.debug("Current state: ${it.showEpChooserDialog}")
            it.copy(showEpChooserDialog = !it.showEpChooserDialog)
        }
    }


    /**
     * 批量选择剧集，根据传入的索引切换到对应的剧集分组。
     * 该方法会根据传入的索引计算出新的标签索引，
     * 并获取该标签索引对应的剧集列表，最后更新状态流中的视频详情信息。
     *
     * @param i 用于计算新标签索引的整数，通过该值除以 `Constants.EpSize` 得到新的标签索引。
     */
    fun chooseEpBatch(i: Int) {
        // 获取当前状态中的视频详情信息
        val detail = state.value.detail
        // 计算新的标签索引，通过传入的索引除以每个分组的剧集数量得到
        detail.currentTabIndex = i / Constants.EpSize
        // 复制视频详情信息，并更新子剧集列表为新标签索引对应的剧集列表
        val dt = detail.copy(
            subEpisode = detail.currentFlag.episodes.getPage(
                detail.currentTabIndex
            ).toMutableList()
        )
        // 更新状态流中的视频详情信息
        _state.update { it.copy(detail = dt, isLoading = false, isBuffering = false) }
    }


    val videoLoading = mutableStateOf(false)


    /**
     * 选择指定剧集进行播放操作，根据剧集链接类型和播放器设置执行不同播放逻辑。
     * 在操作开始时标记视频加载中，操作完成后标记加载结束。
     *
     * @param it 要选择播放的剧集对象，包含剧集的名称、URL 等信息。
     * @param openUri 一个函数，用于处理打开特定 URI 的操作，传入参数为 URI 字符串。
     */
    fun chooseEp(it: Episode, openUri: (String) -> Unit) {
        // 标记视频正在加载
        videoLoading.value = true

        currentSelectedEpUrl.value = it.url
        val detail = _state.value.detail

        // 在协程作用域中启动一个协程处理剧集选择逻辑
        scope.launch {
            // 检查当前选中剧集的 URL 是否为下载链接
            val isDownloadLink = Utils.isDownloadLink(it.url)
            // 遍历当前播放线路下的所有剧集
            for (i in detail.currentFlag.episodes) {
                // 标记当前剧集是否为选中的剧集
                i.activated = (i.name == it.name)
                if (i.activated) {
                    // 更新状态流中的状态
                    _state.update { model ->
                        if (!isDownloadLink) {
                            // 若当前选中的剧集与状态中的当前剧集不同，重置播放位置为 0
                            if (model.currentEp?.name != it.name) {
                                controller.doWithHistory { it.copy(position = 0L) }
                            }
                            // 更新控制器的历史记录，记录当前剧集的 URL 和备注信息
                            controller.doWithHistory {
                                it.copy(
                                    episodeUrl = i.url, vodRemarks = i.name
                                )
                            }
                        }
                        // 更新状态流中的当前剧集信息
                        model.copy(currentEp = i, isLoading = false, isBuffering = false)
                    }
                }
            }
            // 若当前选中剧集的 URL 是下载链接
            if (isDownloadLink) {
                // 调用 openUri 函数处理下载链接
                openUri(it.url)
                // 结束当前协程
                return@launch
            } else {
                // 复制视频详情信息，更新子剧集列表为当前标签页对应的剧集列表
                val dt = detail.copy(
                    subEpisode = detail.currentFlag.episodes.getPage(
                        detail.currentTabIndex
                    ).toMutableList().toList().toMutableList(),
                )
                _state.update { it.copy(detail = dt, isLoading = false, isBuffering = false) }

                // 获取当前剧集的播放内容信息
                val result = SiteViewModel.playerContent(
                    detail.site?.key ?: "", detail.currentFlag.flag ?: "", it.url
                )
                _state.update { it.copy(currentUrl = result?.url, isLoading = false, isBuffering = false) }

                // 获取播放器设置
                val playerType =
                    SettingStore.getSettingItem(SettingType.PLAYER.id).getPlayerSetting(detail.site?.playerType)

                // 根据播放器类型执行不同的播放操作
                when (playerType.first()) {
                    PlayerType.Innie.id -> play(result)
                    PlayerType.Outie.id -> Play.start(result?.url?.v() ?: "", state.value.currentEp?.name)
                    PlayerType.Web.id -> openUri(KtorD.getWebPlayerPath(result?.url?.v() ?: ""))
                }
            }
        }.invokeOnCompletion {
            // 协程完成后，标记视频加载结束
            videoLoading.value = false
        }
    }


    /**
     * 设置当前播放的 URL。
     * 该方法会更新状态流中的 `currentPlayUrl` 字段为传入的 URL 字符串。
     *
     * @param string 要设置的播放 URL 字符串。
     */
    fun setPlayUrl(string: String) {
        // 使用 _state.update 更新状态流的值
        _state.update {
            // 复制当前状态，将 currentPlayUrl 字段更新为传入的 string
            it.copy(currentPlayUrl = string, isLoading = false, isBuffering = false)
        }
    }
}