package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.corner.bean.SettingStore
import com.corner.bean.SettingStore.getHistoryList
import com.corner.catvodcore.config.api
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.SearchComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

class DefaultSearchComponentComponent(componentContext: ComponentContext):SearchComponent, ComponentContext by componentContext, BackHandlerOwner {

    private val log = LoggerFactory.getLogger("Search")

    private val _models:MutableValue<SearchComponent.Model> = MutableValue(
        SearchComponent.Model(GlobalModel.hotList.value,
            getHistoryList())
    )

    override val model: MutableValue<SearchComponent.Model> = _models

    override fun search(searchText:String) {
        log.info("开始搜索：{}", searchText)
        model.value.isSearching.value = true
        model.value.cancelAndClearJobList()
        SiteViewModel.clearSearch()
        SiteViewModel.viewModelScope.launch {
            SettingStore.addSearchHistory(searchText)
            val searchableSites = api?.sites?.filter { it.searchable == 1 }
            searchableSites?.forEach {
                val job = model.value.searchScope.launch {
                    SiteViewModel.searchContent(it, searchText, false)
                }
                job.invokeOnCompletion {
                    if(it != null){
                        log.error("搜索执行 异常 msg:{}", it.message)
                    }
                    model.update { it.copy(currentVodList = CopyOnWriteArrayList(SiteViewModel.search.find { it.isActivated().value }?.getList() ?: listOf())) }
                    if(it == null) log.debug("一个job执行完毕 List size:{}", model.value.currentVodList.size)
                }
                model.value.jobList.add(job)
            }
            model.value.searchScope.coroutineContext[Job]?.children?.forEach { it.join() }
            model.value.isSearching.value = false
        }
    }

    override fun clear() {
        log.info("清空工作区")
        SiteViewModel.clearSearch()
        model.value.cancelAndClearJobList()
        model.value.isSearching.value = false
    }

}