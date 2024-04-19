package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.catvod.enum.bean.Vod
import java.util.concurrent.CopyOnWriteArrayList

interface DetailComponent {

    val model: MutableValue<Model>
    data class Model(
        var siteKey:String = "",
        var detail:Vod? = null,
        var quickSearchResult:CopyOnWriteArrayList<Vod> = CopyOnWriteArrayList()
    )

    fun load()

    fun quickSearch()

    fun loadDetail(vod:Vod)

    fun nextSite(lastVod:Vod?)

    fun clear()

    fun getChooseVod(): Vod

    fun setDetail(vod: Vod)
}