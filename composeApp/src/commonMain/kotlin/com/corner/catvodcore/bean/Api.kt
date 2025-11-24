package com.corner.catvodcore.bean

import com.corner.catvod.enum.bean.Live
import com.corner.database.entity.Config
import com.github.catvod.bean.Doh
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Api(
    @Transient
    var id: String? = null,
    val spider: String = "",
    var sites: MutableSet<Site> = mutableSetOf(),
    val lives: MutableSet<Live> = mutableSetOf(),
    val parses: MutableSet<Parse> = mutableSetOf(),
    val doh: MutableSet<Doh> = mutableSetOf(),
    val rules: MutableSet<Rule> = mutableSetOf(),
    val flags: MutableSet<String> = mutableSetOf(),
    @Transient
    var url: String? = "",
    @Transient
    var data: String = "",
    @Transient
    var recent: String? = null,
    @Transient
    var cfg: Config? = null,
    @Transient
    var ref: Int = 0,
) {
    @Transient
    var live: Config? = null
}