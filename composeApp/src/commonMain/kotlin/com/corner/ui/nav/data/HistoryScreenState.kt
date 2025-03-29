package com.corner.ui.nav.data

import com.corner.database.entity.History


data class HistoryScreenState(
    var historyList: MutableList<History> = mutableListOf(),
)