package com.corner.catvodcore.util

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import com.seiko.imageloader.component.fetcher.FetchResult
import com.seiko.imageloader.component.fetcher.Fetcher
import com.seiko.imageloader.option.Options
import io.ktor.http.*
import kotlinx.serialization.json.jsonObject
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request


class KtorHeaderUrlFetcher private constructor(
    private val httpUrl: String,
    httpClient: () -> OkHttpClient,
) : Fetcher {

    private val httpClient by lazy(httpClient)

    override suspend fun fetch(): FetchResult {
        val url = httpUrl.toString()
        val headers = mutableMapOf<String, String>()
            headers.run {
            if (url.contains("@Headers=")) {
                val elements = Jsons.parseToJsonElement(url.split("@Headers=")[1].split("@")[0])
                for (jsonElement in elements.jsonObject) {
                    put(jsonElement.key, jsonElement.value.toString())
                }
            }
            if (url.contains("@Cookie=")) put(HttpHeaders.Cookie, url.split("@Cookie=")[1].split("@")[0])
            if (url.contains("@Referer=")) put(HttpHeaders.Referrer, url.split("@Referer=")[1].split("@")[0])
            if (url.contains("@User-Agent=")) put(HttpHeaders.UserAgent, url.split("@User-Agent=")[1].split("@")[0])
        }


        val reqeust = Request.Builder().url(url.split("@")[0]).headers(headers.toHeaders()).build()
        val response = httpClient.newCall(reqeust).execute()
        if (response.isSuccessful) {
            val ofSource = FetchResult.OfPainter(
                painter = BitmapPainter(loadImageBitmap(response.body.byteStream()))
            )
//            val ofSource = FetchResult.OfSource(
//                source = response.bodyAsChannel().toInputStream().source().buffer(),
//                extra = extraData {
//                    mimeType(response.contentType()?.toString())
//                },
//            )
            return ofSource
        }
        throw RuntimeException("code:${response.code}, ${HttpStatusCode.fromValue(response.code)}")
    }

    class Factory(
        private val httpClient: () -> OkHttpClient,
    ) : Fetcher.Factory {
        override fun create(data: Any, options: Options): Fetcher? {
            if (data is String) return KtorHeaderUrlFetcher(data, httpClient)
            return null
        }
    }

    companion object {
        val defaultHttpEngineFactory: () -> OkHttpClient
            get() = { Http.client() }

        val CustomUrlFetcher = Factory {
            Http.client()
        }
    }
}
