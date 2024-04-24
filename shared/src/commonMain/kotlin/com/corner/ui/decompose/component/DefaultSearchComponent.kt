package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.corner.bean.SettingStore
import com.corner.bean.SettingStore.getHistoryList
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.config.api
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.SearchComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class DefaultSearchComponent(componentContext: ComponentContext):SearchComponent, ComponentContext by componentContext, BackHandlerOwner {
    private var searchedText: String = ""

    private val onceSearchSiteNum = 4

    private val log = LoggerFactory.getLogger("Search")

    private val _models:MutableValue<SearchComponent.Model> = MutableValue(
        SearchComponent.Model(GlobalModel.hotList.value,
            getHistoryList())
    )

    override val model: MutableValue<SearchComponent.Model> = _models

    /**
     * 当用户从搜索页面进入详情页， 而后返回搜索页 因为 DisposableEffect 会再次执行
     * 这个搜索方法，为避免不必要的搜索操作， searchedText记录上上次执行搜索的文本，如果一样就不再执行搜索，
     * isLoadMore用来指示搜索操作的来源，一个是 DisposableEffect， 另一个是加载更多按钮
     */
    override fun search(searchText:String, isLoadMore:Boolean) {
        log.info("开始搜索：{}", searchText)
        if(searchedText == searchText && !isLoadMore) {
            log.debug("已经搜索过：{}，忽略", searchText)
            return
        }
        if(model.value.searchCompleteSites.size == api?.sites?.size){
            log.info("所有站源已经搜索完毕")
            return
        }
        searchedText = searchText
        model.update { it.copy(isSearching = true) }
        if(!isLoadMore) {
            model.value.cancelAndClearJobList()
            SiteViewModel.clearSearch()
        }
        SiteViewModel.viewModelScope.launch {
            SettingStore.addSearchHistory(searchText)
            var searchableSites = api?.sites?.filter { it.searchable == 1 && !model.value.searchCompleteSites.contains(it.key)}?.shuffled()
                searchableSites = searchableSites?.subList(0, onceSearchSiteNum.coerceAtMost(searchableSites.size))
            log.info("站源：{}", searchableSites?.map { it.name })
            searchableSites?.forEach {
                val job = model.value.searchScope.launch {
                    model.value.searchCompleteSites.add(it.key)
                    SiteViewModel.searchContent(it, searchText, false)
                }
                job.invokeOnCompletion {
                    if(it != null && it !is CancellationException){
                        log.error("搜索执行 异常 msg:{}", it.message)
                    }
                    model.update { it.copy(currentVodList = CopyOnWriteArrayList(SiteViewModel.search.find { it.isActivated().value }?.getList() ?: listOf())) }
                    if(it == null) log.debug("一个job执行完毕 List size:{}", model.value.currentVodList.size)
                }
                model.value.jobList.add(job)
            }
            model.value.searchScope.coroutineContext[Job]?.children?.forEach { it.join() }
        }.invokeOnCompletion {
            model.update { it.copy(isSearching = false) }
        }
    }

    override fun clear() {
        log.info("清空工作区")
        SiteViewModel.clearSearch()
        model.value.cancelAndClearJobList()
        model.update { it.copy(isSearching = false,
            searchCompleteSites = CopyOnWriteArraySet()
        ) }
    }

    override fun onClickCollection(item: Collect) {
        model.update { it.copy(currentVodList = CopyOnWriteArrayList(item.getList())) }

    }

}