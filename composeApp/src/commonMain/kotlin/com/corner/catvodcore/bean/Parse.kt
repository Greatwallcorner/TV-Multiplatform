package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable

@Serializable
data class Parse(
    var name: String,
    var type: Int,
    var url: String,
    var ext: Ext? = null
)

@Serializable
data class Ext(val flag:List<String>? = null,val header:Map<String, String>? = null)
