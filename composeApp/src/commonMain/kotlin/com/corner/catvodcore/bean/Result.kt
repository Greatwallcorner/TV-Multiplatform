package com.corner.catvodcore.bean

import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.util.JsonStrToMapSerializer
import com.corner.catvodcore.util.ToStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Result {
    @SerialName("class")
    var types: MutableList<Type> = mutableListOf()

    var list: MutableList<Vod> = mutableListOf()

    val filters: MutableMap<String, List<Filter>> = mutableMapOf()

    @Serializable(JsonStrToMapSerializer::class)
    var header:Map<String,String>? = null

    var playUrl: String? = null

    val jxFrom: String? = null

    var parse: Int? = null

    val jx: Int? = null

    var flag: String? = null

    val danmaku: String? = null

    val format: String? = null

    @Serializable(UrlSerializable::class)
    var url: Url = Url()

    var key: String? = null

    val subs: List<Sub>? = null

    val pagecount: Int? = null

    val code: Int? = null

    @Serializable(ToStringSerializer::class)
    val msg: String? = null

    @Transient
    var isSuccess:Boolean = true

    constructor(isSuccess:Boolean = true){
        this.isSuccess = isSuccess
    }
}

fun Result.detailIsEmpty():Boolean{
    return list.isEmpty() || list[0].vodFlags[0]?.episodes?.isEmpty() == true
}

fun Result.playResultIsEmpty():Boolean{
    return url.isEmpty()
}
