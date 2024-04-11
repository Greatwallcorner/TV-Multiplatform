package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.corner.bean.HotData

object GlobalModel {
    val hotList = mutableStateOf(mutableListOf<HotData>())
}