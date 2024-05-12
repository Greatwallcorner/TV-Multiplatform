package com.corner.catvodcore.util

import java.net.URI

object Urls {
    fun convert(url:String):String{
        if(url.startsWith("file://")) return url.replace("file://","")
        return url
    }

    fun convert(baseUrl:String, refUrl:String):String{
//        val convert = convert(baseUrl)
        return URI(baseUrl).resolve(refUrl).toString()
    }
}