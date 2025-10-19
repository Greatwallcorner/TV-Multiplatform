package com.corner.catvodcore.bean

import com.corner.catvodcore.util.Utils
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

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

    /**
     * name$url#name1$url1
     */
    fun createEpisode(url: String) {
        if(url.isBlank()) return
        val urls:List<String> = if(url.contains("#")) url.split("#") else listOf(url)
        for (i in urls.indices) {
            val split = urls[i].split("\\$".toRegex())
            val number = String.format(Locale.getDefault(), "%02d", i + 1)
            val episode = if(split.size > 1) Episode.create(if(split[0].isEmpty()) number else split[0].trim(), split[1]) else Episode.create(number, urls[i])
            if(!episodes.contains(episode)) episodes.add(episode)
        }
        episodes.sortBy { it.number }
    }

    fun find(remarks: String, strict: Boolean): Episode? {
        val number: Int = Utils.getDigit(remarks)
        if (episodes.size == 0) return null
        if (episodes.size == 1) return episodes.get(0)
        for (item in episodes) if (item.rule1(remarks)) return item
        for (item in episodes) if (item.rule2(number)) return item
        if (number == -1) for (item in episodes) if (item.rule3(remarks)) return item
        if (number == -1) for (item in episodes) if (item.rule4(remarks)) return item
        if (position != -1) return episodes[position]
        return if (strict) null else episodes[0]
    }

    fun isEmpty():Boolean{
        return episodes.size == 0
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
