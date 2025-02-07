@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
package com.github.catvod.net

import com.corner.catvodcore.util.Utils
import com.github.catvod.crawler.SpiderDebug
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.util.Map

internal class OkRequest private constructor(
    private val method: String,
    private var url: String,
    private val json: String?,
    private val params: Map<String, String>?,
    private val header: Map<String, String>?
) {
    private var request: Request? = null
    private var tag: Any? = null

    constructor(method: String, url: String, params: Map<String, String>?, header: Map<String, String>?) : this(
        method,
        url,
        null,
        params,
        header
    )

    constructor(method: String, url: String, json: String, header: Map<String, String>?) : this(
        method,
        url,
        json,
        HashMap<String, String>() as Map<String, String>,
        header
    )

    init {
        instance
    }

    fun tag(tag: Any?): OkRequest {
        this.tag = tag
        return this
    }

    private val instance: Unit
        get() {
            val builder = Request.Builder()
            if (method == OkHttp.GET) setParams()
            if (method == OkHttp.POST) builder.post(requestBody)
            header?.apply {
                for (key in header.keySet()) builder.addHeader(key, header[key]!!)
            }
            if (tag != null) builder.tag(tag)
            request = builder.url(url).build()
        }

    private val requestBody: RequestBody
        get() {
            if (!StringUtils.isEmpty(json)) return json!!.toRequestBody("application/json; charset=utf-8".toMediaType())
            val formBody = FormBody.Builder()
            if(params?.isEmpty == false){
                for (key in params.keySet()) formBody.add(key, params[key]!!)
            }
            return formBody.build()
        }

    private fun setParams() {
        url = "$url?"
        if(params?.isEmpty == false){
            for (key in params.keySet()) url = url + key + "=" + params[key] + "&"
        }
        url = Utils.substring(url) ?: ""
    }

    fun execute(client: OkHttpClient): OkResult {
        try {
            client.newCall(request!!).execute().use { response ->
                return OkResult(response.code, response.body.string(), response.headers.toMultimap())
            }
        } catch (e: IOException) {
            SpiderDebug.log("request fail path:" + e.message)
            return OkResult()
        }
    }
}
