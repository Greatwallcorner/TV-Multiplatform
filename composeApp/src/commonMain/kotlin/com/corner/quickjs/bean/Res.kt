package com.corner.quickjs.bean

import com.corner.catvodcore.util.Json
import com.corner.catvodcore.util.Utils.decode
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.apache.commons.lang3.StringUtils
import java.io.ByteArrayInputStream
import java.util.*

class Res {
    @SerializedName("code")
    val code: Int = 200

    @SerializedName("buffer")
    val buffer: Int = 0

    @SerializedName("content")
    val content: String? = null
        get() = if (StringUtils.isEmpty(field)) "" else field

    @SerializedName("headers")
    private val headers: JsonElement? = null

    val header: Map<String, String>
        get() = Json.toMap(headers)

    val contentType: String
        get() {
            val header = header
            val keys: List<String> = mutableListOf("Content-Type", "content-type")
            for (key in keys) if (header.containsKey(key)) return header[key]!!
            return "application/octet-stream"
        }

    val stream: ByteArrayInputStream
        get() {
            if (buffer == 2) return ByteArrayInputStream(decode(content))
            return ByteArrayInputStream(content!!.toByteArray())
        }

    companion object {
        fun objectFrom(json: String?): Res {
            return Gson().fromJson(json, Res::class.java)
        }
    }
}
