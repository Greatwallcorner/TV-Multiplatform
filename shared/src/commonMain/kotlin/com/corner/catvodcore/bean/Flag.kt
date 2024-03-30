package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.Comparator

/**
@author heatdesert
@date 2023-12-28 21:43
@description
 */
@Serializable
data class Flag (
    var flag: String? = null,
    var show: String? = null,

    val urls: String? = null,

    var episodes: MutableList<Episode> = mutableListOf(),
    @Transient
    var activated: Boolean = false,
    @Transient
    var position: Int = 0
){
    fun createEpisode(url: String) {
        if(url.isBlank()) return
        val urls:List<String> = if(url.contains("#")) url.split("#") else listOf(url)
        for (i in urls.indices) {
            val split = urls[i].split("\\$".toRegex())
            val number = String.format(Locale.getDefault(), "%02d", i + 1)
            val episode = if(split.size > 1) Episode.create(if(split[0].isEmpty()) number else split[0].trim(), split[1]) else Episode.create(number, urls[i])
            if(!episodes.contains(episode)) episodes.add(episode)
        }
        episodes.sortWith(Comparator<Episode> { o1, o2 ->
            o1.name?.compareTo(o2.name ?: "")!!
        })

    }

    companion object{
        fun create(flag:String):Flag{
            val f = Flag()
            f.episodes = mutableListOf()
            f.show = flag
            f.flag = flag
            f.position = -1
            return f
        }
    }
}
