package com.corner.catvodcore.config

import com.corner.catvod.enum.bean.Api
import com.corner.catvod.enum.bean.Rule
import com.corner.catvod.enum.bean.Site
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.loader.JarLoader
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Urls
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.database.Config
import com.corner.database.Db
import com.corner.ui.scene.SnackBar
import com.corner.util.isEmpty
import com.github.catvod.crawler.Spider
import okio.Path.Companion.toPath
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files

var api: Api? = null

private val log = LoggerFactory.getLogger("apiConfig")


fun parseConfig(cfg: Config, isJson: Boolean): Api? {
    log.info("parseConfig start cfg:{} isJson:{}", cfg, isJson)
    val data = getData(if (isJson) cfg.json ?: "" else cfg.url!!, isJson) ?: throw RuntimeException("配置读取异常")
    if(StringUtils.isBlank(data)) {
        setHome(null)
        return api
    }
    val apiConfig = Jsons.decodeFromString<Api>(data)
    api = apiConfig
    api?.url = cfg.url
    api?.data = data
    api?.cfg?.value = cfg
    JarLoader.loadJar("", api!!.spider)
    if(cfg.home?.isNotBlank() == true) {
        setHome(api?.sites?.find { it.key == cfg.home })
    }else{
        setHome(api?.sites?.first())
    }
    log.info("parseConfig end")
    return apiConfig
}

fun setHome(home:Site?){
    GlobalModel.home.value = home ?: Site.get("","")
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
    val sites = api?.sites ?: listOf()
    return sites.find { it.key == key }
}

fun setRecent(site: Site) {
    api?.recent = site.key
    val js: Boolean = site.api.contains(".js")
    val py: Boolean = site.api.startsWith("py_")
    val csp: Boolean = site.api.startsWith("csp_")
    /*if (js) jsLoader.setRecent(site.getKey())
    else if (py) pyLoader.setRecent(site.getKey())
    else*/ if (csp) JarLoader.SetRecent(
        site.jar
    )
}

fun Api.init() {
    initProxy()
    initSite()
    updateConfig()
}

fun updateConfig() {
//     Db.Config.updateSome(api.site.value.id
}

fun Api.initSite() {
    if (sites == null) return
    for (site in sites) {
        site.api = parseApi(site.api)
        site.ext = parseExt(site.ext)
    }
    if (GlobalModel.home.value.isEmpty() && sites.size > 0) {
        GlobalModel.home.value = sites.first()
        Db.Config.setHome(url, ConfigType.SITE.ordinal, GlobalModel.home.value.toString())
    }
}

fun parseExt(ext: String): String {
    if (StringUtils.isBlank(ext)) return ext
    if (ext.startsWith("http") || ext.startsWith("file")) return ext
    if (ext.endsWith(".js") || ext.endsWith(".json") || ext.endsWith(".txt")) parseExt(
        Urls.convert(
            api?.url ?: "",
            ext
        )
    )
    return ext
}

fun parseApi(str: String): String {
    if (StringUtils.isBlank(str)) return ""
    if (str.startsWith("http") || str.startsWith("file")) return str
    if (str.endsWith(".js")) return parseApi(Urls.convert(api?.url ?: "", str))
    return str
}


fun initProxy() {
    Http.setProxyHosts(getRuleByName("proxy")?.hosts)
}

fun getRuleByName(name: String): Rule? {
    return api?.rules?.find { i -> i.name == name }
}

private fun getData(str: String, isJson: Boolean): String? {
    try {
        if (StringUtils.isBlank(str)) {
            return ""
        }
        if (isJson) {
            return Jsons.decodeFromString(str)
        } else if (str.startsWith("http")) {
            return Http.Get(str).execute().body.string()
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

fun proxyLocal() {

}