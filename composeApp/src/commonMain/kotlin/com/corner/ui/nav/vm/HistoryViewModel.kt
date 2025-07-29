package com.corner.ui.nav.vm

import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Db
import com.corner.database.entity.History
import com.corner.ui.nav.BaseViewModel
import com.corner.ui.nav.data.HistoryScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class HistoryViewModel: BaseViewModel() {
    private val _state = MutableStateFlow(HistoryScreenState())
    val state: StateFlow<HistoryScreenState> = _state

    fun clearHistory() {
        scope.launch {
            Db.History.deleteAll()
        }
    }

    fun deleteBatchHistory(listOf: List<String>) {
        scope.launch {
            Db.History.deleteBatch(listOf)
            fetchHistoryList() // 在删除完成后立即刷新
        }
    }

    fun fetchHistoryList() {
        scope.launch {
            Db.History.findAll(ApiConfig.api.cfg?.id).collect { list ->
                _state.update { it.copy(historyList = list.toMutableList()) }
            }
        }
    }
}