package com.corner.ui.nav.data

import com.corner.catvodcore.bean.Vod
import com.corner.catvodcore.bean.Episode
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.Url
import java.util.concurrent.CopyOnWriteArrayList


data class DetailScreenState(
    var siteKey: String = "",
    var detail: Vod = Vod(),
    var quickSearchResult: CopyOnWriteArrayList<Vod> = CopyOnWriteArrayList(),
    var isLoading: Boolean = false,
    var currentPlayUrl: String = "",
    var currentEp: Episode? = null,
    var showEpChooserDialog: Boolean = false,
    val currentUrl: Url? = null,
    val playResult: Result? = null,
    val loadingMessage: String = "",
    var isCleaning: Boolean = false,
    var isBuffering: Boolean = false,
    var isDLNA: Boolean = false
)
