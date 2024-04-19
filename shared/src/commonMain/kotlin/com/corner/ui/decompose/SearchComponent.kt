package com.corner.ui.decompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Vod
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
    }

    fun search(searchText:String)

    fun clear()
}