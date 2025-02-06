package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Db
import com.corner.database.entity.History
import com.corner.ui.decompose.BaseComponent
import com.corner.ui.decompose.HistoryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DefaultHistoryComponent(componentContext: ComponentContext) : HistoryComponent, BaseComponent(Dispatchers.IO),
    ComponentContext by componentContext {
    private val _model = MutableValue(
        HistoryComponent.Model(
            emptyList<History>().toMutableList()
        )
    )

    override val model: MutableValue<HistoryComponent.Model> = _model
    override fun fetchHistoryList() {
        val listFlow = Db.History.findAll(ApiConfig.api.cfg.value?.id)
        scope.launch {
            val list = listFlow.firstOrNull() ?: emptyList<History>()
            _model.update { it.copy(historyList = list.toMutableList()) }
        }
    }

    fun clearHistory() {
        scope.launch {
            Db.History.deleteAll()
        }
    }

    fun deleteBatchHistory(listOf: List<String>) {
        scope.launch {
            Db.History.deleteBatch(listOf)
        }
    }


}