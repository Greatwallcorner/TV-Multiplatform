package com.corner.ui.nav.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.ui.search.SearchPageType
import java.util.concurrent.CopyOnWriteArraySet


data class SearchScreenState(
    var keyword:String = "",
    var isSearching:Boolean = false,
    val hotList:List<HotData> = listOf(),
    val historyList:Set<String> = setOf(),
    var currentVodList: MutableState<MutableList<Vod>> = mutableStateOf(mutableListOf()),
    var currentPage: SearchPageType = SearchPageType.page,
    var searchCompleteSites: CopyOnWriteArraySet<String> = CopyOnWriteArraySet(),
    var searchableSites: CopyOnWriteArraySet<Site> = CopyOnWriteArraySet(),
    var searchBarText: String = "",
    var ref:Int = 0,
    var searchedText: String = "",
    val onceSearchSiteNum: Int = 4
)