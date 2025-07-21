package com.corner.ui.nav.vm

import SiteViewModel
import androidx.compose.runtime.mutableStateOf
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


    val controller: VlcjFrameController = VlcjFrameController(this).apply { VlcJInit.setController(this) }

    val currentSelectedEpUrl = mutableStateOf<String?>(null) // 新增状态记录

    fun updateHistory(it: History) {
        if (StringUtils.isNotBlank(state.value.detail.site?.key)) {
            scope.launch {
                Db.History.update(it.copy(createTime = Clock.System.now().toEpochMilliseconds()))
            }
        }
    }

    fun load() {
        controller.init()
        val chooseVod = getChooseVod()
        _state.update { it.copy(detail = chooseVod) }
        currentSiteKey.value = chooseVod.site?.key ?: ""
        SiteViewModel.viewModelScope.launch {
            if (GlobalAppState.detailFrom == DetailFromPage.SEARCH) {
                val list = SiteViewModel.getSearchResultActive().list
                _state.update { it.copy(quickSearchResult = CopyOnWriteArrayList(list), detail = chooseVod) }
                fromSearchLoadJob = SiteViewModel.viewModelScope.launch {
                    if (_state.value.quickSearchResult.isNotEmpty()) _state.value.detail.let { loadDetail(it) }
                }
            } else {
                _state.update { it.copy(isLoading = true) }
                val dt = SiteViewModel.detailContent(chooseVod.site?.key ?: "", chooseVod.vodId)
                _state.update { it.copy(isLoading = false) }
                // id为空不执行后续代码
                if (chooseVod.vodId.isBlank()) return@launch
                if (dt == null || dt.detailIsEmpty()) {
                    quickSearch()
                } else {
                    var detail = dt.list[0]
                    detail =
                        detail.copy(subEpisode = detail.currentFlag.episodes.getPage(detail.currentTabIndex))
                    if (StringUtils.isNotBlank(getChooseVod().vodRemarks)) {
                        for (it: Episode in detail.subEpisode) {
                            if (it.name == getChooseVod().vodRemarks) {
                                it.activated = true
                                break
                            }
                        }
                    }
                    detail.site = getChooseVod().site
                    _state.update { it.copy(detail = detail) }
                    startPlay()
                }
            }
        }
    }

    fun loadDetail(vod: Vod) {
        log.info("加载详情 <${vod.vodName}> <${vod.vodId}> site:<${vod.site}>")
        showProgress()
        try {
            val dt = SiteViewModel.detailContent(vod.site?.key!!, vod.vodId)
            if (dt == null || dt.detailIsEmpty()) {
                log.info("请求详情为空 加载下一个")
                nextSite(vod)
            } else {
                val first = dt.list[0]
                log.info("加载详情完成 $first")
                first.site = vod.site
                if (first.isEmpty()) {
                    nextSite(vod)
                } else {
                    setDetail(first)
                    supervisor.cancelChildren()
                    jobList.cancelAll().clear()
                }
            }
        } finally {
            launched = false
            hideProgress()
        }
    }

    fun quickSearch() {
        _state.update { it.copy(isLoading = true) }
        searchScope.launch {
            val quickSearchSites = ApiConfig.api.sites.filter { it.changeable == 1 }.shuffled()
            log.debug("开始执行快搜 sites:{}", quickSearchSites.map { it.name }.toString())
            val semaphore = Semaphore(2)
            quickSearchSites.forEach {
                val job = launch() {
                    semaphore.acquire()
                    withTimeout(2500L) {
                        SiteViewModel.searchContent(it, getChooseVod().vodName ?: "", true)
                        log.debug("{}完成搜索", it.name)
                    }
                    semaphore.release()
                }

                job.invokeOnCompletion {
                    if (it != null) {
                        log.error("quickSearch 协程执行异常 msg:{}", it.message)
                    }
                    _state.update {
                        val list = CopyOnWriteArrayList<Vod>()
                        list.addAllAbsent(SiteViewModel.quickSearch.value[0].list)
                        it.copy(
                            quickSearchResult = list
                        )
                    }
                    if (it == null) log.debug("一个job执行完毕 result size:{}", _state.value.quickSearchResult.size)

                    synchronized(lock) {
                        if (_state.value.quickSearchResult.isNotEmpty() && (_state.value.detail.isEmpty()) && !launched) {
                            log.info("开始加载 详情")
                            launched = true
                            loadDetail(_state.value.quickSearchResult[0])
                        }
                    }
                }
                jobList.add(job)
            }
            jobList.forEach {
                it.join()
            }
            if (_state.value.quickSearchResult.isEmpty()) {
                _state.update { it.copy(detail = GlobalAppState.chooseVod.value) }
                SnackBar.postMsg("暂无线路数据")
            }
        }.invokeOnCompletion {
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun nextSite(lastVod: Vod?) {
        if (_state.value.quickSearchResult.isEmpty()) {
            log.warn("nextSite 快搜结果为空 返回")
            return
        }
        val list = _state.value.quickSearchResult
        if (lastVod != null) {
            val remove = list.remove(lastVod)
            log.debug("remove last vod result:$remove")
        }
        _state.update { it.copy(quickSearchResult = list) }
        if (_state.value.quickSearchResult.isNotEmpty()) loadDetail(_state.value.quickSearchResult[0])
    }

    /**
     * 修改clear,传入releaseController = false时不释放播放器资源
     * */

    fun clear(releaseController: Boolean = true) {
        log.debug("detail clear")
        if (releaseController) {
            controller.release()
        }
        launched = false
        jobList.forEach { it.cancel("detail clear") }
        jobList.clear()
        _state.update {
            it.copy(
                quickSearchResult = CopyOnWriteArrayList(),
                detail = Vod(),
                showEpChooserDialog = false
            )
        }
        SiteViewModel.clearQuickSearch()
    }

    private fun getChooseVod(): Vod {
        return GlobalAppState.chooseVod.value
    }

    private fun setDetail(vod: Vod) {
        if (currentSiteKey.value != vod.site?.key) {
            SnackBar.postMsg("正在切换站源至 [${vod.site!!.name}]")
        }
        _state.update {
            it.copy(
                detail = vod.copy(
                    subEpisode = vod.vodFlags.first().episodes.getPage(vod.currentTabIndex).toMutableList()
                )
            )
        }
        startPlay()
    }

    fun play(result: Result?) {
        //获取到的播放结果为空 尝试下一个线路
        if (result == null || result.playResultIsEmpty()) {
            nextFlag()
            return
        }
        _state.update { it.copy(currentPlayUrl = result.url.v(), playResult = result) }
    }

    private fun playEp(detail: Vod, ep: Episode) {
        currentSelectedEpUrl.value = ep.url
        if (Utils.isDownloadLink(ep.url)) return
        val result = SiteViewModel.playerContent(
            detail.site?.key ?: "",
            detail.currentFlag.flag ?: "",
            ep.url
        )
        _state.update { it.copy(currentUrl = result?.url) }
        if (result == null || result.playResultIsEmpty()) {
            nextFlag()
            return
        }
        controller.doWithHistory { it.copy(episodeUrl = ep.url) }
        val internalPlayer = SettingStore.getSettingItem(SettingType.PLAYER).getPlayerSetting(detail.site?.playerType).first() == PlayerType.Innie.id
        if (internalPlayer) {
            _state.update { it.copy(currentPlayUrl = result.url.v(), currentEp = ep) }
        }
        detail.subEpisode.parallelStream().forEach {
            it.activated = it == ep
        }
        if (!internalPlayer) {
            SnackBar.postMsg("上次看到" + ": ${ep.name}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun startPlay() {
        if (!_state.value.detail.isEmpty()) {
            if (controller.isReleased) {
                log.error("Controller已释放，无法播放")
                return
            }
            if (controller.isPlaying() && !_state.value.shouldPlay) {
                log.info("视频播放中 返回")
                return
            }
            _state.value.shouldPlay = false
            log.info("start play")
            val detail = _state.value.detail
            var findEp: Episode? = null
            if (detail.isEmpty()) return
            val historyDeferred =
                scope.async { Db.History.findHistory(Utils.getHistoryKey(detail.site?.key!!, detail.vodId)) }
            runBlocking {
                historyDeferred.await()
            }
            var history = historyDeferred.getCompleted()
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
                if (_state.value.currentEp != null && !_state.value.currentEp?.name.equals(history.vodRemarks) && history.position != null) {
                    history = history.copy(position = 0L)
                }
                controller.setStartEnd(history.opening ?: -1, history.ending ?: -1)

                findEp = detail.findAndSetEpByName(history)
                _state.update { it.copy(detail = detail) }
            }
            val findHistoryDeferred = scope.async {
                Db.History.findHistory(
                    Utils.getHistoryKey(detail.site?.key!!, detail.vodId)
                )
            }
            runBlocking {
                findHistoryDeferred.await()
            }
            val findHistory = findHistoryDeferred.getCompleted()
            if (findHistory != null) {
                controller.setControllerHistory(findHistory)
            }
            detail.subEpisode.apply {
                val ep = findEp ?: first()
                playEp(detail, ep)
            }
        }
    }

    fun nextEP() {
        log.info("下一集")
        val detail = _state.value.detail
        var nextIndex = 0
        var currentIndex = 0
        val currentEp = detail.subEpisode.find { it.activated }
        controller.doWithHistory { it.copy(position = 0) }
        if (currentEp != null) {
            currentIndex = detail.subEpisode.indexOf(currentEp)
            nextIndex = currentIndex + 1
        }
        if (currentIndex >= Constants.EpSize - 1) {
            log.info("当前分组播放完毕 下一个分组")
            nextIndex = 0
            _state.update { it.copy(detail = detail.copy(subEpisode = detail.currentFlag.episodes.getPage(++detail.currentTabIndex))) }
        }
        val size = detail.currentFlag.episodes.size
        if (size <= nextIndex) {
//            SnackBar.postMsg("没有更多了")
            return
        }
        detail.subEpisode.get(nextIndex).let {
            playEp(detail, it)
        }
    }

    fun nextFlag() {
        log.info("nextFlag")
        var detail = _state.value.detail.copy()
        val nextFlag = _state.value.detail.nextFlag()
        if (nextFlag == null) {
            log.info("没有更多线路")
            return
        }
        detail.currentFlag = nextFlag
        if (detail.currentFlag.isEmpty()) {
            detail.vodId = "" // 清空id 快搜就会重新加载详情
            quickSearch()
            return
        }
        detail = detail.copy(subEpisode = detail.currentFlag.episodes.getPage(_state.value.detail.currentTabIndex))
        SnackBar.postMsg("切换至线路[${detail.currentFlag.flag}]")
        controller.doWithHistory { it.copy(vodFlag = detail.currentFlag.flag) }
        GlobalAppState.chooseVod.value = _state.value.detail
        _state.update { it.copy(detail = detail) }
        val findEp = detail.findAndSetEpByName(controller.history.value!!)
        playEp(detail, findEp ?: detail.subEpisode.first())
    }

    fun syncHistory() {
        val detail = _state.value.detail
        scope.launch {
            var history = Db.History.findHistory(Utils.getHistoryKey(detail.site?.key!!, detail.vodId))
            if (history == null) Db.History.create(detail, detail.currentFlag.flag!!, detail.vodName!!)
            else {
                if (!_state.value.currentEp?.name.equals(history.vodRemarks) && history.position != null) {
                    history = history.copy(position = 0L)
                }
                controller.setControllerHistory(history)
                controller.setStartEnd(history.opening ?: -1, history.ending ?: -1)

                val findEp = detail.findAndSetEpByName(history)
                withContext(Dispatchers.Default) {
                    _state.update { it.copy(detail = detail, currentEp = findEp, currentPlayUrl = findEp?.url ?: "") }
                }
            }
        }

    }

    fun clickShowEp() {
        _state.update { it.copy(showEpChooserDialog = !_state.value.showEpChooserDialog) }
    }

    fun chooseFlag(detail: Vod, it: Flag) {
        scope.launch {
            for (vodFlag in detail.vodFlags) {
                if (it.show == vodFlag.show) {
                    it.activated = true
                    detail.currentFlag = it
                } else {
                    vodFlag.activated = false
                }
            }
            val dt = detail.copy(
                currentFlag = it,
                subEpisode = it.episodes.getPage(detail.currentTabIndex).toMutableList()
            )
            controller.doWithHistory { it.copy(vodFlag = detail.currentFlag.flag) }
            val history = controller.history.value
            if (history != null) {
                val findEp = detail.findAndSetEpByName(controller.history.value!!)
                if (findEp != null) playEp(dt, findEp)
            }
            _state.update { model ->
                model.copy(
                    detail = dt,
                    shouldPlay = true,
                )
            }
        }
    }

    fun chooseLevel(i: Url?, v: String?) {
        _state.update {
            it.copy(
                currentUrl = i,
                currentPlayUrl = v ?: ""
            )
        }
    }

    fun showEpChooser() {
        _state.update { it.copy(showEpChooserDialog = false) }
    }

    fun chooseEpBatch(i: Int) {
        val detail = state.value.detail
        detail.currentTabIndex = i / Constants.EpSize
        val dt = detail.copy(
            subEpisode = detail.currentFlag.episodes.getPage(
                detail.currentTabIndex
            ).toMutableList()
        )
        _state.update { it.copy(detail = dt) }
    }

    val videoLoading = mutableStateOf(false)


    fun chooseEp(it: Episode, openUri: (String) -> Unit) {
        videoLoading.value = true
        currentSelectedEpUrl.value = it.url // 记录当前选中的URL
        val detail = _state.value.detail
        scope.launch {
            val isDownloadLink = Utils.isDownloadLink(it.url)
            for (i in detail.currentFlag.episodes) {
                i.activated = (i.name == it.name)
                if (i.activated) {
                    _state.update { model ->
                        if (!isDownloadLink) {
                            if (model.currentEp?.name != it.name) {
                                controller.doWithHistory { it.copy(position = 0L) }
                            }
                            controller.doWithHistory {
                                it.copy(
                                    episodeUrl = i.url, vodRemarks = i.name
                                )
                            }
                        }
                        model.copy(currentEp = i)
                    }
                }
            }
            if (isDownloadLink) {
                openUri(it.url)
                return@launch
            } else {
                val dt = detail.copy(
                    subEpisode = detail.currentFlag.episodes.getPage(
                        detail.currentTabIndex
                    ).toMutableList().toList().toMutableList(),
                )
                _state.update { it.copy(detail = dt) }
                val result = SiteViewModel.playerContent(
                    detail.site?.key ?: "", detail.currentFlag.flag ?: "", it.url
                )
                _state.update { it.copy(currentUrl = result?.url) }

                val playerType = SettingStore.getSettingItem(SettingType.PLAYER.id).getPlayerSetting(detail.site?.playerType)
                when (playerType.first()) {
                    PlayerType.Innie.id -> play(result)
                    PlayerType.Outie.id -> Play.start(result?.url?.v() ?: "", state.value.currentEp?.name)
                    PlayerType.Web.id -> openUri(KtorD.getWebPlayerPath(result?.url?.v() ?: ""))
                }
            }
        }.invokeOnCompletion {
            videoLoading.value = false
        }
    }

    fun setPlayUrl(string: String) {
        _state.update { it.copy(currentPlayUrl = string) }
    }
}