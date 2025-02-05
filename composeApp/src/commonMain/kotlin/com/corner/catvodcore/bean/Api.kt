package com.corner.catvodcore.bean

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.corner.catvod.enum.bean.Live
import com.corner.catvod.enum.bean.Parse
import com.corner.catvod.enum.bean.Rule
import com.corner.catvod.enum.bean.Site
import com.corner.database.entity.Config
import com.github.catvod.bean.Doh
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Api(
    val spider: String = "",
    val sites: MutableSet<Site> = mutableSetOf(),
    val lives: MutableSet<Live> = mutableSetOf(),
    val parses: MutableSet<Parse> = mutableSetOf(),
    val doh: MutableSet<Doh> = mutableSetOf(),
    val rules: MutableSet<Rule> = mutableSetOf(),
    val flags: MutableSet<String> = mutableSetOf()
) {
    @Transient
    var url: String? = ""

    @Transient
    var data:String = ""

    @Transient
    var recent: String? = null

//    @Transient
//    var home: MutableState<Site?>? = mutableStateOf(null)

    @Transient
    var cfg: MutableState<Config?> = mutableStateOf(null)

    @Transient
    var live: Config? = null

    @Transient
    var id:String? = null
}