package com.corner.ui.nav.data

import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Filter
import com.corner.catvodcore.bean.Type
import java.util.concurrent.atomic.AtomicInteger


data class VideoScreenState(
    var homeVodResult: MutableSet<Vod> = mutableSetOf(),
    var homeLoaded: Boolean = false,
    var classList: MutableSet<Type> = mutableSetOf(),
    var filtersMap: MutableMap<String, List<Filter>> = mutableMapOf(),
    var currentClass: Type? = null,
    var currentFilters: List<Filter> = listOf(),
    var page: AtomicInteger = AtomicInteger(1),
    var isRunning: Boolean = false,
    val prompt:String = "",
    val dirPaths:MutableList<String> = mutableListOf(), // 当前目录路径
    var ref:Int = 0
)