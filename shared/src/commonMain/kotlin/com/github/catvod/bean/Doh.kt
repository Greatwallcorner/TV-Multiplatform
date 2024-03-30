package com.github.catvod.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetAddress

@Serializable
data class Doh(
    val name: String,
    val url: String
) {
    var ips: List<String>? = null
        get() = field ?: listOf()

    val hosts: List<InetAddress>?
        get() = try {
            val list: MutableList<InetAddress> = ArrayList<InetAddress>()
            for (ip in ips!!) list.add(InetAddress.getByName(ip))
            if (list.isEmpty()) null else list
        } catch (ignored: Exception) {
            null
        }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj !is Doh) return false
        return url == obj.url
    }

    override fun toString(): String {
        return Json.encodeToString(this)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }

    companion object {
        fun objectFrom(str: String): Doh {
            val item = Json.decodeFromString<Doh>(str)
            return item
        }

        fun defaultDoh(): List<Doh> {
            return listOf(
                Doh("System", ""),
                Doh("Tencent", "https://doh.pub/dns-query"),
                Doh("Alibaba", "https://dns.alidns.com/dns-query"),
                Doh("360", "https://doh.360.cn/dns-query")
            )
        }

//        fun arrayFrom(element: JsonElement?): List<Doh> {
//            val listType: Type = object : TypeToken<List<Doh?>?>() {}.getType()
//            val items: List<Doh> = Gson().fromJson(element, listType)
//            return items ?: ArrayList()
//        }
    }
}
