import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.setVodFlags
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.Url
import com.corner.catvodcore.bean.add
import com.corner.catvodcore.bean.v
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.util.copyAdd
import com.corner.util.createCoroutineScope
import com.github.catvod.crawler.Spider
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import okhttp3.Call
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Response
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import com.corner.ui.nav.data.DialogState
import com.corner.ui.nav.data.DialogState.toggleSpecialVideoLink
import com.corner.util.M3U8AdFilterInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request

object SiteViewModel {
    private val log = LoggerFactory.getLogger("SiteViewModel")

    val result: MutableState<Result> by lazy { mutableStateOf(Result()) }
    val detail: MutableState<Result> by lazy { mutableStateOf(Result()) }
    val player: MutableState<Result> by lazy { mutableStateOf(Result()) }
    val search: MutableState<CopyOnWriteArrayList<Collect>> =
        mutableStateOf(CopyOnWriteArrayList(listOf(Collect.all())))
    val quickSearch: MutableState<CopyOnWriteArrayList<Collect>> =
        mutableStateOf(CopyOnWriteArrayList(listOf(Collect.all())))

    private val supervisorJob = SupervisorJob()
    val viewModelScope = createCoroutineScope(Dispatchers.IO)

    fun cancelAll() {
        supervisorJob.cancelChildren()
    }

    fun getSearchResultActive(): Collect {
        return search.value.first { it.activated.value }
    }

    fun homeContent(): Result {
        val site: Site = GlobalAppState.home.value
        result.value = Result()
        try {
            when (site.type) {
                3 -> {
                    val spider = ApiConfig.getSpider(site)
                    val homeContent = spider.homeContent(true)
//                    log.debug("home: $homeContent")
                    ApiConfig.setRecent(site)
                    val rst: Result = Jsons.decodeFromString<Result>(homeContent)
                    if (rst.list.size > 0) result.value = rst
                    val homeVideoContent = spider.homeVideoContent()
//                    log.debug("homeContent: $homeVideoContent")
                    rst.list.addAll(Jsons.decodeFromString<Result>(homeVideoContent).list)
                    result.value = rst.also { this.result.value = it }
                }

                4 -> {
                    val params: MutableMap<String, String> = mutableMapOf()
                    params["filter"] = "true"
                    val homeContent = call(site, params, false)
                    log.debug("home: $homeContent")
                    result.value = Jsons.decodeFromString<Result>(homeContent).also { this.result.value = it }
                }

                else -> {
                    //  use 确保 Response 正确关闭
                    val homeContent: String =
                        Http.newCall(site.api, site.header.toHeaders()).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            val body = response.body
                            body.string()
                        }
                    log.debug("home: $homeContent")
                    fetchPic(site, Jsons.decodeFromString<Result>(homeContent)).also { result.value = it }
                }
            }
        } catch (e: Exception) {
            log.error("home Content site:{}", site.name, e)
            return Result(false)
        }
        result.value.list.forEach { it.site = site }
        return result.value
    }

    fun detailContent(key: String, id: String): Result? {
        // 切换视频时重置浏览器选择标志位
        DialogState.resetBrowserChoice()
        toggleSpecialVideoLink(false)

        val site: Site = ApiConfig.api.sites.find { it.key == key } ?: return null
        var rst = Result()
        try {
            if (site.type == 3) {
                val spider: Spider = ApiConfig.getSpider(site)
                val detailContent = spider.detailContent(listOf(id))
//                log.debug("detailContent : detail:$detailContent")
                ApiConfig.setRecent(site)
                rst = Jsons.decodeFromString<Result>(detailContent)
                if (rst.list.isNotEmpty()) rst.list[0].setVodFlags()
                //            if (!rst.list.isEmpty()) checkThunder(rst.list.get(0).vodFlags())
                detail.value = rst
            } else if (site.key.isEmpty() && site.name.isEmpty() && key == "push_agent") {
                val vod = Vod()
                vod.vodId = id
                vod.vodName = id
                vod.vodPic = "https://pic.rmb.bdstatic.com/bjh/1d0b02d0f57f0a42201f92caba5107ed.jpeg"
                //            vod.vodFlags = (Flag.create(ResUtil.getString(R.string.push), ResUtil.getString(R.string.play), id))
                //            checkThunder(vod.getVodFlags())
                val rs = Result()
                rs.list = mutableListOf(vod)
                detail.value = rs
            } else {
                val params: MutableMap<String, String> =
                    mutableMapOf()
                params["ac"] = if (site.type == 0) "videolist" else "detail"
                params["ids"] = id
                val detailContent = call(site, params, true)
                log.debug("detail: $detailContent")
                rst = Jsons.decodeFromString<Result>(detailContent)
                if (rst.list.isNotEmpty()) rst.list[0].setVodFlags()
                //            if (!rst.list.isEmpty()) checkThunder(rst.list.get(0).getVodFlags())
                detail.value = rst
            }
        } catch (e: Exception) {
            log.error("${site.name} detailContent 异常", e)
            return null
        }
        rst.list.forEach { it.site = site }

        return rst
    }

    /**
     * 获取视频播放信息并处理播放链接
     * @param key 站点唯一标识
     * @param flag 播放标识（区分不同播放源）
     * @param id 视频唯一标识
     * @return Pair对象，第一个元素为Result对象，第二个元素表示是否为特殊链接
     */
    fun playerContent(key: String, flag: String, id: String): Result? {
        // 根据key获取站点信息，若站点不存在则返回null
        val site: Site = ApiConfig.getSite(key) ?: return null
        try {
            // 分支1：处理类型为3的站点（爬虫类型站点）
            if (site.type == 3) {
                // 获取该站点对应的爬虫实例
                val spider: Spider = ApiConfig.getSpider(site)
                // 通过爬虫获取播放内容
                val playerContent = spider.playerContent(flag, id, ApiConfig.api.flags.toList())
                // 记录最近使用的站点
                ApiConfig.setRecent(site)
                // 解析播放内容为Result对象
                val result = Jsons.decodeFromString<Result>(playerContent)
                // 保留原始播放标识（若存在）
                if (StringUtils.isNotBlank(result.flag)) result.flag = flag

                // 检测包含.m3u8但不以.m3u8结尾的链接（特殊链接处理）
                // 提取URL基础部分（去除查询参数和片段标识后的部分）
                val urlStr = result.url.v()
                val isSpecialLink = urlStr.isNotBlank() &&
                        !urlStr.contains("proxy") &&
                        urlStr.contains(".m3u8", ignoreCase = true) &&
                        !urlStr.trim().endsWith(".m3u8", ignoreCase = true)
                if (isSpecialLink) {
                    log.debug("检测到包含.m3u8的非M3U8链接，标记为特殊链接")
                    if (!DialogState.userChoseOpenInBrowser) {
                        DialogState.showPngDialog(urlStr) // 显示特殊链接对话框
                        toggleSpecialVideoLink(true) // 标记为特殊视频链接
                    } else {
                        toggleSpecialVideoLink(false) // 取消特殊视频链接标记
                    }
                } else {
                    // 处理标准M3U8链接（以.m3u8结尾、不含proxy、非空）
                    if (result.url.v().run {
                            endsWith(".m3u8") && !contains("proxy") && isNotBlank()
                        }) {
                        result.url = processM3U8(result.url) // 调用M3U8处理函数
                    }
                }
                // 设置结果头信息和站点key
                result.header = site.header
                result.key = key
                return result
            }
            // 分支2：处理类型为4的站点（参数请求类型站点）
            else if (site.type == 4) {
                // 构建播放请求参数
                val params = mutableMapOf<String, String>()
                params["play"] = id // 视频ID
                params["flag"] = flag // 播放标识
                // 调用call方法获取播放内容
                val playerContent = call(site, params, true)
                // 解析播放内容为Result对象
                val result = Jsons.decodeFromString<Result>(playerContent)
                // 保留原始播放标识（若存在）
                if (StringUtils.isNotBlank(result.flag)) result.flag = flag

                // 检测包含.m3u8但不以.m3u8结尾的链接（特殊链接处理）
                // 提取URL基础部分（去除查询参数和片段标识后的部分）
                val urlStr = result.url.v()
                val isSpecialLink = urlStr.isNotBlank() &&
                        !urlStr.contains("proxy") &&
                        urlStr.contains(".m3u8", ignoreCase = true) &&
                        !urlStr.trim().endsWith(".m3u8", ignoreCase = true)

                if (isSpecialLink) {
                    log.debug("检测到包含.m3u8的非M3U8链接，标记为特殊链接")
                    if (!DialogState.userChoseOpenInBrowser) {
                        DialogState.showPngDialog(urlStr) // 显示特殊链接对话框
                        toggleSpecialVideoLink(true) // 标记为特殊视频链接
                    } else {
                        toggleSpecialVideoLink(false) // 取消特殊视频链接标记
                    }
                } else {
                    // 处理标准M3U8链接（以.m3u8结尾、不含proxy、非空）
                    if (result.url.v().run {
                            endsWith(".m3u8") && !contains("proxy") && isNotBlank()
                        }) {
                        result.url = processM3U8(result.url) // 调用M3U8处理函数
                    }
                }
                // 设置结果头信息
                result.header = site.header
                return result
            }
            // 分支3：处理其他类型站点
            else {
                // 初始化播放链接（默认为id）
                var url: Url = Url().add(id)
                // 检查是否为JSON类型链接
                val type: String? = Url(id).parameters["type"]
                if (type != null && type == "json") {
                    // 若为JSON类型，请求并解析真实播放链接
                    val string = Http.newCall(id, site.header.toHeaders()).execute().body.string()
                    if (StringUtils.isNotBlank(string)) {
                        url = Jsons.decodeFromString<Result>(string).url
                    }
                }
                // 构建Result对象并设置基础信息
                val result = Result()
                result.url = url
                result.flag = flag

                // 检测包含.m3u8但不以.m3u8结尾的链接（特殊链接处理）
                // 提取URL基础部分（去除查询参数和片段标识后的部分）
                val urlStr = result.url.v()
                val isSpecialLink = urlStr.isNotBlank() &&
                        !urlStr.contains("proxy") &&
                        urlStr.contains(".m3u8", ignoreCase = true) &&
                        !urlStr.trim().endsWith(".m3u8", ignoreCase = true)

                if (isSpecialLink) {
                    log.debug("检测到包含.m3u8的非M3U8链接，标记为特殊链接")
                    if (!DialogState.userChoseOpenInBrowser) {
                        DialogState.showPngDialog(urlStr) // 显示特殊链接对话框
                        toggleSpecialVideoLink(true) // 标记为特殊视频链接
                    } else {
                        toggleSpecialVideoLink(false) // 取消特殊视频链接标记
                    }
                } else {
                    // 处理标准M3U8链接（以.m3u8结尾、不含proxy、非空）
                    if (result.url.v().run {
                            endsWith(".m3u8") && !contains("proxy") && isNotBlank()
                        }) {
                        result.url = processM3U8(result.url) // 调用M3U8处理函数
                    }
                }
                // 设置结果头信息、播放链接和解析标识
                result.header = site.header
                result.playUrl = site.playUrl
                result.parse = (if (StringUtils.isBlank(result.playUrl)) 0 else 1) // 0：无需解析，1：需要解析
                return result
            }
        } catch (e: Exception) {
            // 捕获异常并记录错误日志
            log.error("${site.name} player error:", e)
            return null
        }
    }


    /**
     * 处理M3U8文件，修正错误的扩展名、处理加密密钥、过滤广告链接并返回本地代理URL
     *
     * @param url 包含 M3U8 文件地址的 Url 对象
     * @return 处理后的本地代理 Url 对象，若处理失败则返回原始 Url 对象
     */
    private fun processM3U8(url: Url): Url {
        // 如果不是 .m3u8 文件，直接返回原始 Url 对象
        if (!url.v().endsWith(".m3u8", ignoreCase = true)) {
            return url
        }

        try {
            showProgress()
            // 定义请求 M3U8 文件时需要携带的请求头
            val header: Map<String, String> = mapOf(
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "Accept-Encoding" to "gzip, deflate, br, zstd",
                "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "DNT" to "1",
                "Origin" to "https://hhjx.hhplayer.com",
                "Priority" to "u=1, i",
                "Sec-Ch-Ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"",
                "Sec-Ch-Ua-Mobile" to "?0",
                "Sec-Ch-Ua-Platform" to "\"Windows\"",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "same-origin",
                "Sec-Fetch-Storage-Access" to "active",
                "Sec-Gpc" to "1",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0",
                "X-Requested-With" to "XMLHttpRequest"
            )
            val interceptor = M3U8AdFilterInterceptor.Interceptor()
            // 创建OkHttpClient并添加拦截器
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            // 使用拦截器处理请求
            val request = Request.Builder()
                .url(url.v())
                .headers(header.toHeaders())
                .build()

            val content = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("下载失败: ${response.code}")
                response.body.string()
            }

            // 0. 处理加密密钥（仅在存在密钥时处理）
            val processedKeyContent = if (content.contains("#EXT-X-KEY:")) {
                processEncryptionKeys(content, url.v())
            } else {
                content
            }

//            // 1. 处理.jpg链接（增强版正则）
//            val jpgRegex = Regex("""(https?://[^\s"']+?\.jpg)[^\s"']*(?=[\s"'>]|$)""", RegexOption.IGNORE_CASE)
//            val processedJpgContent = if (jpgRegex.containsMatchIn(processedKeyContent)) {
//                log.debug("检测到.jpg链接，执行替换...")
//                processedKeyContent.replace(jpgRegex) { match ->
//                    val newUrl = match.groupValues[1].replace(".jpg", ".ts", ignoreCase = true)
//                    log.debug("替换链接: ${match.value} → $newUrl")
//                    newUrl
//                }
//            } else {
//                log.debug("未检测到.jpg链接，跳过替换")
//                processedKeyContent
//            }

            // 2. 特殊链接检测（只匹配.png、.image和无后缀名链接）
            val pattern = Regex(
                // 匹配 http/https 协议
                "https?://" +
                        // 匹配域名部分（允许点号）
                        "[^\\s\"'/?#]+" +
                        // 匹配可选的路径部分（不包含点号）
                        "(?:/[^\\s\"'.?#]*)*" +
                        // 匹配三种情况：
                        // 1. 以.png结尾
                        // 2. 以.image结尾
                        // 3. 以.jpg结尾
                        // 4. 无后缀名（路径中无点号）
                        "(?:" +
                        "\\.(?:png|image|jpg)(?=[\\s\"'>]|$)" +  // 情况1和2
                        "|" +
                        "(?<!\\.)(?=[\\s\"'>]|$)" +         // 情况3：无后缀名（前面无点号）
                        ")",
                RegexOption.IGNORE_CASE
            )

            if (pattern.containsMatchIn(processedKeyContent)) {
                log.debug("process检测到特殊链接，弹出弹窗")
                if (!DialogState.userChoseOpenInBrowser) {
                    DialogState.showPngDialog(url.v())
                    toggleSpecialVideoLink(true)
                } else {
                    toggleSpecialVideoLink(false)
                }
                return url
            }

            // 3. 处理嵌套M3U8
            val processedContent = Regex("(?m)^(?!#).*\\.m3u8$").replace(processedKeyContent) { match ->
                val nestedUrl = match.value.let {
                    if (it.startsWith("http")) it else "${url.v().substringBeforeLast("/")}/$it"
                }
                processM3U8(Url().add(nestedUrl)).v() // 递归处理
            }

            // 缓存内容并返回代理URL
            val cacheId = M3U8Cache.put(processedContent)
            return Url().add("http://127.0.0.1:9978/proxy/cached_m3u8?id=$cacheId")
        } catch (e: Exception) {
            log.error("处理 M3U8 文件失败", e)
            return url
        } finally {
            hideProgress()
        }

    }

    /**
     * 处理M3U8中的加密密钥
     */
    private fun processEncryptionKeys(content: String, baseUrl: String): String {
        log.debug("开始处理密钥")
        val keyRegex = """#EXT-X-KEY:METHOD=([^,]+),URI="([^"]+)"(,IV=([^"]+))?""".toRegex()

        return keyRegex.replace(content) { match ->
            val (method, uri, _, iv) = match.destructured
            try {
                val keyUrl = when {
                    uri.startsWith("http") -> uri
                    uri.startsWith("/") -> {
                        // 修复点：正确提取协议和域名部分
                        val baseUri = java.net.URI(baseUrl)
                        "${baseUri.scheme}://${baseUri.host}$uri"
                    }

                    else -> {
                        // 处理相对路径
                        val basePath = baseUrl.substringBeforeLast("/")
                        "$basePath/$uri".replace(Regex("(?<!:)//"), "/")
                    }
                }
//                log.debug("处理出的密钥链接: $keyUrl")
                val cacheId = downloadAndStoreKey(keyUrl)

                "#EXT-X-KEY:METHOD=$method,URI=\"$cacheId\"" +
                        (if (iv.isNotEmpty()) ",IV=$iv" else "")
            } catch (e: Exception) {
                log.error("密钥处理失败，保留原始标签", e)
                match.value
            }
        }
    }

    /**
     * 下载并存储加密密钥
     */
    private fun downloadAndStoreKey(keyUrl: String): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(keyUrl).build()

        val keyData = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("密钥下载失败")
            response.body.bytes()
        }

        // 将密钥数据存入M3U8缓存
        val cacheId = M3U8Cache.put(String(keyData))

        // 返回通过本地服务器访问的缓存URL
        return "http://127.0.0.1:9978/proxy/cached_m3u8?id=$cacheId"
    }


    /**
     * 根据站点和关键词进行搜索操作，支持快速搜索模式
     *
     * @param site 搜索使用的站点信息
     * @param keyword 搜索的关键词
     * @param quick 是否为快速搜索模式
     */
    fun searchContent(site: Site, keyword: String, quick: Boolean) {
        try {
            // 检查站点类型是否为 3
            if (site.type == 3) {
                // 获取该站点对应的爬虫实例
                val spider: Spider = ApiConfig.getSpider(site)
                // 调用爬虫的搜索方法进行搜索
                val searchContent = spider.searchContent(keyword, quick)
                // 记录搜索日志，包含站点名称和搜索结果
                log.debug("search: " + site.name + "," + searchContent)
                // 将搜索结果字符串解析为 Result 对象
                val result = Jsons.decodeFromString<Result>(searchContent)
                // 将搜索结果进行后续处理，如展示到界面等
                post(site, result, quick)
            } else {
                // 非类型 3 的站点，构建搜索请求参数
                val params = mutableMapOf<String, String>()
                // 添加搜索关键词
                params["wd"] = keyword
                // 添加是否为快速搜索的标识
                params["quick"] = quick.toString()
                // 调用 call 方法发起搜索请求并获取结果
                val searchContent = call(site, params, true)
                // 记录搜索日志，包含站点名称和搜索结果
                log.debug(site.name + "," + searchContent)
                // 将搜索结果字符串解析为 Result 对象，并获取图片信息
                val result = Jsons.decodeFromString<Result>(searchContent)
                // 将搜索结果进行后续处理，如展示到界面等，同时获取图片信息
                post(site, fetchPic(site, result), quick)
            }
        } catch (e: Exception) {
            // 捕获搜索过程中发生的异常，并记录错误日志
            log.error("${site.name} search error", e)
        }
    }


    /**
     * 根据指定站点、关键词和页码进行搜索操作，并将搜索结果存储在 `result` 状态中。
     *
     * @param site 搜索使用的站点信息
     * @param keyword 搜索的关键词
     * @param page 搜索的页码
     */
    fun searchContent(site: Site, keyword: String, page: String) {
        try {
            // 检查站点类型是否为 3
            if (site.type == 3) {
                // 获取该站点对应的爬虫实例
                val spider: Spider = ApiConfig.getSpider(site)
                // 调用爬虫的搜索方法进行搜索，第三个参数 false 表示非快速搜索
                val searchContent = spider.searchContent(keyword, false, page)
                // 记录搜索日志，包含站点名称和搜索结果
                log.debug(site.name + "," + searchContent)
                // 将搜索结果字符串解析为 Result 对象
                val rst = Jsons.decodeFromString<Result>(searchContent)
                // 为搜索结果中的每个视频设置所属站点
                for (vod in rst.list) vod.site = site
                // 将解析后的搜索结果存储在 result 状态中
                result.value = rst
            } else {
                // 非类型 3 的站点，构建搜索请求参数
                val params = mutableMapOf<String, String>()
                // 添加搜索关键词
                params["wd"] = keyword
                // 添加搜索页码
                params["pg"] = page
                // 调用 call 方法发起搜索请求并获取结果
                val searchContent = call(site, params, true)
                // 记录搜索日志，包含站点名称和搜索结果
                log.debug(site.name + "," + searchContent)
                // 将搜索结果字符串解析为 Result 对象，并获取图片信息
                val rst: Result = fetchPic(site, Jsons.decodeFromString<Result>(searchContent))
                // 为搜索结果中的每个视频设置所属站点
                for (vod in rst.list) vod.site = site
                // 将解析后的搜索结果存储在 result 状态中
                result.value = rst
            }
        } catch (e: Exception) {
            // 捕获搜索过程中发生的异常，并记录错误日志
            log.error("${site.name} searchContent error", e)
        }
    }


    /**
     * 根据站点 key、分类 ID、页码、过滤标志和扩展参数获取分类内容
     *
     * @param key 站点的唯一标识
     * @param tid 分类的 ID
     * @param page 请求的页码
     * @param filter 是否启用过滤
     * @param extend 扩展参数，包含额外的请求信息
     * @return 包含分类内容的 Result 对象，若出错则返回表示失败的 Result 对象
     */
    fun categoryContent(
        key: String,
        tid: String,
        page: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): Result {
        // 记录方法调用时传入的参数信息，方便调试和监控
        log.info("categoryContent key:{} tid:{} page:{} filter:{} extend:{}", key, tid, page, filter, extend)
        // 根据站点 key 获取对应的站点信息，若未找到则返回表示失败的 Result 对象
        val site: Site = ApiConfig.getSite(key) ?: return Result(false)
        try {
            // 根据站点类型进行不同处理
            if (site.type == 3) {
                // 获取该站点对应的爬虫实例
                val spider: Spider = ApiConfig.getSpider(site)
                // 调用爬虫的分类内容获取方法
                val categoryContent = spider.categoryContent(tid, page, filter, extend)
                // 记录获取到的分类内容信息
                log.debug("cate: $categoryContent")
                // 将该站点标记为最近使用
                ApiConfig.setRecent(site)
                // 将获取到的分类内容字符串解析为 Result 对象并更新状态
                result.value = Jsons.decodeFromString<Result>(categoryContent)
            } else {
                // 非类型 3 的站点，构建请求参数
                val params = mutableMapOf<String, String>()
                // 根据站点类型和扩展参数添加不同的请求参数
                if (site.type == 1 && extend.isNotEmpty()) params.put("f", Jsons.encodeToString(extend))
                else if (site.type == 4) params.put("ext", Utils.base64(Jsons.encodeToString(extend)))
                // 根据站点类型设置操作类型参数
                params["ac"] = if (site.type == 0) "videolist" else "detail"
                // 添加分类 ID 参数
                params["t"] = tid
                // 添加页码参数
                params["pg"] = page
                // 调用 call 方法发起请求并获取响应内容
                val categoryContent = call(site, params, true)
                // 记录获取到的分类内容信息
                log.debug("cate: $categoryContent")
                // 将获取到的分类内容字符串解析为 Result 对象并更新状态
                result.value = Jsons.decodeFromString<Result>(categoryContent)
            }
        } catch (e: Exception) {
            // 捕获异常并记录错误信息
            log.error("${site.name} category error", e)
            // 发生异常时，将结果状态更新为失败
            result.value = Result(false)
        }
        // 为结果列表中的每个项设置所属站点信息
        result.value.list.forEach { it.site = site }
        // 返回最终的结果对象
        return result.value
    }


    private fun post(site: Site, result: Result, quick: Boolean) {
        if (site.name.isNotEmpty()) {
            SnackBar.postMsg("开始在${site.name}搜索", type = SnackBar.MessageType.INFO)
        }
        if (result.list.isEmpty()) {
            return
        }
        for (vod in result.list) vod.site = site
        if (quick) {
            search.value = quickSearch.value.copyAdd(Collect.create(result.list))
            if (quickSearch.value.size == 0) {
                search.value = quickSearch.value.copyAdd(Collect.all())
            }
            // 同样的数据添加到全部
            quickSearch.value[0].list.addAll(result.list)
        } else {
            search.value = search.value.copyAdd(Collect.create(result.list))
            if (search.value.size == 0) {
                search.value = search.value.copyAdd(Collect.all())
            }
            // 同样的数据添加到全部
            search.value[0].list.addAll(result.list)
        }
    }

    fun clearSearch() {
        search.value.clear()
        search.value.add(Collect.all())
    }

    fun clearQuickSearch() {
        quickSearch.value.clear()
        quickSearch.value.add(Collect.all())
    }
}


private fun call(site: Site, params: MutableMap<String, String>, limit: Boolean): String {
    val call: Call = if (fetchExt(site, params, limit).length <= 1000) {
        Http.newCall(site.api, site.header.toHeaders(), params)
    } else {
        Http.newCall(site.api, site.header.toHeaders(), Http.toBody(params))
    }

    return call.execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }

        val body = response.body

        body.string()
    }
}

private fun fetchExt(site: Site, params: MutableMap<String, String>, limit: Boolean): String {
    var extend: String = site.ext
    if (extend.startsWith("http")) extend = fetchExt(site)
    if (limit && extend.length > 1000) extend = extend.substring(0, 1000)
    if (extend.isNotEmpty()) params["extend"] = extend
    return extend
}

private fun fetchExt(site: Site): String {
    val res: Response = Http.newCall(site.ext, site.header.toHeaders()).execute()
    if (res.code != 200) return ""
    site.ext = res.body.string()
    return site.ext
}

private fun fetchPic(site: Site, result: Result): Result {
    if (result.list.isEmpty() || StringUtils.isNotBlank((result.list[0].vodPic))) return result
    val ids = ArrayList<String>()
    for (item in result.list) ids.add(item.vodId)
    val params: MutableMap<String, String> = mutableMapOf()
    params["ac"] = if (site.type == 0) "videolist" else "detail"
    params["ids"] = StringUtils.join(ids, ",")
    val response: String =
        Http.newCall(site.api, site.header.toHeaders(), params).execute().body.string()
    result.list.clear()
    result.list.addAll(Jsons.decodeFromString<Result>(response).list)
    return result
}
//
//@Throws(Exception::class)
//private fun checkThunder(flags: List<Flag>) {
//    for (flag in flags) {
//        val executor = java.util.concurrent.Executors.newFixedThreadPool(Constant.THREAD_POOL * 2)
//        for (future in executor.invokeAll(flag.getMagnet(), 30, TimeUnit.SECONDS)) flag.getEpisodes()
//            .addAll(future.get())
//        executor.shutdownNow()
//    }
//}

