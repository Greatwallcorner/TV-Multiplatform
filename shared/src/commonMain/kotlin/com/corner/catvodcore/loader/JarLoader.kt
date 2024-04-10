package com.corner.catvodcore.loader

import com.github.catvod.crawler.Spider
import com.corner.catvodcore.Constant
import com.corner.catvodcore.config.api
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Paths
import com.corner.catvodcore.util.Urls
import com.corner.catvodcore.util.Utils
import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

/**
@author heatdesert
@date 2023-11-30 22:41
@description
 */
object JarLoader {
    private val loaders: ConcurrentHashMap<String, URLClassLoader> by lazy { ConcurrentHashMap() }

    //proxy method
    private val methods: ConcurrentHashMap<String, Method> by lazy { ConcurrentHashMap() }
    private val spiders: ConcurrentHashMap<String, Spider> by lazy { ConcurrentHashMap() }

    var recent:String? = null;
    fun loadJar(key: String, spider: String) {
        val texts = spider.split(Constant.md5Split)
//        val md5 = if(texts.size<=1) "" else texts[1].trim()
        val jar = texts[0]

        if(Paths.jar(jar).exists()){
            load(key, Paths.jar(jar))
        }else if (jar.startsWith("file")) {
            load(key, Paths.local(jar))
        } else if (jar.startsWith("http")) {
            load(key, download(jar))
        } else {
             loadJar(key, Urls.convert(api!!.url!!, spider))
        }

    }

    private fun load(key: String, jar: File) {
        loaders[key] =  URLClassLoader(arrayOf(jar.toURI().toURL()),this.javaClass.classLoader)
        invokeInit(key)
        putProxy(key)
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
            if (spiders.contains(spKey)) return spiders[spKey]!!
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
        return Paths.write(Paths.jar(jar), Http.Get(jar).execute().body.bytes())
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
