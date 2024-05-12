package com.corner.catvodcore.bean

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
class Episode {
    companion object {
        fun create(name: String, string: String): Episode {
            val episode = Episode()
            episode.name = name
            episode.url = string
            return episode
        }
    }

    @SerializedName("name")
    var name: String? = null

    @SerializedName("desc")
    val desc: String? = null

    @SerializedName("url")
    var url: String? = null

    val number = 0
    var activated = false
    val selected = false
}