package com.github.catvod.net

import io.ktor.client.plugins.*
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.IOException


class OkhttpInterceptor : okhttp3.Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val response = chain.proceed(getRequest(chain))
        val encoding = response.header("Content-Encoding")
        if (encoding == null || (encoding != "deflate")) return response
        val `is` = java.util.zip.InflaterInputStream(response.body.byteStream(), java.util.zip.Inflater(true))
        return response.newBuilder().headers(response.headers).body(object : ResponseBody() {
            override fun contentType(): okhttp3.MediaType? {
                return response.body.contentType()
            }

            override fun contentLength(): Long {
                return response.body.contentLength()
            }

            override fun source(): BufferedSource {
                return `is`.source().buffer()
            }
        }).build()
    }

    private fun getRequest(chain: okhttp3.Interceptor.Chain): okhttp3.Request {
        val request = chain.request()
//        if (request.url.host == "gitcode.net") return request.newBuilder().addHeader("User-Agent", UserAgent.)
//            .build()
        return request
    }
}
