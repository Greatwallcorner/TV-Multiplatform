package com.corner.catvodcore.util

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import com.seiko.imageloader.component.fetcher.FetchResult
import com.seiko.imageloader.component.fetcher.Fetcher
import com.seiko.imageloader.option.Options
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.json.jsonObject
import kotlin.time.Duration


class KtorHeaderUrlFetcher private constructor(
    private val httpUrl: String,
    httpClient: () -> HttpClient,
) : Fetcher {

    private val httpClient by lazy(httpClient)

    override suspend fun fetch(): FetchResult {
            var url = httpUrl.toString()
            val response = httpClient.request {
                headers {
                    if (url.contains("@Headers=")) {
                        appendAll(StringValues.build {
                            val elements = Jsons.parseToJsonElement(url.split("@Headers=").apply { url = this[0] }[1].split("@")[0])
                            for (jsonElement in elements.jsonObject) {
                                append(jsonElement.key, jsonElement.value.toString())
                            }
                        })
                    }
                    if (url.contains("@Cookie=")) append(HttpHeaders.Cookie, url.split("@Cookie=").apply { url = this[0] }[1].split("@")[0])
                    if (url.contains("@Referer=")) append(HttpHeaders.Referrer, url.split("@Referer=").apply { url = this[0] }[1].split("@")[0])
                    if (url.contains("@User-Agent=")) append(HttpHeaders.UserAgent, url.split("@User-Agent=").apply { url = this[0] }[1].split("@")[0])

                }
                url(url)
            }
            if (response.status.isSuccess()) {
                val ofSource = FetchResult.OfPainter(
                    painter = BitmapPainter(loadImageBitmap(response.bodyAsChannel().toInputStream()))
                )
                return ofSource
            }
            throw RuntimeException("code:${response.status.value}, ${response.status.description}")
    }


    class Factory(
        private val httpClient: () -> HttpClient,
    ) : Fetcher.Factory {
        override fun create(data: Any, options: Options): Fetcher? {
            if (data is String) return KtorHeaderUrlFetcher(data, httpClient)
            return null
        }
    }

    companion object {

        val CustomUrlFetcher = Factory {
            HttpClient(OkHttp){
                engine{
                    config {
                        callTimeout(Duration.parse("2s"))
                        readTimeout(Duration.parse("2s"))
                    }
                }
            }
        }
    }
}
