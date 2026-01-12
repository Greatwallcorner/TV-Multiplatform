package com.corner.util.network

import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.bean.parseAsSettingEnable
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

class KtorClient {
    companion object {
        val client = HttpClient(OkHttp)
        private val log = LoggerFactory.getLogger(KtorClient::class.java)

        fun createHttpClient(block: HttpClientConfig<OkHttpConfig>.() -> Unit = {}) = HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                    proxy(getProxy())
                }
            }
            install(HttpRequestRetry) {
                maxRetries = 1
                delayMillis { 1000L }
            }
            block()
        }

        fun getProxy():Proxy{
            val settingEnable = SettingStore.getSettingItem(SettingType.PROXY).parseAsSettingEnable()
            if(!settingEnable.isEnabled) return Proxy.NO_PROXY
            val uri = try {
                URI.create(settingEnable.value)
            } catch (e: Exception) {
                log.error("解析代理url异常 不使用代理",e)
                return Proxy.NO_PROXY
            }
            var type:Proxy.Type = Proxy.Type.HTTP
            when(uri.scheme){
                "http"->type= Proxy.Type.HTTP
                "https"->type= Proxy.Type.HTTP
                "socks"->type= Proxy.Type.SOCKS
                "socks5"->type= Proxy.Type.SOCKS
            }
            return Proxy(type, InetSocketAddress(uri.host, uri.port))
        }
    }
}