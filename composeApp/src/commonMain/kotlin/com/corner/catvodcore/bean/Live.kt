package com.corner.catvod.enum.bean

import kotlinx.serialization.Serializable

@Serializable
class Live(
    val name: String,
    val type: Int,
    val url: String,
    val playerType: Int = 0,
    val epg: String? = null,
    val logo: String? = null
)
