package com.github.catvod.crawler

import com.corner.catvodcore.util.Http
import okhttp3.Dns
import okhttp3.OkHttpClient


interface Spider1 {
    val dns: Dns
        get() = Http.dns()

    //abstract class Spider {
    @Throws(Exception::class)
    fun init()

    @Throws(Exception::class)
    open fun init(extend: String?) {
        init()
    }

    @Throws(Exception::class)
    fun homeContent(filter: Boolean): String

    @Throws(Exception::class)
    fun homeVideoContent(): String

    @Throws(Exception::class)
    fun categoryContent(tid: String?, pg: String?, filter: Boolean, extend: HashMap<String?, String?>?): String

    @Throws(Exception::class)
    fun detailContent(ids: List<String?>?): String

    @Throws(Exception::class)
    fun searchContent(key: String?, quick: Boolean): String

    @Throws(Exception::class)
    fun searchContent(key: String?, quick: Boolean, pg: String?): String

    @Throws(Exception::class)
    fun playerContent(flag: String?, id: String?, vipFlags: List<String?>?): String

    @Throws(Exception::class)
    fun manualVideoCheck(): Boolean

    @Throws(Exception::class)
    open fun isVideoFormat(url: String?): Boolean {
        return false
    }

    @Throws(Exception::class)
    fun proxyLocal(params: Map<String?, String?>?): Array<Any>?

    fun destroy()

    companion object {
        @JvmField
        val dns:Dns = Http.dns()
        @JvmStatic
        fun safeDns(): Dns {
            return Http.dns()
        }
    }

    fun client(): OkHttpClient {
        return Http.client()
    }

}
