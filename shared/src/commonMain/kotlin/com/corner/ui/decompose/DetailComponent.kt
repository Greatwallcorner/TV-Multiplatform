package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.catvod.enum.bean.Vod

interface DetailComponent {

    val model: MutableValue<Model>
    data class Model(
        var chooseVod: Vod = Vod(),
        var siteKey:String = "",
        var detail:Vod? = null,
        var quickSearchResult:List<Vod> = listOf()
    )

    fun load()

    fun quickSearch()

    fun loadDetail(vod:Vod)

    fun nextSite(lastVod:Vod?)

    fun clear()

    fun getChooseVod(): Vod

}