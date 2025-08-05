package com.corner.catvodcore.util

import com.corner.util.thisLogger
import java.net.URI

object Urls {
    private val log = thisLogger()

    fun convert(url:String):String{
        if(url.startsWith("file:/")) return url.replace("file://","").replace("file:/","")
        return url
    }

    fun convert(baseUrl:String, refUrl:String):String{
        try {
            return URI(baseUrl.replace("file://", "file:/").replace("\\", "/")).resolve(refUrl).toString()
        } catch (e: Exception) {
            log.error("解析url失败 返回空值", e)
            return ""
        }
    }
}