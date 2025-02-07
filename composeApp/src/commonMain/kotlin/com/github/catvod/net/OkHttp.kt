@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
package com.github.catvod.net

import com.corner.util.KtorClient.Companion.getProxy
import com.github.catvod.crawler.Spider.Companion.safeDns
import com.github.catvod.crawler.SpiderDebug
import com.github.catvod.crawler.SpiderDebug.log
import okhttp3.*
import java.io.IOException
import java.net.Proxy
import java.time.Duration
import java.util.Map
import java.util.concurrent.TimeUnit

object OkHttp {
    private var client: OkHttpClient? = null
    private val shortTimeOutClient: OkHttpClient? = null
    private val proxy: Proxy? = null

    const val POST: String = "POST"
    const val GET: String = "GET"

    @JvmStatic
    fun get(): OkHttp {
        return this
    }

    @JvmStatic
    fun dns(): Dns {
        return safeDns()
    }

    @JvmStatic
    fun client(): OkHttpClient {
        if (get().client != null) return get().client!!
        return builder.build().also { get().client = it }
    }

    fun clearClient() {
        get().client = null
    }

    @JvmStatic
    fun shortTimeoutClient(): OkHttpClient? {
        if (get().client != null) return get().shortTimeOutClient
        return builder.callTimeout(Duration.ofSeconds(2L)).build().also { get().client = it }
    }

    @JvmStatic
    fun noRedirect(): OkHttpClient {
        return client().newBuilder().followRedirects(false).followSslRedirects(false).build()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newCall(request: Request): Response {
        return client().newCall(request).execute()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newCall(url: String): Response {
        return client().newCall(Request.Builder().url(url).build()).execute()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun newCall(url: String, header: Map<String, String>): Response {
        return client().newCall(Request.Builder().url(url).headers(header.getHeaders()).build()).execute()
    }

    @JvmStatic
    private fun Map<String, String>.getHeaders():Headers{
        val b = Headers.Builder()
        this.entrySet().forEach{e->
            run {
                b.add(e.key, e.value)
            }
        }
        return b.build()
    }

    @JvmStatic
    fun string(url: String): String {
        log("proxy string http")
        return string(url, HashMap<String, String>() as Map<String, String>)
    }

    @JvmStatic
    fun string(url: String, header: Map<String, String>?): String {
        SpiderDebug.log("proxy http string:$url")
        return string(client(), url, HashMap<String, String>() as Map<String, String>, header)
    }

    @JvmStatic
    fun string(url: String, param: Map<String, String>, header: Map<String, String>): String {
        return string(client(), url, param, header)
    }

    @JvmStatic
    fun string(client: OkHttpClient, url: String, header: Map<String, String>): String {
        return string(client, url, HashMap<String, String>() as Map<String, String>, header)
    }

    @JvmStatic
    fun string(
        client: OkHttpClient,
        url: String,
        params: Map<String, String>,
        header: Map<String, String>?
    ): String {
        return if (url.startsWith("http")) OkRequest(GET, url, params, header).execute(client).body else ""
    }

    @JvmStatic
    fun post(url: String, params: Map<String, String>): String {
        return post(client(), url, params, HashMap<String, String>() as Map<String, String>).body
    }

    @JvmStatic
    fun post(url: String, params: Map<String, String>, header: Map<String, String>): OkResult {
        return post(client(), url, params, header)
    }

    @JvmStatic
    fun post(
        client: OkHttpClient,
        url: String,
        params: Map<String, String>,
        header: Map<String, String>
    ): OkResult {
        return OkRequest(POST, url, params, header).execute(client)
    }

    @JvmStatic
    fun post(url: String, json: String = "{}"): String {
        return post(url, json, HashMap<String, String>() as Map<String, String>).body
    }

    @JvmStatic
    fun post(url: String, json: String = "{}", header: Map<String, String>?): OkResult {
        return post(client(), url, json, header)
    }

    @JvmStatic
    fun post(client: OkHttpClient, url: String, json: String = "{}", header: Map<String, String>?): OkResult {
        return OkRequest(POST, url, json, header).execute(client)
    }

    @JvmStatic
    fun get(url: String, params: Map<String, String>?, header: Map<String, String>?): OkResult {
        return OkRequest(GET, url, params, header).execute(client())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getLocation(url: String, header: Map<String, String>): String? {
        return getLocation(
            noRedirect().newCall(Request.Builder().url(url).headers(header.getHeaders()).build()).execute().headers.getHeaderMap() as Map<String, List<String>>
        )
    }

    @JvmStatic
    private fun Headers.getHeaderMap(): HashMap<String, List<String>> {
        val map = HashMap<String, List<String>>()
        this.map { p -> map.put(p.first, listOf(p.second)) }
        return map
    }

    @JvmStatic
    fun getLocation(headers: Map<String, List<String>>): String? {
        if (headers.containsKey("location")) return headers["location"]!![0]
        return null
    }

    @JvmStatic
    val builder: OkHttpClient.Builder
        get() = OkHttpClient.Builder()
            .proxy(getProxy())
            .addInterceptor(OkhttpInterceptor()).dns(dns())
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .sslSocketFactory(SSLSocketClient.sSLSocketFactory, SSLSocketClient.x509TrustManager)
            .hostnameVerifier((SSLSocketClient.hostnameVerifier))

}
