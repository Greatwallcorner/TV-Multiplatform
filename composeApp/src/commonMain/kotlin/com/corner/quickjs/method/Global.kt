package com.corner.quickjs.method

import cn.hutool.core.net.URLEncodeUtil
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Trans
import com.corner.catvodcore.util.Urls
import com.corner.quickjs.bean.Req
import com.corner.quickjs.utils.Connect
import com.corner.quickjs.utils.Crypto
import com.corner.quickjs.utils.JSUtil
import com.corner.quickjs.utils.Parser
import com.corner.server.logic.getUrl
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.JsFunction
import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.evaluate
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService

class Global private constructor(
    private val ctx: QuickJs,
    private val executor: ExecutorService
) {
    private val parser: Parser = Parser()
    private val timer: Timer = Timer()

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

//    fun setProperty() {
//        for (method in javaClass.methods) {
//            if (!method.isAnnotationPresent(JSFunction::class.java)) continue
//            jsObject.put(method.name, jsObject, FunctionObject(method.name, method, jsObject))
//        }
//    }

    private fun submit(runnable: Runnable) {
        if (!executor.isShutdown) executor.submit(runnable)
    }


    @JsMethod
    fun s2t(text: String?): String {
        return Trans.s2t(false, text)
    }

    @JsMethod
    fun t2s(text: String?): String {
        return Trans.t2s(false, text)
    }

    @JsMethod
    fun getProxy(local: Boolean): String {
        return getUrl(local) + "?do=js"
    }

    @JsMethod
    fun js2Proxy(dynamic: Boolean?, siteType: Int, siteKey: String, url: String?, headers: JsObject): String {
        return getProxy(!dynamic!!) + "&from=catvod" + "&siteType=" + siteType + "&siteKey=" + siteKey + "&header=" + URLEncodeUtil.encode(
            (Jsons.encodeToString(headers)) + "&url=" + URLEncodeUtil.encode(url)
        )
    }

    @JsMethod
    fun setTimeout(func: JsFunction, delay: Int): Any? {
//        func.hold()
        schedule(func, delay)
        return null
    }

    @JsMethod
    fun _http(url: String?, options: JsObject): JsObject? {
        if (options.contains("complete")) {
            val funComplete = options["complete"]
            val req: Req = Req.objectFrom(Jsons.encodeToString(options))
            Connect.to(url, req).enqueue(getCallback(funComplete as JsFunction, req))
        } else {
            return req(url, options)
        }
//        val complete = options.getJSFunction("complete") ?: return req(url, options)
//        val req: Req = Req.objectFrom(options.stringify())
//        Connect.to(url, req).enqueue(getCallback(complete, req))
        return null
    }

    @JsMethod
    fun req(url: String?, options: JsObject): JsObject {
        try {
            val req: Req = Req.objectFrom(Jsons.encodeToString(options))
            val res: Response = Connect.to(url, req).execute()
            return Connect.success(req, res)
        } catch (e: Exception) {
            return Connect.error()
        }
    }

    @JsMethod
    fun pd(html: String?, rule: String?, urlKey: String?): String {
        return parser.parseDomForUrl(html, rule, urlKey)
    }

    @JsMethod
    fun pdfh(html: String?, rule: String?): String {
        return parser.parseDomForUrl(html, rule, "")
    }

    @JsMethod
    fun pdfa(html: String?, rule: String?): List<Any> {
        return parser.parseDomForArray(html, rule)
    }

    @JsMethod
    fun pdfl(html: String?, rule: String?, texts: String?, urls: String?, urlKey: String?): List<String> {
        return parser.parseDomForList(html, rule, texts, urls, urlKey)
    }

    @JsMethod
    fun joinUrl(parent: String, child: String): String {
        return Urls.convert(parent, child)
    }

    @JsMethod
    @Throws(CharacterCodingException::class)
    fun gbkDecode(buffer: ArrayList<Any>?): String {
        val result: String = JSUtil.decodeTo("GB2312", buffer)
        log.debug("text:{}\nresult:\n{}", buffer, result)
        return result
    }

    @JsMethod
    fun md5X(text: String?): String {
        val result: String = Crypto.md5(text)
        log.debug("text:{}\nresult:\n{}", text, result)
        return result
    }

    @JsMethod
    fun aesX(
        mode: String?,
        encrypt: Boolean,
        input: String?,
        inBase64: Boolean,
        key: String?,
        iv: String?,
        outBase64: Boolean
    ): String {
        val result: String = Crypto.aes(mode, encrypt, input, inBase64, key, iv, outBase64)
        log.debug(
            "mode:{}\nencrypt:{}\ninBase64:{}\noutBase64:{}\nkey:{}\niv:{}\ninput:\n{}\nresult:\n{}",
            mode,
            encrypt,
            inBase64,
            outBase64,
            key,
            iv,
            input,
            result
        )
        return result
    }
    @JsMethod
    fun rsaX(
        mode: String?,
        pub: Boolean,
        encrypt: Boolean,
        input: String?,
        inBase64: Boolean,
        key: String?,
        outBase64: Boolean
    ): String {
        val result: String = Crypto.rsa(mode, pub, encrypt, input, inBase64, key, outBase64)
        log.debug(
            "mode:{}\npub:{}\nencrypt:{}\ninBase64:{}\noutBase64:{}\nkey:\n{}\ninput:\n{}\nresult:\n{}",
            mode,
            pub,
            encrypt,
            inBase64,
            outBase64,
            key,
            input,
            result
        )
        return result
    }

    private fun getCallback(complete: JsFunction, req: Req): Callback {
        return object : Callback {

            override fun onResponse(cal: Call, res: Response) {

                submit {
                    runBlocking {
                        ctx.run {

                        }
                    }
//                    ctx.
//                    (complete as FunctionObject).call(
//                        ctx,
//                        jsObject,
//                        complete,
//                        arrayOf(Connect.success(ctx, jsObject, req, res))
//                    )
//                    FunctionObject.callMethod(complete, complete.className, arrayOf(Connect.success(ctx, req, res)))
//                    complete.call(Connect.success(ctx, req, res)) }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                submit {
                    (complete as FunctionObject).call(
                        ctx,
                        jsObject,
                        complete,
                        arrayOf(Connect.error(ctx, jsObject))
                    )
                }
            }
        }
    }

    private fun schedule(func: ScriptableObject, delay: Int) {
        timer.schedule(object : TimerTask() {
            override fun run() {
                submit { (func as FunctionObject).call(ctx, jsObject, func, arrayOf()) }
            }
        }, delay.toLong())
    }

    companion object {
        fun create(ctx: QuickJs, executor: ExecutorService): Global {
            return Global(ctx, executor)
        }
    }
}