package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.corner.bean.SettingStore
import com.corner.bean.SettingStore.getHistoryList
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.BaseComponent
import com.corner.ui.decompose.SearchComponent
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

open class DefaultSearchComponent(componentContext: ComponentContext) : SearchComponent,
    BaseComponent(Dispatchers.IO.limitedParallelism(12)),
    ComponentContext by componentContext, BackHandlerOwner {
    private var searchedText: String = ""

    private val onceSearchSiteNum = 4

    private val log = LoggerFactory.getLogger("Search")

    private val _models: MutableValue<SearchComponent.Model> = MutableValue(
        SearchComponent.Model(
            GlobalModel.hotList.value,
            getHistoryList()
        )
    )

    override val model: MutableValue<SearchComponent.Model> = _models

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onCreate() {
                model.value.searchableSites.addAll(ApiConfig.api.sites.filter {
                    it.searchable == 1 && !model.value.searchCompleteSites.contains(
                        it.key
                    )
                }.shuffled().map { it.copy() })
                model.value.searchBarText = getSearchBarText(model.value)
            }

            override fun onDestroy() {
                log.debug("search onDestroy")
                model.value.searchScope.cancel("onDestroy")
            }
        })

        scope.launch {
            model.subscribe {
                model.update { it.copy(searchBarText = getSearchBarText(model.value)) }
            }
        }
    }

    fun getSearchBarText(model: SearchComponent.Model): String {
        val size = model.searchableSites.filter { it.isSearchable() }.size
        if (model.isSearching) return "${model.searchCompleteSites.size}/$size"
        return "$size"
    }

    /**
     * 当用户从搜索页面进入详情页， 而后返回搜索页 因为 DisposableEffect 会再次执行
     * 这个搜索方法，为避免不必要的搜索操作， searchedText记录上上次执行搜索的文本，如果一样就不再执行搜索，
     * isLoadMore用来指示搜索操作的来源，一个是 DisposableEffect， 另一个是加载更多按钮
     */
    override fun search(searchText: String, isLoadMore: Boolean) {
        log.info("开始搜索：{}", searchText)
        if (searchedText == searchText && !isLoadMore) {
            log.debug("已经搜索过：{}，忽略", searchText)
            return
        }
        if (model.value.searchCompleteSites.size == ApiConfig.api.sites.size) {
            log.info("所有站源已经搜索完毕")
            return
        }
        searchedText = searchText
        if (!isLoadMore) {
            clear()
        }
        model.update { it.copy(isSearching = true) }
        SiteViewModel.viewModelScope.launch {
            SettingStore.addSearchHistory(searchText)
            var searchableSites =
                model.value.searchableSites.filter { it.searchable == 1 && !model.value.searchCompleteSites.contains(it.key) }
                    .shuffled()
            searchableSites = searchableSites.subList(0, onceSearchSiteNum.coerceAtMost(searchableSites.size))
            log.info("站源：{}", searchableSites.map { it.name })
            searchableSites.forEach {
                val job = model.value.searchScope.launch {
                    model.value.searchCompleteSites.add(it.key)
                    SiteViewModel.searchContent(it, searchText, false)
                }
                job.invokeOnCompletion { e ->
                    if (e != null) {
                        if (e is CancellationException) {
                            log.debug("搜索被取消 msg:${e.message}")
                        } else {
                            log.error("搜索执行 异常 msg:{}", e.message)
                        }
                    }
                    model.value.currentVodList.value = (SiteViewModel.search.value.find { it.activated.value }
                        ?.list ?: listOf()).toMutableList()
                    if (e == null) log.debug("一个job执行完毕 List size:{}", model.value.currentVodList.value.size)
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
        model.value.cancelJobAndClear()
        SiteViewModel.clearSearch()
        model.value.searchCompleteSites.clear()
        model.value.currentVodList.value = CopyOnWriteArrayList()
        model.update {
            it.copy(
                isSearching = false
            )
        }
    }

    override fun onClickCollection(item: Collect) {
        model.value.currentVodList.value = CopyOnWriteArrayList(item.list)
//        model.update { it.copy(currentVodList = CopyOnWriteArrayList(item.getList())) }

    }

    fun updateModel(function: (SearchComponent.Model) -> Unit) {
        function(model.value)
        val searchBarText = getSearchBarText(model.value)
        _models.update { it.copy(ref = it.ref + 1, searchBarText = searchBarText) }
    }

}