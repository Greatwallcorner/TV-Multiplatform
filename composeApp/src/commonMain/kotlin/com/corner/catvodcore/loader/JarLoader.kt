package com.corner.catvodcore.loader

import com.corner.catvodcore.Constant
import com.corner.catvodcore.config.ApiConfig
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Paths
import com.corner.catvodcore.util.Urls
import com.corner.catvodcore.util.Utils
import com.github.catvod.crawler.Spider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
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

    var recent: String? = null

    /**
     * CLEAR了个什么？？？？？
     * */
    fun clear() {
        spiders.values.forEach { spider ->
            CoroutineScope(Dispatchers.IO).launch {
                spider.destroy()
                spiders.clear()//???
            }
        }
        loaders.clear()
        methods.clear()
        recent = null
    }

    // 类级别变量
    private var loadJarStackDepth = 0
    private const val MAX_RETRY_COUNT = 30
    private const val STACK_OVERFLOW_THRESHOLD = 50 // 根据实际JVM栈深度调整

    /**
     * 这里有点懵，虽然限制了递归了深度，但是为什么能加载内容？为什么重试次数一定会满？
     * What The Fuck????
     * 但是不会出现栈溢出错误了，这绝对是最烂地修复！！！
     * */

    fun loadJar(key: String, spider: String, retryCount: Int = 0) {
        try {
            loadJarStackDepth++
            if (loadJarStackDepth > STACK_OVERFLOW_THRESHOLD) {
                resetLoadJarState()
                throw IllegalStateException("检测到堆栈溢出风险，已重置状态")
            }

            if(StringUtils.isBlank(spider)) return

            val texts = spider.split(Constant.md5Split)
            val md5 = if(texts.size<=1) "" else texts[1].trim()
            val jar = texts[0]

            when {
                md5.isNotEmpty() && Utils.equals(parseJarUrl(jar), md5) -> {
                    load(key, Paths.jar(parseJarUrl(jar)))
                }
                jar.startsWith("file") -> {
                    load(key, Paths.local(jar))
                }
                jar.startsWith("http") -> {
                    load(key, download(jar))
                }
                else -> {
                    val processedUrl = parseJarUrl(jar)
                    try {
                        if (processedUrl == jar) {
                            if (retryCount < MAX_RETRY_COUNT) {
                                log.warn("路径解析失败，尝试第${retryCount + 1}次重试: $jar")
                                loadJar(key, jar, retryCount + 1) // 重试原始路径
                                return
                            }
                            throw IllegalStateException("无法解析的路径格式: $jar (已尝试$MAX_RETRY_COUNT 次)")
                        }
                        loadJar(key, processedUrl, 0) // 重置重试计数器
                    } catch (e: Exception) {
                        log.error("""
                                        加载失败！
                                        原始路径: $jar
                                        解析后路径: $processedUrl
                                        重试次数: $retryCount/$MAX_RETRY_COUNT
                                        错误类型: ${e.javaClass.simpleName}
                        """.trimIndent(), e)
                        if (retryCount < MAX_RETRY_COUNT) {
                            Thread.sleep(1000) // 延迟1秒后重试
                            loadJar(key, spider, retryCount + 1)
                        } else {
                            throw e
                        }
                    }
                }
            }

        } finally {
            loadJarStackDepth--
        }
    }


    private fun resetLoadJarState() {
        loadJarStackDepth = 0
        log.error("重置loadJar状态，防止堆栈溢出")
    }

    /**
     * 如果在配置文件种使用的相对路径， 下载的时候使用的全路径 如果的判断md5是否一致的时候使用相对路径 就会造成重复下载
     */
    private fun parseJarUrl(jar: String): String {
        if (jar.startsWith("file") || jar.startsWith("http")) return jar
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
            val loader = loaders[jaKey] ?: throw IllegalStateException("Loader is null for JAR: $jar")
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
        log.debug("download jar file {} to:{}", jar, jarPath)
        return Paths.write(jarPath, Http.Get(jar).execute().body.bytes())
    }

    fun proxyInvoke(params: Map<String, String>): Array<Any>? {
        return try {
            val md5 = Utils.md5(recent ?: "")
            val proxy = methods[md5]
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
