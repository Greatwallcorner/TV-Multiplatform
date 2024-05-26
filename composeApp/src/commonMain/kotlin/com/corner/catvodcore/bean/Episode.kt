package com.corner.catvodcore.bean

import com.corner.catvodcore.util.Utils
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Episode(
    @SerializedName("name")
    var name: String = "",
    @SerializedName("desc")
    val desc: String? = null,
    @SerializedName("url")
    var url: String = "",
    var number: Int = 0,
    var activated: Boolean = false,
    val selected: Boolean = false
) {
    companion object {
        fun create(name: String, string: String): Episode {
            val episode = Episode()
            episode.number = Utils.getDigit(name)
            episode.name = name
            episode.url = string
            return episode
        }

    }

    fun rule1(name: String?): Boolean {
        return name.equals(name, ignoreCase = true)
    }

    fun rule2(number: Int): Boolean {
        return this.number == number && number != -1
    }

    fun rule3(name: String): Boolean {
        return name.lowercase(Locale.getDefault()).contains(name.lowercase(Locale.getDefault()))
    }

    fun rule4(name: String): Boolean {
        return name.lowercase(Locale.getDefault()).contains(name.lowercase(Locale.getDefault()))
    }
}