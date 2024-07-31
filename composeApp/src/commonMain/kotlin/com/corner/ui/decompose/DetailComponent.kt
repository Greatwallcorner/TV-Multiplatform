package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Episode
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.Url
import com.corner.database.History
import com.corner.ui.player.vlcj.VlcjFrameController
import java.util.concurrent.CopyOnWriteArrayList

interface DetailComponent {

    val model: MutableValue<Model>

    val controller:VlcjFrameController

    data class Model(
        var siteKey:String = "",
        var detail:Vod? = null,
        var quickSearchResult:CopyOnWriteArrayList<Vod> = CopyOnWriteArrayList(),
        var isLoading: Boolean = false,
        var currentPlayUrl:String = "",
        var currentEp: Episode? = null,
        var showEpChooserDialog:Boolean = false,
        var shouldPlay:Boolean = false,
        val currentUrl:Url? = null,
        val playResult:Result? = null
    )

    fun load()

    fun quickSearch()

    fun loadDetail(vod:Vod)

    fun nextSite(lastVod:Vod?)

    fun clear()

    fun getChooseVod(): Vod

    fun setDetail(vod: Vod)
    fun play(result: Result?)
    fun startPlay()
    fun nextEP()
    fun playEp(detail: Vod, ep: Episode)
    fun updateHistory(it:History?)
    fun nextFlag()
    fun syncHistory()
}