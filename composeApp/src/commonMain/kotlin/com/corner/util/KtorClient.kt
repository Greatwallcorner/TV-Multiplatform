package com.corner.util

import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.bean.parseAsSettingEnable
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*

class KtorClient {
    companion object {
        val client = HttpClient(OkHttp)

        fun createHttpClient(block: HttpClientConfig<*>.() -> Unit = {}) = HttpClient(OkHttp) {
            val settingEnable = SettingStore.getSettingItem(SettingType.PROXY).parseAsSettingEnable()
            engine {
                config {
                    followRedirects(true)
                    proxy()
                }
            }
            install(HttpRequestRetry) {
                maxRetries = 1
                delayMillis { 1000L }
            }
            block()
        }
    }
}