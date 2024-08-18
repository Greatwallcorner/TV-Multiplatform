package com.corner.quickjs.method

import com.corner.catvodcore.util.Trans
import com.corner.catvodcore.util.Urls
import com.corner.quickjs.bean.Req
import com.corner.quickjs.utils.Connect
import com.corner.quickjs.utils.Crypto
import com.corner.quickjs.utils.JSUtil
import com.corner.quickjs.utils.Parser
import com.corner.server.logic.getUrl
import com.whl.quickjs.wrapper.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ExecutorService

class Global private constructor(ctx: QuickJSContext, executor: ExecutorService) {
    private val executor: ExecutorService
    private val ctx: QuickJSContext
    private val parser: Parser
    private val timer: Timer
    
    private val log:Logger = LoggerFactory.getLogger(this::class.java)

    init {
        this.parser = Parser()
        this.executor = executor
        this.timer = Timer()
        this.ctx = ctx
    }

    fun setProperty() {
        for (method in javaClass.methods) {
            if (!method.isAnnotationPresent(JSMethod::class.java)) continue
            ctx.getGlobalObject().setProperty(method.name) { args: Array<Any?> ->
                try {
                    return@setProperty method.invoke(this, *args)
                } catch (e: Exception) {
                    return@setProperty null
                }
            }
        }
    }

    private fun submit(runnable: Runnable) {
        if (!executor.isShutdown()) executor.submit(runnable)
    }

    @JSMethod
    fun s2t(text: String?): String {
        return Trans.s2t(false, text)
    }

    @JSMethod
    fun t2s(text: String?): String {
        return Trans.t2s(false, text)
    }

    @JSMethod
    fun getProxy(local: Boolean): String {
        return getUrl(local) + "?do=js"
    }

    @JSMethod
    fun js2Proxy(dynamic: Boolean?, siteType: Int, siteKey: String, url: String?, headers: JSObject): String {
        return getProxy(!dynamic!!) + "&from=catvod" + "&siteType=" + siteType + "&siteKey=" + siteKey + "&header=" + URLEncoder.encode(
            headers.stringify()
        ) + "&url=" + URLEncoder.encode(url)
    }

    @JSMethod
    fun setTimeout(func: JSFunction, delay: Int): Any? {
        func.hold()
        schedule(func, delay)
        return null
    }

    @JSMethod
    fun _http(url: String?, options: JSObject): JSObject? {
        val complete = options.getJSFunction("complete") ?: return req(url, options)
        val req: Req = Req.objectFrom(options.stringify())
        Connect.to(url, req).enqueue(getCallback(complete, req))
        return null
    }

    @JSMethod
    fun req(url: String?, options: JSObject): JSObject {
        try {
            val req: Req = Req.objectFrom(options.stringify())
            val res: Response = Connect.to(url, req).execute()
            return Connect.success(ctx, req, res)
        } catch (e: Exception) {
            return Connect.error(ctx)
        }
    }

    @JSMethod
    fun pd(html: String?, rule: String?, urlKey: String?): String {
        return parser.parseDomForUrl(html, rule, urlKey)
    }

    @JSMethod
    fun pdfh(html: String?, rule: String?): String {
        return parser.parseDomForUrl(html, rule, "")
    }


    @JSMethod
    fun pdfa(html: String?, rule: String?): JSArray {
        return JSUtil.toArray(ctx, parser.parseDomForArray(html, rule))
    }


    @JSMethod
    fun pdfl(html: String?, rule: String?, texts: String?, urls: String?, urlKey: String?): JSArray {
        return JSUtil.toArray(ctx, parser.parseDomForList(html, rule, texts, urls, urlKey))
    }


    @JSMethod
    fun joinUrl(parent: String, child: String): String {
        return Urls.convert(parent, child)
    }


    @JSMethod
    @Throws(CharacterCodingException::class)
    fun gbkDecode(buffer: JSArray?): String {
        val result: String = JSUtil.decodeTo("GB2312", buffer)
        log.debug("text:{}\nresult:\n{}", buffer, result)
        return result
    }


    @JSMethod
    fun md5X(text: String?): String {
        val result: String = Crypto.md5(text)
        log.debug("text:{}\nresult:\n{}", text, result)
        return result
    }


    @JSMethod
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


    @JSMethod
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

    private fun getCallback(complete: JSFunction, req: Req): Callback {
        return object : Callback {

            override fun onResponse( call: Call,  res: Response) {
                submit { complete.call(Connect.success(ctx, req, res)) }
            }

            override fun onFailure( call: Call,  e: IOException) {
                submit { complete.call(Connect.error(ctx)) }
            }
        }
    }

    private fun schedule(func: JSFunction, delay: Int) {
        timer.schedule(object : TimerTask() {
            override fun run() {
                submit { func.call() }
            }
        }, delay.toLong())
    }

    companion object {
        fun create(ctx: QuickJSContext, executor: ExecutorService): Global {
            return Global(ctx, executor)
        }
    }
}
