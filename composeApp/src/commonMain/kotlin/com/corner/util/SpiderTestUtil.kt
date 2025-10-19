package com.corner.util

import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.catvodcore.bean.Site
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Jsons
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object SpiderTestUtil {
    private val log = thisLogger()
    private val spiderStatusMap = ConcurrentHashMap<String, SpiderStatus>()
    private val testJobs = ConcurrentHashMap<String, Job>()
    private var globalTestJob: Job? = null
    private val activeTestCount = AtomicInteger(0)

    private val _enableAdvancedMode = MutableStateFlow(false)
    val enableAdvancedMode: StateFlow<Boolean> = _enableAdvancedMode.asStateFlow()

    fun setEnableAdvancedMode(enabled: Boolean) {
        _enableAdvancedMode.value = enabled
    }

    enum class SpiderStatus {
        UNKNOWN, AVAILABLE, UNAVAILABLE, TESTING
    }

    suspend fun testSpider(siteKey: String, onStatusChange: (String, SpiderStatus) -> Unit) {
        testJobs[siteKey]?.cancel()
        activeTestCount.incrementAndGet()
        try {
            spiderStatusMap[siteKey] = SpiderStatus.TESTING
            onStatusChange(siteKey, SpiderStatus.TESTING)

            val site = ApiConfig.api.sites.find { it.key == siteKey }
            if (site == null || site.type != 3) {
                updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                return
            }

            withContext(Dispatchers.IO) {
                val job = currentCoroutineContext()[Job]!!
                try {
                    if (!enableAdvancedMode.value) {
                        testSpiderSimpleMode(siteKey, site, onStatusChange, job)
                    } else {
                        testSpiderAdvancedMode(siteKey, site, onStatusChange, job)
                    }
                } catch (e: Exception) {
                    if (!job.isCancelled) {
                        log.error("Test spider $siteKey error: ${e.message}", e)
                        updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                    }
                }
            }
        } finally {
            testJobs.remove(siteKey)
            activeTestCount.decrementAndGet()
        }
    }

    private suspend fun testSpiderSimpleMode(
        siteKey: String,
        site: Site,
        onStatusChange: (String, SpiderStatus) -> Unit,
        job: Job
    ) {
        val spider = ApiConfig.getSpider(site)
        if (spider != null) {
            try {
                // 默认模式：验证爬虫获取主页数据的能力，并验证数据有效性
                val homeContent = withTimeoutOrNull(5000) {
                    if (job.isCancelled) null else spider.homeContent(false)
                }

                if (!job.isCancelled) {
                    if (homeContent != null && homeContent.isNotBlank() && !isInvalidResponse(homeContent)) {
                        // homeContent 成功返回、内容不为空白且不包含反爬虫特征
                        // 现在需要验证返回的JSON是否包含有效的视频数据
                        if (validateJsonHasRealVodData(homeContent)) {
                            // 数据有效
                            updateSpiderStatus(siteKey, SpiderStatus.AVAILABLE, onStatusChange)
                        } else {
                            // 数据无效（例如空列表、格式不正确、无关键字段等）
                            updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                        }
                    } else {
                        // homeContent 为空、空白或包含反爬虫内容
                        updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                if (!job.isCancelled) {
                    log.warn("Test spider $siteKey timeout in simple mode")
                    // 超时也认为是不可用
                    updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                }
            } catch (e: Exception) {
                if (!job.isCancelled) {
                    log.debug("Test spider $siteKey homeContent failed in simple mode: ${e.message}")
                    // 其他异常也认为是不可用
                    updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                }
            }
        } else {
            if (!job.isCancelled) {
                updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
            }
        }
    }

    private suspend fun testSpiderAdvancedMode(
        siteKey: String,
        site: Site,
        onStatusChange: (String, SpiderStatus) -> Unit,
        job: Job
    ) {
        val spider = ApiConfig.getSpider(site)
        if (spider != null) {
            try {
                var searchContent: String? = null
                try {
                    // 获取用户设置的搜索词，如果没有设置则使用默认值"阿甘正传"
                    val searchKeyword = SettingStore.getSettingItem(SettingType.CRAWLER_SEARCH_TERMS).ifBlank { "阿甘正传" }
                    searchContent = withTimeoutOrNull(8000) {
                        if (job.isCancelled) null else spider.searchContent(searchKeyword, true)
                    }
                } catch (e: Exception) {
                    log.debug("Spider $siteKey searchContent failed: ${e.message}")
                }

                if (!job.isCancelled) {
                    if (searchContent != null) {
                        val isSearchValid = searchContent.isNotBlank() && !isInvalidResponse(searchContent)
                        if (isSearchValid) {
                            // 搜索成功，再验证JSON结构和数据有效性
                            if (validateJsonHasRealVodData(searchContent)) {
                                updateSpiderStatus(siteKey, SpiderStatus.AVAILABLE, onStatusChange)
                                return
                            } else {
                                // 搜索返回了内容，但JSON无效，可能是广告或其他格式
                                updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                                return
                            }
                        } else {
                            // 搜索返回了反爬虫内容
                            updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                            return
                        }
                    }

                    // 3. 如果搜索失败或无效，尝试 homeContent
                    var homeContent: String? = null
                    try {
                        homeContent = withTimeoutOrNull(5000) {
                            if (job.isCancelled) null else spider.homeContent(false)
                        }
                    } catch (e: Exception) {
                        log.debug("Spider $siteKey homeContent failed in advanced mode: ${e.message}")
                    }

                    if (homeContent != null) {
                        val isHomeValid = homeContent.isNotBlank() && !isInvalidResponse(homeContent)
                        if (isHomeValid && validateJsonHasRealVodData(homeContent)) {
                            updateSpiderStatus(siteKey, SpiderStatus.AVAILABLE, onStatusChange)
                            return
                        }
                    }

                    // 4. 如果搜索和home都失败或无效，回退到连接性测试
                    testSpiderHttpConnectivity(siteKey, site, onStatusChange, job)
                }
            } catch (e: TimeoutCancellationException) {
                if (!job.isCancelled) {
                    log.warn("Test spider $siteKey search timeout in advanced mode")
                    testSpiderHttpConnectivity(siteKey, site, onStatusChange, job)
                }
            }
        } else {
            if (!job.isCancelled) {
                updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
            }
        }
    }

    private suspend fun testSpiderHttpConnectivity(
        siteKey: String,
        site: Site,
        onStatusChange: (String, SpiderStatus) -> Unit,
        job: Job
    ) {
        if (job.isCancelled) return

        try {
            val testUrl = getTestUrl(site).trim()
            if (testUrl.isNotEmpty()) {
                val response = testUrlConnectivity(testUrl, job)
                if (response != null) {
                    val available = response.isSuccess && !isInvalidResponse(response.body)
                    updateSpiderStatus(siteKey, if (available) SpiderStatus.AVAILABLE else SpiderStatus.UNAVAILABLE, onStatusChange)
                } else {
                    updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
                }
            } else {
                // 如果无法获取测试URL，检查是否能初始化spider
                val canInitialize = withTimeoutOrNull(2000) {
                    if (job.isCancelled) false else ApiConfig.getSpider(site) != null
                } ?: false

                if (!job.isCancelled) {
                    updateSpiderStatus(siteKey, if (canInitialize) SpiderStatus.AVAILABLE else SpiderStatus.UNAVAILABLE, onStatusChange)
                }
            }
        } catch (e: Exception) {
            if (!job.isCancelled) {
                log.debug("HTTP connectivity test failed for $siteKey: ${e.message}")
                updateSpiderStatus(siteKey, SpiderStatus.UNAVAILABLE, onStatusChange)
            }
        }
    }

    private suspend fun testUrlConnectivity(url: String, job: Job): TestResponse? {
        var response: okhttp3.Response? = null
        try {
            response = withTimeoutOrNull(3000) {
                if (job.isCancelled) null else Http.client().newCall(Request.Builder().url(url).build()).execute()
            }

            if (response != null && response.isSuccessful) {
                val body = response.body?.string() ?: ""
                return TestResponse(true, body)
            } else {
                // 即使状态码不是2xx，也可能返回反爬虫页面，所以检查body
                val body = response?.body?.string() ?: ""
                if (isInvalidResponse(body)) {
                    return TestResponse(true, body) // Connection successful, but blocked
                }
                return if (response != null) TestResponse(false, "") else null
            }
        } finally {
            response?.close()
        }
    }

    data class TestResponse(val isSuccess: Boolean, val body: String)

    fun testAllSpiders(onStatusChange: (String, SpiderStatus) -> Unit) {
        cancelAllTests()
        val type3Sites = ApiConfig.api.sites.filter { it.type == 3 }
        if (type3Sites.isEmpty()) return

        globalTestJob = CoroutineScope(Dispatchers.IO).launch {
            for (site in type3Sites) {
                if (isActive) {
                    try {
                        testSpider(site.key, onStatusChange)
                        delay(100)
                    } catch (e: CancellationException) {
                        log.debug("Test cancelled for site: ${site.key}")
                        updateSpiderStatus(site.key, SpiderStatus.UNKNOWN, onStatusChange)
                    }
                }
            }
        }
    }

    fun cancelAllTests() {
        globalTestJob?.cancel()
        globalTestJob = null
        val activeJobs = testJobs.values.toList()
        testJobs.clear()
        activeJobs.forEach { it.cancel() }
        spiderStatusMap.replaceAll { _, status ->
            if (status == SpiderStatus.TESTING) SpiderStatus.UNKNOWN else status
        }
    }

    private fun updateSpiderStatus(siteKey: String, status: SpiderStatus, onStatusChange: (String, SpiderStatus) -> Unit) {
        spiderStatusMap[siteKey] = status
        onStatusChange(siteKey, status)
    }

    private fun getTestUrl(site: Site): String {
        if (site.api.isNotEmpty() && site.api.startsWith("http")) {
            return site.api
        }
        return when {
            site.name.contains("豆瓣") -> "https://movie.douban.com/j/search_subjects?type=movie&tag=热门&page_limit=1"
            site.name.contains("哔哩") || site.name.contains("B站") -> "https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword=test&page=1"
            site.name.contains("优酷") -> "https://www.youku.com"
            site.name.contains("爱奇艺") -> "https://www.iqiyi.com"
            site.name.contains("腾讯") -> "https://v.qq.com"
            else -> ""
        }
    }

    // 判断是否为无效响应：HTML、403/404、Cloudflare、人机验证、JS检测、特定反爬虫特征
    private fun isInvalidResponse(content: String): Boolean {
        val s = content.trim()
        if (s.isEmpty()) return true
        val lower = s.lowercase()

        // 检查是否为HTML
        if (s.startsWith("<")) {
            // 检查HTML内容中是否包含反爬虫特征
            return lower.contains("cloudflare") ||
                    lower.contains("checking your browser") ||
                    lower.contains("enable javascript") ||
                    lower.contains("security check") ||
                    lower.contains("just a moment") ||
                    lower.contains("access denied") ||
                    lower.contains("blocked") ||
                    lower.contains("navigator.platform") || // JS检测特征
                    lower.contains("skjlivqy_b") || // 日志中发现的特征
                    lower.contains("xdmi4s.com") || // 日志中发现的特征
                    lower.contains("dns劫持") // 日志中发现的中文特征
        }

        // 检查纯文本响应中的反爬虫关键词
        return lower.contains("403 forbidden") ||
                lower.contains("404 not found") ||
                lower.contains("cloudflare") ||
                lower.contains("checking your browser") ||
                lower.contains("enable javascript") ||
                lower.contains("security check") ||
                lower.contains("just a moment") ||
                lower.contains("access denied") ||
                lower.contains("blocked") ||
                lower.contains("navigator.platform") || // JS检测特征
                lower.contains("skjlivqy_b") || // 日志中发现的特征
                lower.contains("xdmi4s.com") || // 日志中发现的特征
                lower.contains("dns劫持") // 日志中发现的中文特征
    }


    // 验证 JSON 是否包含真实视频数据（至少一个 item 有 vod_id + vod_name）
    private fun validateJsonHasRealVodData(content: String): Boolean {
        if (content == "{}") return false
        return try {
            val json = Jsons.parseToJsonElement(content).jsonObject
            // 尝试从多个可能的键中获取列表
            val listArray = json["list"]?.jsonArray
                ?: json["data"]?.jsonObject?.get("list")?.jsonArray
                ?: json["data"]?.jsonArray
                ?: json["content"]?.jsonArray
                ?: json["data"]?.jsonObject?.get("content")?.jsonArray
                ?: json["data"]?.jsonObject?.get("data")?.jsonArray
                ?: return false // 如果没有找到列表，则无效

            if (listArray.isEmpty()) return false // 如果列表为空，则无效

            // 检查列表中的至少一个项目是否包含有效的 vod_id 和 vod_name
            listArray.any { item ->
                val obj = item.jsonObject
                val vodId = obj["vod_id"]?.jsonPrimitive?.content
                val vodName = obj["vod_name"]?.jsonPrimitive?.content
                // 确保 vod_id 和 vod_name 存在且不为空白
                !vodId.isNullOrBlank() && !vodName.isNullOrBlank()
            }
        } catch (e: Exception) {
            log.debug("Failed to validate JSON data: ${e.message}")
            false // 解析失败或结构不符合预期，则无效
        }
    }
}