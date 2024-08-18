package com.corner.catvod.enum.bean

import com.corner.catvodcore.util.ToStringSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Site(
    var key: String,
    var name: String,
    var type: Int,
    var api: String,
    var searchable: Int? = null,
    var changeable: Int? = null,
    var playUrl: String? = null,
    var quickSearch: Int? = null,
    var categories: Set<String> = mutableSetOf(),
    @Serializable(ToStringSerializer::class)
    var ext: String= "",
    var style: Style? = null,
    var timeout: Int? = null,
    var jar:String? = null,
    var header:Map<String,String> = mutableMapOf()
){
    companion object{
        fun get(key:String, name: String):Site{
            return Site(key, name, -1, "")
        }
    }

    fun isEmpty(): Boolean {
        return key.isEmpty() && name.isEmpty()
    }
}
