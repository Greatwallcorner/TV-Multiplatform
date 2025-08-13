package com.corner.util

import M3U8Filter
import com.corner.bean.SettingStore
import okhttp3.ResponseBody.Companion.toResponseBody
import org.slf4j.LoggerFactory
import java.net.URI


private val log = LoggerFactory.getLogger("M3U8AdFilterInterceptor")

class M3U8AdFilterInterceptor {
    class Interceptor() : okhttp3.Interceptor {

        private val config = SettingStore.getM3U8FilterConfig() // 主动获取最新配置
        private val filter = M3U8Filter(config)

        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            val url = request.url.toString()

            // 只拦截m3u8请求
            if (!url.endsWith(".m3u8", ignoreCase = true)) {
                return chain.proceed(request)
            }
            log.info("拦截请求，URL: $url")

            val response = chain.proceed(request)
            if (!response.isSuccessful) return response

            val originalContent = response.body?.string() ?: return response

            // 1. 转换相对路径
            val baseUrl = url.substringBeforeLast("/") + "/"
            val absolutePathContent = originalContent.lines().joinToString("\n") { line ->
                when {
                    line.startsWith("#") || line.isBlank() -> line
                    line.startsWith("http") -> line
                    line.startsWith("/") -> URI(baseUrl).resolve(line).toString()
                    else -> "$baseUrl$line"
                }
            }

            val filteredContent = if (SettingStore.isAdFilterEnabled()) {
                // 2. 广告过滤处理
                filter.safelyProcessM3u8(url, absolutePathContent)
            } else {
                absolutePathContent
            }

            val adCount = filter.getFilteredAdCount()
            if (adCount > 0) {
                log.info("广告过滤完成，共过滤 $adCount 条广告")
                SnackBar.postMsg("广告过滤完成，共过滤 $adCount 条广告")
            }

            // 3. 直接返回处理后的内容（不再走代理）
            return response.newBuilder()
                .body(filteredContent.toResponseBody(response.body?.contentType()))
                .build()
        }
    }
}