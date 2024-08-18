package com.corner.quickjs.bean

import com.corner.catvodcore.util.Json
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import java.util.*

class Req {
    @SerializedName("buffer")
    val buffer: Int? = null
        get() = field ?: 0

    @SerializedName("redirect")
    val redirect: Int? = null
        get() = field ?: 1

    @SerializedName("timeout")
    val timeout: Int? = null
        get() = field ?: 10000

    @SerializedName("postType")
    val postType: String? = null
        get() = if (StringUtils.isEmpty(field)) "json" else field

    @SerializedName("method")
    val method: String? = null
        get() = if (StringUtils.isEmpty(field)) "get" else field

    @SerializedName("body")
    val body: String? = null

    @SerializedName("data")
    val data: JsonElement? = null

    @SerializedName("headers")
    private val headers: JsonElement? = null

    fun isRedirect(): Boolean {
        return redirect == 1
    }

    val header: Map<String, String>
        get() = Json.toMap(headers)

    val charset: String
        get() {
            val header = header
            val keys: List<String> = mutableListOf("Content-Type", "content-type")
            for (key in keys) if (header.containsKey(key)) return getCharset(
                Objects.requireNonNull(
                    header[key]
                )
            )
            return "UTF-8"
        }

    private fun getCharset(value: String?): String {
        for (text in value!!.split(";".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()) if (text.contains("charset=")) return text.split("=".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]
        return "UTF-8"
    }

    companion object {
        fun objectFrom(json: String?): Req {
            return Gson().fromJson(json, Req::class.java)
        }
    }
}
