package com.corner.quickjs.crawler

import com.corner.catvodcore.util.Asset
import com.corner.catvodcore.util.Json
import com.corner.catvodcore.util.Utils.decode
import com.corner.quickjs.bean.Res
import com.corner.quickjs.method.Async
import com.corner.quickjs.method.Global
import com.corner.quickjs.method.JsMethod
import com.corner.quickjs.method.Local
import com.corner.quickjs.utils.JSUtil
import com.corner.quickjs.utils.Module
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import netscape.javascript.JSObject
import org.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.*

class Spider(private val key: String, private val api: String, private val loader: URLClassLoader?) :
    com.github.catvod.crawler.Spider() {
    private val coroutine = CoroutineScope(Dispatchers.Default)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cat = false
    private var ctx: QuickJs? = null

    //    private var jsObject: ScriptableObject? = null
    private val log: Logger = LoggerFactory.getLogger(Spider::class.java)

    init {
        initializeJS()
//        ctx.f
    }

    private fun submit(runnable: Runnable) {
        executor.submit(runnable)
    }

    private fun <T> submit(callable: Callable<T>): Future<T> {
        return executor.submit(callable)
    }

    private fun call(func: String, vararg args: Any): Any {
        //return executor.submit((Function.call(ScriptableObject, func, args))).get();
        try {
            return CompletableFuture.supplyAsync(
                { Async.run(func, args as Array<Any>) }, executor
            ).join().get() ?: Any()
        } catch (e: Exception) {
            log.error("call 调用错误", e)
            return Any()
        }
    }

    override fun init(extend: String?) {
        try {
            if (cat) call("init", submit<JsObject> { cfg(extend) }.get())
            else call("init", (if (Json.valid(extend)) Json.toMap(extend) else extend)!!)
        } catch (e: Exception) {
            log.error("初始化错误", e)
            //            throw new RuntimeException(e);
        }
    }

    override fun homeContent(filter: Boolean): String {
        return call("home", filter) as String
    }

    override fun homeVideoContent(): String {
        return call("homeVod") as String
    }

    override fun categoryContent(tid: String, pg: String, filter: Boolean, extend: HashMap<String, String>): String {
        try {
            return call("category", tid, pg, filter, extend) as String
        } catch (e: Exception) {
            log.error("cate错误", e)
            //            throw new RuntimeException(e);
            return ""
        }
    }

    override fun detailContent(ids: List<String?>?): String {
        return call("detail", ids!![0]!!) as String
    }

    override fun searchContent(key: String?, quick: Boolean): String {
        return call("search", key!!, quick) as String
    }

    override fun searchContent(key: String?, quick: Boolean, pg: String?): String {
        return call("search", key!!, quick, pg!!) as String
    }

    override fun playerContent(flag: String?, id: String?, vipFlags: List<String?>?): String {
//        try {
//        } catch (e: Exception) {
//            log.error("playContent 错误", e)
//        }
        return call("play", flag!!, id!!, vipFlags!!) as String
    }

    override fun manualVideoCheck(): Boolean {
        return call("sniffer") as Boolean
    }

    override fun isVideoFormat(url: String?): Boolean {
        return call("isVideo", url!!) as Boolean
    }

    override fun proxyLocal(params: Map<String?, String?>?): Array<Any>? {
        try {
            return if ("catvod" == params!!["from"]) proxy2(params)
            else submit(Callable<Array<Any>?> { proxy1(params) }).get()
        } catch (e: Exception) {
            log.error("proxyLocal 错误", e)
        }
        return arrayOf()
    }

    override fun destroy() {
        submit {
            executor.shutdownNow()
//            ctx!!.destroy()
        }
    }

    @Throws(Exception::class)
    private fun initializeJS() {
        submit<Any?> {
            createCtx()
            createObj()
            null
        }.get()
    }

    private fun createCtx() {
        ctx = QuickJs.create(Dispatchers.Default)
        coroutine.launch {
            ctx!!.apply {
//            setConsole(Console())
                evaluate<Any>(Asset.read("js/lib/http.js"))
                val g = Global.create(ctx!!, executor)
                g.javaClass.methods.forEach {method ->
                    if(method.isAnnotationPresent(JsMethod::class.java)){
                        function(method.name){
                            method.invoke(g, it)
                        }
                    }
                }
//                Global.create(ctx!!, executor).setProperty()
                define<Local>("local", Local())
//            getGlobalObject().setProperty("local", Local::class.java)
//            setModuleLoader(object : BytecodeModuleLoader() {
//                override fun moduleNormalizeName(baseModuleName: String, moduleName: String): String {
//                    return convert(baseModuleName, moduleName)
//                }
//
//                override fun getModuleBytecode(moduleName: String): ByteArray {
//                    val content = Module.get().fetch(moduleName)
//                    return if (content.startsWith("//bb")) Module.get().bb(content) else compileModule(
//                        content,
//                        moduleName
//                    )
//                }
//            })
            }
        }
    }

    private fun createObj() {
        val spider = "__JS_SPIDER__"
        val global = "globalThis.$spider"
        val content: String = Module.get().fetch(api)
        val bb = content.startsWith("//bb")
        cat = bb || content.contains("__jsEvalReturn")
        runBlocking {
            quickJs {
                if (!bb) evaluate<Any>(content.replace(spider, global), api)
                evaluate<Any>(String.format(Asset.read("js/lib/spider.js"), api))
            }
        }
//        jsObject = ctx.getProperty(ctx.getGlobalObject(), spider) as JSObject
    }

    private fun cfg(ext: String?): JsObject {
        val map = mutableMapOf<String, Any?>()
        map["stype"] = 3
        map["skey"] = key
        if (Json.invalid(ext)) map["ext"] = ext
        else map["ext"] = ext
        return JsObject(map)
    }

    private fun proxy1(params: Map<String?, String?>): Array<Any> {
        val obj = params
        var res: Array<Any> = arrayOf()
//        val array = FunctionObject.callMethod(jsObject, "proxy", arrayOf(obj)) as Array<Any>
        runBlocking {
            ctx.run {
                res = call("proxy", params) as Array<Any>
            }
//            quickJs {
//                res = call("proxy", params) as Array<Any>
////                val array = JSONArray(res)
////                val headers = if (array.length() > 3) Json.toMap(array.optString(3)) else null
////                val base64 = array.length() > 4 && array.optInt(4) == 1
////                val result = arrayOf<Any>(
////                    array.optInt(0),
////                    array.optString(1),
////                    getStream(array.opt(2), base64),
////                    headers ?: mapOf<String, String>()
////                )
////                return result
//            }
//        }
//        val array =
//            JSONArray(Context.toString(FunctionObject.callMethod(jsObject, "proxy", arrayOf(obj)) as NativeArray))
//        val headers = if (array.length() > 3) Json.toMap(array.optString(3)) else null
//        val base64 = array.length() > 4 && array.optInt(4) == 1
//        val result = arrayOf<Any>(
//            array.optInt(0),
//            array.optString(1),
//            getStream(array.opt(2), base64),
//            headers ?: mapOf<String, String>()
//        )
        return res
    }

    @Throws(Exception::class)
    private fun proxy2(params: Map<String?, String?>?): Array<Any> {
        val url = params!!["url"]
        val header = params["header"]
        val array = listOf(*url!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
//        val array = submit<NativeArray> {
//            JSUtil.toArray(ctx, Arrays.asList(*url!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }
//                .toTypedArray()))
//        }.get()

        val obj = Json.toMap(header)
        val json = call("proxy", array, obj) as String
        val res = Res.objectFrom(json)
        val result = arrayOf<Any>(res.code, res.contentType, res.stream)
        return result
    }

    private fun getStream(o: Any, base64: Boolean): ByteArrayInputStream {
        if (o is JSONArray) {
            val a = o
            val bytes = ByteArray(a.length())
            for (i in 0 until a.length()) bytes[i] = a.optInt(i).toByte()
            return ByteArrayInputStream(bytes)
        } else {
            var content = o.toString()
            if (base64 && content.contains("base64,")) content =
                content.split("base64,".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1]
            return ByteArrayInputStream(if (base64) decode(content) else content.toByteArray())
        }
    }

}
