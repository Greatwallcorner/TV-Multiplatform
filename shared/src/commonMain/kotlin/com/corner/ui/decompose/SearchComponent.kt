package com.corner.ui.decompose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.HotData
import kotlinx.coroutines.*

interface SearchComponent {
    val models: MutableValue<Model>
    data class Model(
        val hotList:List<HotData>,
        val historyList:Set<String>,
        val searchScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        var isSearching: MutableState<Boolean> = mutableStateOf(false),
        var jobList: MutableList<Job> = mutableListOf<Job>()
    ) {
        fun cancelAndClearJobList() {
            jobList.forEach { i -> i.cancel("search") }
            jobList.clear()
        }
    }
}