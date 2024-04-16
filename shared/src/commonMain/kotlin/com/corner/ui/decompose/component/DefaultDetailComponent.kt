package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.getPage
import com.corner.catvodcore.bean.Episode
import com.corner.catvodcore.bean.detailIsEmpty
import com.corner.catvodcore.config.api
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.scene.SnackBar
import com.corner.util.Utils.cancelAll
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

class DefaultDetailComponent(componentContext: ComponentContext) : DetailComponent,
    ComponentContext by componentContext {
    private val _model = MutableValue(DetailComponent.Model())

    private var supervisor = SupervisorJob()
    private val searchScope = CoroutineScope(Dispatchers.Default + supervisor)

    private val log = LoggerFactory.getLogger("Detail")

    private val lock = Any()
    @Volatile
    private var launched = false

    private val jobList = mutableListOf<Job>()

    override val model: MutableValue<DetailComponent.Model> = _model


    override fun load() {
        val chooseVod = getChooseVod()
        SiteViewModel.viewModelScope.launch {
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
                            break;
                        }
                    }
                }
                detail.site = getChooseVod().site
                model.update { it.copy(detail = detail) }
            }
        }
    }
     override fun quickSearch() {
        searchScope.launch {
            val quickSearchSites = api?.sites?.filter { it.changeable == 1 }
            quickSearchSites?.forEach {
                val job = launch() {
                    withTimeout(2500L) {
                        SiteViewModel.searchContent(it, getChooseVod().vodName ?: "", true)
                        log.debug("{}完成搜索", it.name)
                    }
                }

                job.invokeOnCompletion {
                    model.update { it.copy(quickSearchResult = SiteViewModel.quickSearch.get(0).getList()?.toList() ?: listOf()) }
                    log.debug("一个job执行完毕 result size:{}", model.value.quickSearchResult.size)

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
//                model.update { it.copy(chooseVod = vod) }
                SnackBar.postMsg("暂无线路数据")
            }
        }
    }

     override fun loadDetail(vod: Vod) {
        val dt = SiteViewModel.detailContent(vod.site?.key!!, vod.vodId)
        if (dt == null || dt.detailIsEmpty()) {
            nextSite(vod)
        } else {
            if (vod.site?.name?.isNotBlank() == true && model.value.detail != null && !model.value.detail?.site?.name?.equals(vod.site!!.name)!!) SnackBar.postMsg(
                "正在切换站源至 [${vod.site!!.name}]"
            )
            var first = dt.list[0]
            first = first.copy(
                subEpisode = first.vodFlags.get(0)?.episodes?.getPage(first.currentTabIndex)?.toMutableList()
            )
            first.site = vod.site
            model.update { it.copy(detail = first) }
            launched = false
//        searchScope.cancel("已找到可用站源${detail?.site?.name}")
            supervisor.cancelChildren()
            jobList.cancelAll().clear()
        }
    }

    override fun nextSite(lastVod: Vod?) {
        if (model.value.quickSearchResult.isEmpty()) return
        val list = model.value.quickSearchResult.toMutableList()
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

    override fun clear(){
        model.update { it.copy(quickSearchResult = listOf(), detail = null) }
        SiteViewModel.clearQuickSearch()
        jobList.cancelAll().clear()
        launched = false
    }

    override fun getChooseVod(): Vod {
        return GlobalModel.chooseVod.value
    }
}