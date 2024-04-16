package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod

object GlobalModel {
    val hotList = mutableStateOf(mutableListOf<HotData>())
    val chooseVod = mutableStateOf<Vod>(Vod())
    val home = MutableValue<Site>(Site.get("",""))
}