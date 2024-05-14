package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.getPage
import com.corner.catvodcore.bean.Episode
import com.corner.catvodcore.bean.detailIsEmpty
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.scene.SnackBar
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
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

    private var fromSearchLoadJob:Job = Job()

    override val model: MutableValue<DetailComponent.Model> = _model

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onStop() {
                log.info("Detail onStop")
                searchScope.cancel("on stop")
                fromSearchLoadJob.cancel("on stop")
                hideProgress()
                super.onStop()
            }
        })
    }


    override fun load() {
        val chooseVod = getChooseVod()
        currentSiteKey.value = chooseVod.site?.key ?: ""
        SiteViewModel.viewModelScope.launch {
            if(GlobalModel.detailFromSearch){
                val list = SiteViewModel.getSearchResultActive().getList()
                model.update { it.copy(quickSearchResult = CopyOnWriteArrayList(list), detail = chooseVod) }
                fromSearchLoadJob = SiteViewModel.viewModelScope.launch {
                    if(model.value.quickSearchResult.isNotEmpty()) loadDetail(model.value.quickSearchResult[0])
                }
            }else{
                val dt = SiteViewModel.detailContent(chooseVod.site?.key ?: "", chooseVod.vodId)
                if (dt == null || dt.detailIsEmpty()) {
                    quickSearch()
                } else {
                    var detail = dt.list[0]
                    detail =
                        detail.copy(subEpisode = detail.currentFlag?.episodes?.getPage(detail.currentTabIndex))
                    if (StringUtils.isNotBlank(getChooseVod().vodRemarks)) {
                        for (it: Episode in detail.subEpisode ?: listOf()) {
                            if (it.name.equals(getChooseVod().vodRemarks)) {
                                it.activated = true
                                break
                            }
                        }
                    }
                    detail.site = getChooseVod().site
                    model.update { it.copy(detail = detail) }
                }
            }
        }
    }

    override fun quickSearch() {
        model.update { it.copy(isQuickSearch = true) }
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
                    if(it == null) log.debug("一个job执行完毕 result size:{}", model.value.quickSearchResult.size)

                    synchronized(lock) {
                        if (model.value.quickSearchResult.isNotEmpty() && model.value.detail == null && !launched) {
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
            model.update { it.copy(isQuickSearch = false) }
        }
    }

    override fun loadDetail(vod: Vod) {
        showProgress()
        try {
            val dt = SiteViewModel.detailContent(vod.site?.key!!, vod.vodId)
            if (dt == null || dt.detailIsEmpty()) {
                nextSite(vod)
            } else {
                var first = dt.list[0]
                first = first.copy(
                    subEpisode = first.vodFlags[0]?.episodes?.getPage(first.currentTabIndex)?.toMutableList()
                )
                first.site = vod.site
                setDetail(first)
                launched = false
                supervisor.cancelChildren()
                jobList.cancelAll().clear()
            }
        } finally {
            hideProgress()
        }
    }

    override fun nextSite(lastVod: Vod?) {
        if (model.value.quickSearchResult.isEmpty()) return
        val list = model.value.quickSearchResult
        if (lastVod != null) list.remove(lastVod)
        model.update { it.copy(quickSearchResult = list) }
        if (model.value.quickSearchResult.isNotEmpty()) loadDetail(model.value.quickSearchResult[0])
    }

    fun buildVodList(): List<Vod> {
        val list = mutableListOf<Vod>()
        for (i in 0 until 30) {
            list.add(Vod(vodId = "$i", vodName = "name$i", vodRemarks = "remark$i"))
        }
        return list
    }

    override fun clear() {
        SiteViewModel.viewModelScope.launch {
            supervisor.cancel("退出详情页")
            delay(2000)
            jobList.forEach{it.cancel("detail clear")}
            jobList.clear()
            model.update { it.copy(quickSearchResult = CopyOnWriteArrayList(), detail = null) }
            SiteViewModel.clearQuickSearch()
            launched = false
        }
    }

    override fun getChooseVod(): Vod {
        return GlobalModel.chooseVod.value
    }

    override fun setDetail(vod: Vod) {
        if (!currentSiteKey.equals(vod.site?.key)) {
            SnackBar.postMsg("正在切换站源至 [${vod.site!!.name}]")
        }
        model.update { it.copy(detail = vod) }
    }
}