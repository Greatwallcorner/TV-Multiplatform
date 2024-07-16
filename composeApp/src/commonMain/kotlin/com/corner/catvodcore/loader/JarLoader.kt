package com.corner.catvodcore.loader

import com.corner.catvodcore.Constant
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Paths
import com.corner.catvodcore.util.Urls
import com.corner.catvodcore.util.Utils
import com.github.catvod.crawler.Spider
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

object JarLoader {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val loaders: ConcurrentHashMap<String, URLClassLoader> by lazy { ConcurrentHashMap() }

    //proxy method
    private val methods: ConcurrentHashMap<String, Method> by lazy { ConcurrentHashMap() }
    private val spiders: ConcurrentHashMap<String, Spider> by lazy { ConcurrentHashMap() }

    var recent:String? = null;

    fun clear(){
        loaders.clear()
        methods.clear()
        spiders.clear()
        recent = null
    }

    fun loadJar(key: String, spider: String) {
        val texts = spider.split(Constant.md5Split)
        val md5 = if(texts.size<=1) "" else texts[1].trim()
        val jar = texts[0]

        // 可以避免重复下载
        if(md5.isNotEmpty() && Utils.equals(parseJarUrl(jar), md5)){
            load(key, Paths.jar(parseJarUrl(jar)))
        }else if (jar.startsWith("file")) {
            load(key, Paths.local(jar))
        } else if (jar.startsWith("http")) {
            load(key, download(jar))
        } else {
             loadJar(key, Urls.convert(ApiConfig.api.url!!, jar))
        }

    }

    /**
     * 如果在配置文件种使用的相对路径， 下载的时候使用的全路径 如果的判断md5是否一致的时候使用相对路径 就会造成重复下载
     */
    private fun parseJarUrl(jar: String): String {
        if(jar.startsWith("file") || jar.startsWith("http")) return jar
        return Urls.convert(ApiConfig.api.url!!, jar)
    }

    private fun load(key: String, jar: File) {
        log.debug("load jar {}", jar)
        loaders[key] =  URLClassLoader(arrayOf(jar.toURI().toURL()),this.javaClass.classLoader)
        putProxy(key)
        invokeInit(key)
    }

    private fun putProxy(key: String) {
        try {
            val clazz = loaders[key]?.loadClass(Constant.catVodProxy)
            val method = clazz!!.getMethod("proxy", Map::class.java)
            methods[key] = method
        } catch (e: Exception) {
//            e.printStackTrace()
        }
    }

    private fun invokeInit(key: String) {
        try {
            val clazz = loaders[key]?.loadClass(Constant.catVodInit)
            val method = clazz?.getMethod("init")
            method?.invoke(clazz)
        } catch (e: Exception) {
//            e.printStackTrace()
        }
    }

    fun getSpider(key: String, api: String, ext: String, jar: String): Spider {
        try {
            val jaKey = Utils.md5(jar)
            val spKey = jaKey + key
            if (spiders.containsKey(spKey)) return spiders[spKey]!!
            if (loaders[jaKey] == null) loadJar(jaKey, jar)
            val loader = loaders[jaKey]
            val classPath = "${Constant.catVodSpider}.${api.replace("csp_", "")}"
            val spider: Spider =
                loader!!.loadClass(classPath).getDeclaredConstructor()
                    .newInstance() as Spider
            spider.init(ext)
            spiders[spKey] = spider
            return spider
        } catch (e: Exception) {
            e.printStackTrace()
            return Spider()
        }
    }

    private fun download(jar: String): File {
        val jarPath = Paths.jar(jar)
        log.debug("download jar file {} to:{}",jar, jarPath)
        return Paths.write(jarPath, Http.Get(jar).execute().body.bytes())
    }

    fun proxyInvoke(params: Map<String, String>): Array<Any>? {
        return try {
            val md5 = Utils.md5(recent ?: "")
            val proxy = methods.get(md5)
            proxy?.invoke(null, params) as Array<Any>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun SetRecent(jar: String?) {
         recent = jar
    }
}
