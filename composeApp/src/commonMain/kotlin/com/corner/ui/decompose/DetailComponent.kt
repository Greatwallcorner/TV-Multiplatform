package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Result
import com.corner.ui.player.PlayerController
import java.util.concurrent.CopyOnWriteArrayList

interface DetailComponent {

    val model: MutableValue<Model>

    var playerController:PlayerController?

    data class Model(
        var siteKey:String = "",
        var detail:Vod? = null,
        var quickSearchResult:CopyOnWriteArrayList<Vod> = CopyOnWriteArrayList(),
        var isLoading: Boolean = false,
        var currentPlayUrl:String = ""
    )

    fun load()

    fun quickSearch()

    fun loadDetail(vod:Vod)

    fun nextSite(lastVod:Vod?)

    fun clear()

    fun getChooseVod(): Vod

    fun setDetail(vod: Vod)
    fun play(result: Result?)
}