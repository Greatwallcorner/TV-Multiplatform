package com.github.catvod.crawler

import com.corner.catvodcore.util.Http
import okhttp3.Dns
import okhttp3.OkHttpClient

open class Spider{
    open fun init() {
    }

    open fun init(extend: String?) {
        init()
    }

    open fun homeContent(filter: Boolean): String {
        return "{}"
    }

    open fun homeVideoContent(): String {
        return "{}"
    }

    open fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String {
        return "{}"
    }

    open fun detailContent(ids: List<String?>?): String {
        return "{}"
    }

    open fun searchContent(key: String?, quick: Boolean): String {
        return "{}"
    }

    open fun searchContent(key: String?, quick: Boolean, pg: String?): String {
        return "{}"
    }

    open fun playerContent(flag: String?, id: String?, vipFlags: List<String?>?): String {
        return "{}"
    }

    open fun manualVideoCheck(): Boolean {
        return false
    }

    open fun isVideoFormat(url: String?): Boolean {
        return false
    }

    open fun proxyLocal(params: Map<String?, String?>?): Array<Any>? {
        return null
    }

    open fun destroy() {
    }

    companion object {
        @JvmField
        val dns: Dns = Http.dns()
        @JvmStatic
        fun safeDns(): Dns {
            return Http.dns()
        }
    }

    fun client(): OkHttpClient {
        return Http.client()
    }

    val dns: Dns
        get() = Http.dns()
}