package com.corner.quickjs.method

import cn.hutool.core.net.URLEncodeUtil
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
import org.mozilla.javascript.*
import org.mozilla.javascript.annotations.JSFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService

class Global private constructor(
    private val ctx: Context,
    scope: ScriptableObject,
    private val executor: ExecutorService
) {
    private val jsObject: ScriptableObject = scope
    private val parser: Parser = Parser()
    private val timer: Timer = Timer()

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun setProperty() {
        for (method in javaClass.methods) {
            if (!method.isAnnotationPresent(JSFunction::class.java)) continue
            jsObject.put(method.name, jsObject, FunctionObject(method.name, method, jsObject))
        }
    }

    private fun submit(runnable: Runnable) {
        if (!executor.isShutdown) executor.submit(runnable)
    }


    fun s2t(text: String?): String {
        return Trans.s2t(false, text)
    }


    fun t2s(text: String?): String {
        return Trans.t2s(false, text)
    }


    fun getProxy(local: Boolean): String {
        return getUrl(local) + "?do=js"
    }


    fun js2Proxy(dynamic: Boolean?, siteType: Int, siteKey: String, url: String?, headers: ScriptableObject): String {
        return getProxy(!dynamic!!) + "&from=catvod" + "&siteType=" + siteType + "&siteKey=" + siteKey + "&header=" + URLEncodeUtil.encode(
            (
                    Context.toString(headers)
                    ) + "&url=" + URLEncodeUtil.encode(url)
        )
    }


    fun setTimeout(func: FunctionObject, delay: Int): Any? {
//        func.hold()
        schedule(func, delay)
        return null
    }


    fun _http(url: String?, options: ScriptableObject): Scriptable? {
        if (options.has("complete", options)) {
            val funComplete = options.get("complete")
            val req: Req = Req.objectFrom(Context.toString(options))
            Connect.to(url, req).enqueue(getCallback(funComplete as ScriptableObject, req))
        } else {
            return req(url, options)
        }
//        val complete = options.getJSFunction("complete") ?: return req(url, options)
//        val req: Req = Req.objectFrom(options.stringify())
//        Connect.to(url, req).enqueue(getCallback(complete, req))
        return null
    }


    fun req(url: String?, options: ScriptableObject): Scriptable {
        try {
            val req: Req = Req.objectFrom(Context.toString(options))
            val res: Response = Connect.to(url, req).execute()
            return Connect.success(ctx, options, req, res)
        } catch (e: Exception) {
            return Connect.error(ctx, options)
        }
    }


    fun pd(html: String?, rule: String?, urlKey: String?): String {
        return parser.parseDomForUrl(html, rule, urlKey)
    }


    fun pdfh(html: String?, rule: String?): String {
        return parser.parseDomForUrl(html, rule, "")
    }


    fun pdfa(html: String?, rule: String?): List<Any> {
        return JSUtil.toArray(ctx, jsObject, parser.parseDomForArray(html, rule))
    }


    fun pdfl(html: String?, rule: String?, texts: String?, urls: String?, urlKey: String?): NativeArray {
        return JSUtil.toArray(ctx, jsObject, parser.parseDomForList(html, rule, texts, urls, urlKey))
    }


    fun joinUrl(parent: String, child: String): String {
        return Urls.convert(parent, child)
    }


    @Throws(CharacterCodingException::class)
    fun gbkDecode(buffer: NativeArray?): String {
        val result: String = JSUtil.decodeTo("GB2312", buffer)
        log.debug("text:{}\nresult:\n{}", buffer, result)
        return result
    }


    fun md5X(text: String?): String {
        val result: String = Crypto.md5(text)
        log.debug("text:{}\nresult:\n{}", text, result)
        return result
    }


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
                    (complete as FunctionObject).call(
                        ctx,
                        jsObject,
                        complete,
                        arrayOf(Connect.success(ctx, jsObject, req, res))
                    )
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
        fun create(ctx: Context, scope: ScriptableObject, executor: ExecutorService): Global {
            return Global(ctx, scope, executor)
        }
    }
}