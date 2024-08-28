package com.corner.quickjs.method

import com.corner.catvodcore.util.Trans
import com.corner.catvodcore.util.Urls
import com.corner.quickjs.bean.Req
import com.corner.quickjs.utils.Connect
import com.corner.quickjs.utils.Crypto
import com.corner.quickjs.utils.JSUtil
import com.corner.quickjs.utils.Parser
import com.corner.server.logic.getUrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.mozilla.javascript.Context
import org.mozilla.javascript.FunctionObject
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ExecutorService

class Global private constructor(ctx: Context, scope: ScriptableObject, executor: ExecutorService) {
    private val executor: ExecutorService
    private val ctx: Context
    private val jsObject: ScriptableObject
    private val parser: Parser
    private val timer: Timer

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        this.parser = Parser()
        this.executor = executor
        this.timer = Timer()
        this.ctx = ctx
        this.jsObject = scope
    }

    fun setProperty() {
        for (method in javaClass.methods) {
            if (!method.isAnnotationPresent(JSFunction::class.java)) continue
            jsObject.put(method.name, jsObject, FunctionObject(method.name, method, jsObject))
        }
    }

    private fun submit(runnable: Runnable) {
        if (!executor.isShutdown()) executor.submit(runnable)
    }

    @JSFunction
    fun s2t(text: String?): String {
        return Trans.s2t(false, text)
    }

    @JSFunction
    fun t2s(text: String?): String {
        return Trans.t2s(false, text)
    }

    @JSFunction
    fun getProxy(local: Boolean): String {
        return getUrl(local) + "?do=js"
    }

    @JSFunction
    fun js2Proxy(dynamic: Boolean?, siteType: Int, siteKey: String, url: String?, headers: ScriptableObject): String {
        return getProxy(!dynamic!!) + "&from=catvod" + "&siteType=" + siteType + "&siteKey=" + siteKey + "&header=" + URLEncoder.encode(
            headers.stringify()
        ) + "&url=" + URLEncoder.encode(url)
    }

    @JSFunction
    fun setTimeout(func: FunctionObject, delay: Int): Any? {
        func.hold()
        schedule(func, delay)
        return null
    }

    @JSFunction
    fun _http(url: String?, options: ScriptableObject): ScriptableObject? {
        val complete = options.getJSFunction("complete") ?: return req(url, options)
        val req: Req = Req.objectFrom(options.stringify())
        Connect.to(url, req).enqueue(getCallback(complete, req))
        return null
    }

    @JSFunction
    fun req(url: String?, options: ScriptableObject): ScriptableObject {
        try {
            val req: Req = Req.objectFrom(options.stringify())
            val res: Response = Connect.to(url, req).execute()
            return Connect.success(ctx, req, res)
        } catch (e: Exception) {
            return Connect.error(ctx)
        }
    }

    @JSFunction
    fun pd(html: String?, rule: String?, urlKey: String?): String {
        return parser.parseDomForUrl(html, rule, urlKey)
    }

    @JSFunction
    fun pdfh(html: String?, rule: String?): String {
        return parser.parseDomForUrl(html, rule, "")
    }


    @JSFunction
    fun pdfa(html: String?, rule: String?): JSArray {
        return JSUtil.toArray(ctx, parser.parseDomForArray(html, rule))
    }


    @JSFunction
    fun pdfl(html: String?, rule: String?, texts: String?, urls: String?, urlKey: String?): NativeArray {
        return JSUtil.toArray(ctx, parser.parseDomForList(html, rule, texts, urls, urlKey))
    }


    @JSFunction
    fun joinUrl(parent: String, child: String): String {
        return Urls.convert(parent, child)
    }


    @JSFunction
    @Throws(CharacterCodingException::class)
    fun gbkDecode(buffer: NativeArray?): String {
        val result: String = JSUtil.decodeTo("GB2312", buffer)
        log.debug("text:{}\nresult:\n{}", buffer, result)
        return result
    }


    @JSFunction
    fun md5X(text: String?): String {
        val result: String = Crypto.md5(text)
        log.debug("text:{}\nresult:\n{}", text, result)
        return result
    }


    @JSFunction
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


    @JSFunction
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

    private fun getCallback(complete: ScriptableObject, req: Req): Callback {
        return object : Callback {

            override fun onResponse(call: Call, res: Response) {
                submit {
                    FunctionObject.callMethod(complete, complete.className, arrayOf(Connect.success(ctx, req, res)))
//                    complete.call(Connect.success(ctx, req, res)) }
                }

                override fun onFailure(call: Call, e: IOException) {
                    submit { complete.call(Connect.error(ctx)) }
                }
            }
        }
    }

    private fun schedule(func: ScriptableObject, delay: Int) {
        timer.schedule(object : TimerTask() {
            override fun run() {
                submit { FunctionObject.callMethod()func.call() }
            }
        }, delay.toLong())
    }

    companion object {
        fun create(ctx: Context, scope: ScriptableObject, executor: ExecutorService): Global {
            return Global(ctx, scope, executor)
        }
    }
}
