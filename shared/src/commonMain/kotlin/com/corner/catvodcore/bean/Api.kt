package com.corner.catvod.enum.bean

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.github.catvod.bean.Doh
import com.corner.database.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Api(
    val spider: String,
    val sites: MutableSet<Site>?,
    val lives: MutableSet<Live>? = null,
    val parses: MutableSet<Parse>? = null,
    val doh: MutableSet<Doh>? = null,
    val rules: MutableSet<Rule>? = null,
    val flags: MutableSet<String>? = null
) {
    @Transient
    var url: String? = null

    @Transient
    var data:String? = null

    @Transient
    var recent: String? = null

    @Transient
    var home: MutableState<Site?>? = mutableStateOf(null)

    @Transient
    var cfg: MutableState<Config?> = mutableStateOf(null)

    @Transient
    var live: Config? = null

    @Transient
    var id:String? = null
}