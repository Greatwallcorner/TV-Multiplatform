package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.corner.bean.SettingStore
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.getPage
import com.corner.catvod.enum.bean.Vod.Companion.isEmpty
import com.corner.catvodcore.bean.*
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.database.Db
import com.corner.database.History
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.getPlayerSetting
import com.corner.ui.player.vlcj.VlcjFrameController
import com.corner.ui.scene.SnackBar
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import com.corner.util.Constants
import com.corner.util.cancelAll
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

class DefaultDetailComponent(componentContext: ComponentContext) : DetailComponent,
    ComponentContext by componentContext {
    private val _model = MutableValue(DetailComponent.Model())

    private var supervisor = SupervisorJob()
    private val searchScope = CoroutineScope(Dispatchers.Default + supervisor)

    private val log = LoggerFactory.getLogger("Detail")

    private val lock = Any()

    @Volatile
    private var launched = false

    private var currentSiteKey = MutableValue("")

    private val jobList = mutableListOf<Job>()

    private var fromSearchLoadJob: Job = Job()

    override val model: MutableValue<DetailComponent.Model> = _model

    override val controller: VlcjFrameController = VlcjFrameController(this)

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onCreate() {
                SiteViewModel.viewModelScope.launch {
                    controller.init()
                }
            }

            override fun onStop() {
                log.info("Detail onStop")
                updateHistory(controller.history.value)
                super.onStop()
            }

            override fun onDestroy() {
                log.info("Detail onDestroy")
                super.onDestroy()
                SiteViewModel.viewModelScope.launch {
                    searchScope.cancel("on stop")
                    fromSearchLoadJob.cancel("on stop")
                    hideProgress()
                    clear()
                    controller.dispose()
                }
            }
        })

    }

    override fun updateHistory(it: History?) {
        if (it != null && StringUtils.isNotBlank(model.value.detail?.site?.key)) {
            Db.History.updateSome(
                it.vodFlag ?: "",
                it.vodRemarks ?: "",
                it.episodeUrl ?: "",
                it.position ?: -1,
                it.speed?.toFloat() ?: 1f,
                it.opening ?: -1L,
                it.ending ?: -1L,
                Utils.getHistoryKey(model.value.detail?.site?.key!!, model.value.detail?.vodId!!)
            )
        }
    }

    override fun load() {
        val chooseVod = getChooseVod()
        model.update { it.copy(detail = chooseVod) }
        currentSiteKey.value = chooseVod.site?.key ?: ""
        SiteViewModel.viewModelScope.launch {
            if (GlobalModel.detailFromSearch) {
                val list = SiteViewModel.getSearchResultActive().getList()
                model.update { it.copy(quickSearchResult = CopyOnWriteArrayList(list), detail = chooseVod) }
                fromSearchLoadJob = SiteViewModel.viewModelScope.launch {
                    if (model.value.quickSearchResult.isNotEmpty()) model.value.detail?.let { loadDetail(it) }
                }
            } else {
                model.update { it.copy(isLoading = true) }
                val dt = SiteViewModel.detailContent(chooseVod.site?.key ?: "", chooseVod.vodId)
                model.update { it.copy(isLoading = false) }
                if (dt == null || dt.detailIsEmpty()) {
                    quickSearch()
                } else {
                    var detail = dt.list[0]
                    detail =
                        detail.copy(subEpisode = detail.currentFlag?.episodes?.getPage(detail.currentTabIndex))
                    if (StringUtils.isNotBlank(getChooseVod().vodRemarks)) {
                        for (it: Episode in detail.subEpisode ?: listOf()) {
                            if (it.name == getChooseVod().vodRemarks) {
                                it.activated = true
                                break
                            }
                        }
                    }
                    detail.site = getChooseVod().site
                    model.update { it.copy(detail = detail) }
                    startPlay()
                }
            }
        }
    }

    override fun quickSearch() {
        model.update { it.copy(isLoading = true) }
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
                    model.update {
                        val list = CopyOnWriteArrayList<Vod>()
                        list.addAllAbsent(SiteViewModel.quickSearch.value[0].getList())
                        it.copy(
                            quickSearchResult = list
                        )
                    }
                    if (it == null) log.debug("一个job执行完毕 result size:{}", model.value.quickSearchResult.size)

                    synchronized(lock) {
                        if (model.value.quickSearchResult.isNotEmpty() && (model.value.detail == null || model.value.detail!!.isEmpty()) && !launched) {
                            log.info("开始加载 详情")
                            launched = true
                            loadDetail(model.value.quickSearchResult[0])
                        }
                    }
                }
                jobList.add(job)
            }
            jobList.forEach {
                it.join()
            }
            if (model.value.quickSearchResult.isEmpty()) {
                model.update { it.copy(detail = GlobalModel.chooseVod.value) }
                SnackBar.postMsg("暂无线路数据")
            }
        }.invokeOnCompletion {
            model.update { it.copy(isLoading = false) }
        }
    }

    override fun loadDetail(vod: Vod) {
        log.info("加载详情 <${vod.vodName}> <${vod.vodId}> site:<${vod.site}>")
        showProgress()
        try {
            val dt = SiteViewModel.detailContent(vod.site?.key!!, vod.vodId)
            if (dt == null || dt.detailIsEmpty()) {
                log.info("请求详情为空 加载下一个")
                nextSite(vod)
            } else {
                var first = dt.list[0]
                log.info("加载详情完成 $first")
                first = first.copy(
                    subEpisode = first.vodFlags[0]?.episodes?.getPage(first.currentTabIndex)?.toMutableList()
                )
                first.site = vod.site
                setDetail(first)
                supervisor.cancelChildren()
                jobList.cancelAll().clear()
            }
        } finally {
            launched = false
            hideProgress()
        }
    }

    override fun nextSite(lastVod: Vod?) {
        if (model.value.quickSearchResult.isEmpty()) {
            log.warn("nextSite 快搜结果为空 返回")
            return
        }
        val list = model.value.quickSearchResult
        if (lastVod != null) {
            val remove = list.remove(lastVod)
            log.debug("remove last vod result:$remove")
        }
        model.update { it.copy(quickSearchResult = list) }
        if (model.value.quickSearchResult.isNotEmpty()) loadDetail(model.value.quickSearchResult[0])
    }

    override fun clear() {
        launched = false
        jobList.forEach { it.cancel("detail clear") }
        jobList.clear()
        model.update { it.copy(quickSearchResult = CopyOnWriteArrayList(), detail = null, showEpChooserDialog = false) }
        SiteViewModel.clearQuickSearch()
    }

    override fun getChooseVod(): Vod {
        return GlobalModel.chooseVod.value
    }

    override fun setDetail(vod: Vod) {
        if (!currentSiteKey.value.equals(vod.site?.key)) {
            SnackBar.postMsg("正在切换站源至 [${vod.site!!.name}]")
        }
        model.update { it.copy(detail = vod) }
        startPlay()
    }

    override fun play(result: Result?) {
        //获取到的播放结果为空 尝试下一个线路
        if (result == null || result.playResultIsEmpty()) {
            nextFlag()
            return
        }
        model.update { it.copy(currentPlayUrl = result.url.v(), playResult = result) }
    }

    override fun startPlay() {
        if (model.value.detail != null && model.value.detail?.isEmpty() == false) {
            if (controller.isPlaying() == true && !model.value.shouldPlay) {
                log.info("视频播放中 返回")
                return
            }
            model.value.shouldPlay = false
//            val internalPlayer: Boolean = SettingStore.getPlayerSetting()[0] as Boolean
//            if(!internalPlayer) return // 如果使用外部播放器直接返回
            log.info("start play")
            val detail = model.value.detail
            var findEp: Episode? = null
            if (detail == null || detail.isEmpty()) return
            var history = Db.History.findHistory(Utils.getHistoryKey(detail.site?.key!!, detail.vodId))
            if (history == null) Db.History.create(detail, detail.currentFlag?.flag!!, detail.vodName!!)
            else {
                if (model.value.currentEp != null && !model.value.currentEp?.name.equals(history.vodRemarks) && history.position != null) {
                    history = history.copy(position = 0L)
                }
                controller.setStartEnd(history.opening ?: -1, history.ending ?: -1)

                findEp = detail.findAndSetEpByName(history)
                model.update { it.copy(detail = detail) }
            }
            val findHistory = Db.History.findHistory(
                Utils.getHistoryKey(detail.site?.key!!, detail.vodId)
            )
            if (findHistory != null) {
                controller.setControllerHistory(findHistory)
            }
            detail.subEpisode?.apply {
                val ep = findEp ?: first()
                playEp(detail, ep)
            }
        }
    }

    override fun playEp(detail: Vod, ep: Episode) {
        val result = SiteViewModel.playerContent(
            detail.site?.key ?: "",
            detail.currentFlag?.flag ?: "",
            ep.url
        )
        model.update { it.copy(currentUrl = result?.url) }
        if (result == null || result.playResultIsEmpty()) {
            nextFlag()
            return
        }
        controller.doWithHistory { it.copy(episodeUrl = ep.url) }
        val internalPlayer = SettingStore.getPlayerSetting()[0] as Boolean
        if (internalPlayer) {
            model.update { it.copy(currentPlayUrl = result.url.v(), currentEp = ep) }
        }
        detail.subEpisode?.parallelStream()?.forEach {
            it.activated = it == ep
        }
        if (!internalPlayer) {
            SnackBar.postMsg("上次播放" + ": ${ep.name}")
        }
    }

    override fun nextEP() {
        log.info("下一集")
        var detail = model.value.detail
        var nextIndex = 0
        var currentIndex = 0
        val currentEp = detail?.subEpisode?.find { it.activated }
        controller.doWithHistory { it.copy(position = 0) }
        if (currentEp != null) {
            currentIndex = detail?.subEpisode?.indexOf(currentEp)!!
            nextIndex = currentIndex + 1
        }
        if (currentIndex >= Constants.EpSize - 1) {
            log.info("当前分组播放完毕 下一个分组")
            detail =
                detail?.copy(subEpisode = detail.currentFlag?.episodes?.getPage(++detail.currentTabIndex))
            nextIndex = 0
            model.update { it.copy(detail = detail) }
        }
        val size = detail?.currentFlag?.episodes?.size ?: 0
        if(size <= nextIndex) {
//            SnackBar.postMsg("没有更多了")
            return
        }
        detail?.subEpisode?.get(nextIndex)?.let {
            playEp(detail, it)
        }
    }

    override fun nextFlag() {
        log.info("nextFlag")
        val detail = model.value.detail?.copy()
        detail?.currentFlag = model.value.detail?.nextFlag()
        if (detail?.currentFlag == null) {
            detail?.vodId = "" // 清空id 快搜就会重新加载详情
            quickSearch()
            return
        }
        detail.subEpisode = model.value.detail?.currentFlag?.episodes?.getPage(model.value.detail?.currentTabIndex ?: 0)
            ?.toMutableList()
        SnackBar.postMsg("切换至线路[${detail.currentFlag?.flag}]")
        controller.doWithHistory { it.copy(vodFlag = detail.currentFlag?.flag) }
        GlobalModel.chooseVod.value = model.value.detail!!
        model.update { it.copy(detail = detail) }
        val findEp = detail.findAndSetEpByName(controller.history.value!!)
//        if(findEp == null)
        playEp(detail, findEp ?: detail.subEpisode!!.first())
    }

    override fun syncHistory() {
        val detail = model.value.detail ?: return
        var history = Db.History.findHistory(Utils.getHistoryKey(detail.site?.key!!, detail.vodId))
        if (history == null) Db.History.create(detail, detail.currentFlag?.flag!!, detail.vodName!!)
        else {
            if (!model.value.currentEp?.name.equals(history.vodRemarks) && history.position != null) {
                history = history.copy(position = 0L)
            }
            controller.setControllerHistory(history)
            controller.setStartEnd(history.opening ?: -1, history.ending ?: -1)

            val findEp = detail.findAndSetEpByName(history)
            model.update { it.copy(detail = detail, currentEp = findEp, currentPlayUrl = findEp?.url ?: "") }
        }
    }
}