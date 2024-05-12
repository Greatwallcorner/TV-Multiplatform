package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.corner.catvodcore.config.api
import com.corner.database.Db
import com.corner.database.History
import com.corner.ui.decompose.HistoryComponent

class DefaultHistoryComponent(componentContext: ComponentContext):HistoryComponent, ComponentContext by componentContext {
    private val _model = MutableValue(HistoryComponent.Model(
        emptyList<History>().toMutableList()
    ))

    override val model: MutableValue<HistoryComponent.Model> = _model
    override fun fetchHistoryList() {
        val list = Db.History.findAll(api?.cfg?.value?.id)
        _model.update { it.copy(historyList = list.toMutableList()) }
    }


}