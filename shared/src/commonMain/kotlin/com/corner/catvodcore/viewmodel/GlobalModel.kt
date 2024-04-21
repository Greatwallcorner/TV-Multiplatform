package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod

object GlobalModel {
    val hotList = MutableValue(listOf<HotData>())
    val chooseVod = mutableStateOf<Vod>(Vod())
    var detailFromSearch = false
    val home = MutableValue<Site>(Site.get("",""))
}