package com.corner.catvodcore.loader

import com.corner.catvod.enum.bean.Site
import com.corner.catvodcore.config.ApiConfig
import com.github.catvod.crawler.Spider
import com.github.catvod.crawler.SpiderNull
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap


object JsLoader {
    private val spiders = ConcurrentHashMap<String?, Spider>()
    private val jarLoader = JarLoader
    private var recent: String? = null

    fun clear() {
        for (spider in spiders.values) spider.destroy()
        jarLoader.clear()
        spiders.clear()
    }

    fun setRecent(recent: String?) {
        this.recent = recent
    }

    private fun loader(key: String?, jar: String?): URLClassLoader? {
        return try {
            if (jar == null || jar.isBlank()) null else jarLoader.getLoader(key!!, jar)
        } catch (e: Throwable) {
            null
        }
    }

    fun getSpider(key: String, api: String, ext: String, jar: String): Spider {
        try {
            if (spiders.containsKey(key)) return spiders[key]!!
            val spider: Spider = com.corner.quickjs.crawler.Spider(key, api, loader(key, jar))
            spider.init(ext)
            spiders[key] = spider
            return spider
        } catch (e: Throwable) {
            e.printStackTrace()
            return SpiderNull()
        }
    }

    private fun find(params: Map<String?, String?>): Spider? {
        if (!params.containsKey("siteKey")) return spiders[recent]
        val site: Site? = ApiConfig.getSite(params["siteKey"]!!)
        return if (site == null || site.isEmpty()) SpiderNull() else ApiConfig.getSpider(site)
    }

    fun proxyInvoke(params: Map<String?, String?>): Array<Any>? {
        try {
            return find(params)!!.proxyLocal(params)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }
}