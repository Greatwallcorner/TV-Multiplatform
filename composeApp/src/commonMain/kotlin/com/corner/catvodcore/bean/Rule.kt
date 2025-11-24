package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    var name: String,
    var hosts: List<String>,
    var regex: List<String>? = null
)
