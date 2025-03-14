package com.corner.ui.decompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import com.corner.ui.search.SearchPageType
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArraySet

interface SearchComponent {
    val model: MutableValue<Model>
    data class Model(
        val hotList:List<HotData>,
        val historyList:Set<String>,
        val searchScope: CoroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(8) + SupervisorJob()),
        var isSearching: Boolean = false,
        var jobList: MutableList<Job> = mutableListOf<Job>(),
        var currentVodList:MutableState<MutableList<Vod>> = mutableStateOf(mutableListOf()),
        var currentPage:SearchPageType = SearchPageType.page,
        var searchCompleteSites:CopyOnWriteArraySet<String> = CopyOnWriteArraySet(),
        var searchableSites:CopyOnWriteArraySet<Site> = CopyOnWriteArraySet(),
        var searchBarText: String = "",
        var ref:Int = 0
    ) {
        fun cancelJobAndClear() {
            jobList.forEach { i -> i.cancel("search clear") }
            jobList.clear()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Model

            if (hotList != other.hotList) return false
            if (historyList != other.historyList) return false
            if (searchScope != other.searchScope) return false
            if (isSearching != other.isSearching) return false
            if (jobList != other.jobList) return false
            if (currentVodList != other.currentVodList) return false
            if (currentPage != other.currentPage) return false
            if (searchBarText != other.searchBarText) return false
            if (searchableSites != other.searchableSites) return false
            if (searchCompleteSites != other.searchCompleteSites) return false
            if (ref != other.ref) return false

            return true
        }

        override fun hashCode(): Int {
            var result = hotList.hashCode()
            historyList.forEach{result += 31 * it.hashCode() }
            result = 31 * result + searchScope.hashCode()
            result = 31 * result + isSearching.hashCode()
            result = 31 * result + jobList.hashCode()
            currentVodList.value.forEach { result += 31 * it.hashCode() }
            result = 31 * result + currentVodList.hashCode()
            result = 31 * result + currentPage.hashCode()
            result = 31 * result + ref.hashCode()
            result = 31 * result + searchCompleteSites.hashCode()
            result = 31 * result + searchableSites.hashCode()
            return result
        }

    }

    fun search(searchText: String, isLoadMore: Boolean)
    fun clear()
    fun onClickCollection(item: Collect)
}