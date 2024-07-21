import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvod.enum.bean.Vod.Companion.setVodFlags
import com.corner.catvodcore.bean.Collect
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.Url
import com.corner.catvodcore.bean.add
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalModel
import com.github.catvod.crawler.Spider
import com.github.catvod.crawler.SpiderDebug
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.serialization.encodeToString
import okhttp3.Headers.Companion.toHeaders
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

object SiteViewModel {
    private val log = LoggerFactory.getLogger("SiteViewModel")
//    val episode: MutableState<Episode?> = mutableStateOf<Episode?>(null)
    val result: MutableState<Result> by lazy {  mutableStateOf(Result())}
    val detail: MutableState<Result> by lazy {  mutableStateOf(Result())}
    val player: MutableState<Result> by lazy {  mutableStateOf(Result())}
    val search: MutableState<CopyOnWriteArrayList<Collect>> = mutableStateOf(CopyOnWriteArrayList(listOf(Collect.all())))
    val quickSearch: MutableState<CopyOnWriteArrayList<Collect>> = mutableStateOf(CopyOnWriteArrayList(listOf(Collect.all())))

    private val supervisorJob = SupervisorJob()
    val viewModelScope = CoroutineScope(Dispatchers.IO + supervisorJob)

    fun cancelAll(){
        supervisorJob.cancelChildren()
    }

    fun getSearchResultActive(): Collect {
        return search.value.first { it.isActivated().value }
    }

    fun homeContent(): Result {
        val site: Site = GlobalModel.home.value
        result.value = Result()
        try {
            when (site.type) {
                3 -> {
                    val spider = ApiConfig.getSpider(site)
                    val homeContent = spider.homeContent(true)
                    log.debug("home:$homeContent")
                    ApiConfig.setRecent(site)
                    val rst: Result = Jsons.decodeFromString<Result>(homeContent)
                    if ((rst.list.size) > 0) result.value = rst
                    val homeVideoContent = spider.homeVideoContent()
                    log.debug("homeContent: $homeVideoContent")
                    rst.list.addAll(Jsons.decodeFromString<Result>(homeVideoContent).list)
                    result.value = rst.also { this.result.value = it }
                }

                4 -> {
                    val params: MutableMap<String, String> =
                        mutableMapOf()
                    params["filter"] = "true"
                    val homeContent = call(site, params, false)
                    log.debug("home:$homeContent")
                    result.value = Jsons.decodeFromString<Result>(homeContent).also { this.result.value = it }
                }
                else -> {
                    val homeContent: String =
                        Http.newCall(site.api, site.header.toHeaders()).execute().body.string()
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
        val site: Site = ApiConfig.api.sites.find { it.key == key } ?: return null
        var rst = Result()
        try {
            if (site.type == 3) {
                val spider: Spider = ApiConfig.getSpider(site)
                val detailContent = spider.detailContent(listOf(id))
                log.debug("detail:$detailContent")
                ApiConfig.setRecent(site)
                rst = Jsons.decodeFromString<Result>(detailContent)
                if (rst.list.isNotEmpty()) rst.list.get(0).setVodFlags()
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
                params.put("ac", if (site.type == 0) "videolist" else "detail")
                params.put("ids", id)
                val detailContent = call(site, params, true)
                log.debug("detail: $detailContent")
                val rs = Jsons.decodeFromString<Result>(detailContent)
                if (rs.list.isNotEmpty()) rs.list[0].setVodFlags()
                //            if (!rst.list.isEmpty()) checkThunder(rst.list.get(0).getVodFlags())
                detail.value = rs
            }
        } catch (e: Exception) {
            log.error("${site.name} detailContent 异常", e)
            return null
        }
        rst.list.forEach { it.site = site }

        return rst
    }

    fun playerContent(key: String, flag: String, id: String): Result? {
//        Source.get().stop()
        val site: Site = ApiConfig.getSite(key) ?: return null
        try {
            if (site.type == 3) {
                val spider: Spider = ApiConfig.getSpider(site)
                val playerContent = spider.playerContent(flag, id, ApiConfig.api.flags.toList())
                log.debug("player:$playerContent")
                ApiConfig.setRecent(site)
                val result = Jsons.decodeFromString<Result>(playerContent)
                if (StringUtils.isNotBlank(result.flag)) result.flag = flag
                //            result.setUrl(Source.get().fetch(result))
                //            result.url.replace() = result.url.v()
                result.header = site.header
                result.key = key
                return result
            } else if (site.type == 4) {
                val params = mutableMapOf<String, String>()
                params.put("play", id)
                params.put("flag", flag)
                val playerContent = call(site, params, true)
                log.debug("player: $playerContent")
                val result = Jsons.decodeFromString<Result>(playerContent)
                if (StringUtils.isNotBlank(result.flag)) result.flag = flag
                //            result.setUrl(Source.get().fetch(result))
                result.header = site.header
                return result
            } /*else if (site.isEmpty() && key == "push_agent") {
                val result = Result<Any>()
                result.setParse(0)
                result.setFlag(flag)
                result.setUrl(Url.create().add(id))
                result.setUrl(Source.get().fetch(result))
                return result
            }*/ else {
                var url: Url = Url().add(id)
                val type: String? = Url(id).parameters.get("type")
                if (type != null && type == "json") {
                    val string = Http.newCall(id, site.header.toHeaders()).execute().body.string()
                    if (StringUtils.isNotBlank(string)) {
                        url = Jsons.decodeFromString<Result>(string).url
                    }
                }
                val result = Result()
                result.url = url
                result.flag = flag
                result.header = site.header
                result.playUrl = site.playUrl
                result.parse =
                    (if (/*Sniffer.isVideoFormat(url.v())*//* && */StringUtils.isBlank(result.playUrl)) 0 else 1)
                //            result.setParse(if (Sniffer.isVideoFormat(url.v()) && result.getPlayUrl().isEmpty()) 0 else 1)
                return result
            }
        } catch (e: Exception) {
            log.error("${site.name} player error:", e)
            return null
        }
    }


    fun searchContent(site: Site, keyword: String, quick: Boolean) {
        try {
            if (site.type == 3) {
                val spider: Spider = ApiConfig.getSpider(site)
                val searchContent = spider.searchContent(keyword, quick)
                log.debug("search: "+ site.name + "," + searchContent)
                val result = Jsons.decodeFromString<Result>(searchContent)
                post(site, result, quick)
            } else {
                val params = mutableMapOf<String, String>()
                params.put("wd", keyword)
                params.put("quick", quick.toString())
                val searchContent = call(site, params, true)
                log.debug(site.name + "," + searchContent)
                val result = Jsons.decodeFromString<Result>(searchContent)
                post(site, fetchPic(site, result), quick)
            }
        } catch (e: Exception) {
            log.error("${site.name} search error", e)
        }
    }

    fun searchContent(site: Site, keyword: String, page: String) {
        try {
            if (site.type == 3) {
                val spider: Spider = ApiConfig.getSpider(site)
                val searchContent = spider.searchContent(keyword, false, page)
                log.debug(site.name + "," + searchContent)
                val rst = Jsons.decodeFromString<Result>(searchContent)
                for (vod in rst.list) vod.site = site
                result.value = rst
            } else {
                val params = mutableMapOf<String, String>()
                params.put("wd", keyword)
                params.put("pg", page)
                val searchContent = call(site, params, true)
                log.debug(site.name + "," + searchContent)
                val rst: Result = fetchPic(site, Jsons.decodeFromString<Result>(searchContent))
                for (vod in rst.list) vod.site = site
                result.value = rst
            }
        } catch (e: Exception) {
            log.error("${site.name} searchContent error", e)
        }
    }

    fun categoryContent(key: String, tid: String, page: String, filter: Boolean, extend: HashMap<String, String>):Result{
        log.info("categoryContent key:{} tid:{} page:{} filter:{} extend:{}", key, tid, page, filter, extend)
        val site: Site = ApiConfig.getSite(key) ?: return Result(false)
         try {
            if (site.type == 3) {
                val spider: Spider = ApiConfig.getSpider(site)
                val categoryContent = spider.categoryContent(tid, page, filter, extend)
                log.debug("cate: $categoryContent")
                ApiConfig.setRecent(site)
                result.value = Jsons.decodeFromString<Result>(categoryContent)
            } else {
                val params = mutableMapOf<String, String>()
                if (site.type == 1 && extend.isNotEmpty()) params.put("f", Jsons.encodeToString(extend))
                else if (site.type == 4) params.put("ext", Utils.base64(Jsons.encodeToString(extend)))
                params["ac"] = if (site.type == 0) "videolist" else "detail"
                params["t"] = tid
                params["pg"] = page
                val categoryContent = call(site, params, true)
                log.debug("cate: $categoryContent")
                result.value = Jsons.decodeFromString<Result>(categoryContent)
            }
        } catch (e: Exception) {
            log.error("${site.name} category error", e)
             result.value = Result(false)
        }
        result.value.list.forEach{it.site = site}
        return result.value
    }


    private fun post(site: Site, result: Result, quick: Boolean) {
        if (result.list.isEmpty()) return
        for (vod in result.list) vod.site = site
        if (quick) {
            quickSearch.value.add(Collect.create(result.list))
            if(quickSearch.value.size == 0){
                quickSearch.value.add(Collect.all())
            }
            // 同样的数据添加到全部
            quickSearch.value.get(0).getList().addAll(result.list)
        } else {
            search.value.add(Collect.create(result.list))
            if(search.value.size == 0){
                search.value.add(Collect.all())
            }
            // 同样的数据添加到全部
            search.value.get(0).getList().addAll(result.list)
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
    val call: okhttp3.Call =
        if (fetchExt(site, params, limit).length <= 1000) Http.newCall(
            site.api,
            site.header.toHeaders(),
            params
        ) else Http.newCall(site.api, site.header.toHeaders(), Http.toBody(params))
    return call.execute().body.string()
}

private fun fetchExt(site: Site, params: MutableMap<String, String>, limit: Boolean): String {
    var extend: String = site.ext
    if (extend.startsWith("http")) extend = fetchExt(site)
    if (limit && extend.length > 1000) extend = extend.substring(0, 1000)
    if (extend.isNotEmpty()) params["extend"] = extend
    return extend
}

private fun fetchExt(site: Site): String {
    val res: okhttp3.Response = Http.newCall(site.ext, site.header.toHeaders()).execute()
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

