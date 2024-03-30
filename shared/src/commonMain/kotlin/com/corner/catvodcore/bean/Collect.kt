package com.corner.catvodcore.bean

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import kotlin.math.max

class Collect{
    private var activated = mutableStateOf(false)
    private var list: MutableList<Vod>? = null
    private var site: Site? = null
    private var page = 0

    constructor(site: Site, vodList: MutableList<Vod>){
        this.site = site
        this.list = vodList
    }

    fun getSite(): Site? {
        return site
    }

    fun getList(): MutableList<Vod>? {
        return list
    }

    fun isActivated(): MutableState<Boolean> {
        return activated
    }

    fun setActivated(activated: Boolean) {
        this.activated.value = activated
    }

    fun getPage(): Int {
        return max(1.0, page.toDouble()).toInt()
    }

    fun setPage(page: Int) {
        this.page = page
    }

    fun Collect() {
    }
    companion object{
        fun all(): Collect {
            val item: Collect = Collect(Site.get("all", "全部"), mutableListOf())
            item.activated.value = true
            return item
        }

        fun create(list: MutableList<Vod>): Collect {
            return Collect(list[0].site!!, list)
        }
    }
}