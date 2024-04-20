package com.corner.ui.decompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Collect
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

interface SearchComponent {
    val model: MutableValue<Model>
    data class Model(
        val hotList:List<HotData>,
        val historyList:Set<String>,
        val searchScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        var isSearching: MutableState<Boolean> = mutableStateOf(false),
        var jobList: MutableList<Job> = mutableListOf<Job>(),
        var currentVodList:CopyOnWriteArrayList<Vod> = CopyOnWriteArrayList()
    ) {
        fun cancelAndClearJobList() {
            jobList.forEach { i -> i.cancel("search") }
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

            return true
        }

        override fun hashCode(): Int {
            var result = hotList.hashCode()
            historyList.forEach{result += 31 * it.hashCode() }
            result = 31 * result + searchScope.hashCode()
            result = 31 * result + isSearching.hashCode()
            result = 31 * result + jobList.hashCode()
            currentVodList.forEach { result += 31 * it.hashCode() }
            result = 31 * result + currentVodList.hashCode()
            return result
        }

    }

    fun search(searchText:String)

    fun clear()
    fun onClickCollection(item: Collect)
}