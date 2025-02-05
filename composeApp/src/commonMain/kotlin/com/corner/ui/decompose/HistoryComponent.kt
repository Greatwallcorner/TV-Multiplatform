package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.database.entity.History

interface HistoryComponent {
    val model:MutableValue<Model>

    data class Model(
        var historyList: MutableList<History>
    )

    fun fetchHistoryList()
}