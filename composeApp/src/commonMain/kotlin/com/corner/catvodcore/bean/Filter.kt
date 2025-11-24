package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable

@Serializable
data class Filter (
    val key: String? = null,

    val name: String? = null,

    var init: String = "",

    val value: List<Value>? = null
){
    companion object{
        val ALL = Filter("", "全部")
    }
}

fun Filter.isEmpty():Boolean{
    return value?.isEmpty() ?: true
}

fun List<Filter>.getFirstOrEmpty():Filter{
    return if(this.isNotEmpty()) this[0] else Filter.ALL
}