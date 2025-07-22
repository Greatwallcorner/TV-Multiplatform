package com.corner.catvodcore.config

import SiteViewModel
import com.corner.catvod.enum.bean.Rule
import com.corner.catvod.enum.bean.Site
import com.corner.catvodcore.bean.Api
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.loader.JarLoader
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Urls
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.database.Db
import com.corner.database.entity.Config
import com.corner.ui.scene.SnackBar
import com.corner.util.createCoroutineScope
import com.corner.util.isEmpty
import com.github.catvod.crawler.Spider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

private val log = LoggerFactory.getLogger("apiConfig")

object ApiConfig{
    private val scope = createCoroutineScope()
    var apiFlow = MutableStateFlow(Api(spider = ""))
    var api:Api = apiFlow.value

    init {
        collectApi()
    }

    private fun collectApi(){
        scope.launch {
            apiFlow.collect{api = it}
        }
    }

    fun clear(){
        apiFlow.value = Api(spider = "")
    }

    fun parseConfig(
        cfg: Config,
        isJson: Boolean,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ): Api {
        return try {
            log.info("parseConfig start cfg:{} isJson:{}", cfg, isJson)

            val data = getData(if (isJson) cfg.json ?: "" else cfg.url!!, isJson)
                ?: throw RuntimeException("配置读取异常")

            if (StringUtils.isBlank(data)) {
                log.warn("配置数据为空")
                SnackBar.postMsg("配置数据为空 请检查")
                setHome(null)
                throw RuntimeException("配置数据为空")
            }

            val apiConfig = Jsons.decodeFromString<Api>(data)
            apiFlow.update { apiConfig }
            apiFlow.update { ap ->
                ap.copy(url = cfg.url, data = data, cfg = cfg, ref = ap.ref + 1)
            }

            JarLoader.loadJar("", apiConfig.spider)

            if (cfg.home?.isNotBlank() == true) {
                setHome(api.sites.find { it.key == cfg.home })
            } else {
                setHome(api.sites.first())
            }

            scope.launch {
                api.sites = Db.Site.update(cfg, api)
            }

            log.info("parseConfig end")
            onSuccess()  // 成功回调
            apiConfig
        } catch (e: Exception) {
            log.error("parseConfig error", e)
            onError(e)  // 失败回调
            throw e  // 保持原有异常抛出行为
        }
    }

    fun setHome(home:Site?){
        GlobalAppState.home.value = home ?: Site.get("","")
    }

    fun getSpider(site: Site): Spider {
//    val js: Boolean = site.api.contains(".js")
//    val py: Boolean = site.api.contains(".py")
        val csp: Boolean = site.api.startsWith("csp_")
        return/* if (py) pyLoader.getSpider(
        site.key,
        site.api,
        site.ext
    ) else if (js) jsLoader.getSpider(
        site.key,
        site.api,
        site.ext,
        site.jar
    ) else*/ if (csp) JarLoader.getSpider(site.key, site.api, site.ext , site.jar ?: "") else Spider()
    }

    fun getSite(key:String): Site? {
        return api.sites.find { it.key == key }
    }

    fun setRecent(site: Site) {
        api.recent = site.key
        //注释js和py实现
//        val js: Boolean = site.api.contains(".js")
//        val py: Boolean = site.api.startsWith("py_")
        val csp: Boolean = site.api.startsWith("csp_")
        /*if (js) jsLoader.setRecent(site.getKey())
        else if (py) pyLoader.setRecent(site.getKey())
        else*/ if (csp) JarLoader.SetRecent(
            site.jar
        )
    }

    fun parseExt(ext: String): String {
        var currentExt = ext
        var attempts = 0
        val maxAttempts = 5

        while (attempts < maxAttempts) {
            when {
                StringUtils.isBlank(currentExt) -> return currentExt
                currentExt.startsWith("file") -> return Files.readString(Paths.get(Urls.convert(currentExt)))
                currentExt.endsWith(".js") || currentExt.endsWith(".json") || currentExt.endsWith(".txt") -> {
                    val newExt = Urls.convert(api.url ?: "", currentExt)
                    if (newExt == currentExt) return currentExt // 无变化时终止
                    currentExt = newExt
                }
                else -> return currentExt
            }
            attempts++
        }
        throw IllegalStateException("Failed to parseExt after $maxAttempts attempts")
    }

    fun parseApi(str: String): String {
        if (StringUtils.isBlank(str)) return ""
        if (str.startsWith("http") || str.startsWith("file")) return str
        if (str.endsWith(".js")) return parseApi(Urls.convert(api.url ?: "", str))
        return str
    }


    fun initProxy() {
        Http.setProxyHosts(getRuleByName("proxy")?.hosts)
    }

    fun getRuleByName(name: String): Rule? {
        return api.rules.find { i -> i.name == name }
    }

    private fun getData(str: String, isJson: Boolean): String? {
        try {
            if (StringUtils.isBlank(str)) {
                return ""
            }
            if (isJson) {
                return Jsons.decodeFromString(str)
            } else if (str.startsWith("http")) {
                return Http.Get(str,connectTimeout = 60,readTimeout = 60).execute().body.string()
            } else if (str.startsWith("file")) {
                val file = Urls.convert(str).toPath().toFile()
                if (!file.exists()) {
                    return ""
                }
                return Files.readString(file.toPath())
            }
        } catch (e: Exception) {
            SnackBar.postMsg("获取配置失败: "+e.message)
            log.error("获取配置失败", e)
            return ""
        }
        return ""
    }

}

fun Api.init() {
    ApiConfig.initProxy()
    initSite()
}

fun Api.initSite() {
    if (sites.isEmpty()) return
    for (site in sites) {
        site.api = ApiConfig.parseApi(site.api)
        site.ext = ApiConfig.parseExt(site.ext)
    }
    if (GlobalAppState.home.value.isEmpty() && sites.size > 0) {
        GlobalAppState.home.value = sites.first()
        SiteViewModel.viewModelScope.launch {
            Db.Config.setHome(url, ConfigType.SITE.ordinal, GlobalAppState.home.value.toString())
        }
    }
}