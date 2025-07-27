package com.corner.server.plugins

import cn.hutool.core.io.file.FileNameUtil
import com.corner.catvodcore.util.Http
import com.corner.server.logic.proxy
import com.corner.ui.scene.SnackBar
import com.corner.util.toSingleValueMap
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream

fun Application.configureRouting() {
    val log = LoggerFactory.getLogger("Routing")
    routing {
        staticResources("/static", "assets"){
            contentType {
                val suffix = FileNameUtil.getSuffix(it.path)
                when(suffix){
                   "js" -> ContentType.Text.JavaScript
                    "jsx" -> ContentType.Application.JavaScript
                    "html" -> ContentType.Text.Html
                    "txt" -> ContentType.Text.Plain
                    "htm" -> ContentType.Text.Html
                    else -> null
                }
            }
        }

//        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/") {
            call.respondText("hello world!")
        }

        get("/postMsg") {
            val msg = call.request.queryParameters["msg"]
            if (msg?.isBlank() == true) {
                call.respond(HttpStatusCode.MultiStatus, "消息不可为空")
                return@get
            }
            SnackBar.postMsg(msg!!)
        }

        /**
         * 播放器必须支持range请求 否则会返回完整资源 导致拨动进度条加载缓慢
         */

        /**
         * clevebitr: 使用use来管理资源，看看能否修复潜在的内存泄露、资源管理问题
         * */

        get("/proxy") {
            val parameters = call.request.queryParameters
            val paramMap = parameters.toSingleValueMap().toMutableMap()
            paramMap.putAll(call.request.headers.toSingleValueMap())
//            log.info("proxy param:{}", paramMap)
            // 0 statusCode 1 content_type 2 body
            try {
                val objects: Array<Any> = proxy(paramMap) ?: arrayOf()
                if (objects.isEmpty()) {
                    errorResp(call)
                } else when {
                    objects[0] is Response -> (objects[0] as Response).use { response ->
                        response.headers.forEach { (name, value) ->
                            if (!HttpHeaders.isUnsafe(name)) {
                                call.response.headers.append(name, value)
                            }
                        }
//                        log.debug("proxy resp code:{} headers:{}", response.code, response.headers)
                        call.respondOutputStream(status = HttpStatusCode.fromValue(response.code)) {
                            response.body.byteStream().use { it.transferTo(this) }
                        }
                    }
                    objects[0] == HttpStatusCode.Found.value -> {
                        val redirectUrl = objects[2] as? String ?: run {
                            errorResp(call)
                            return@get
                        }
                        call.respondRedirect(Url(redirectUrl), false)
                    }
                    else -> {
                        (objects.getOrNull(3) as? Map<*, *>)?.forEach { (t, u) ->
                            if (t is String && u is String) call.response.headers.append(t, u)
                        }
                        (objects[2] as? InputStream)?.use { inputStream ->
                            call.respondOutputStream(
                                contentType = ContentType.parse(objects[1].toString()),
                                status = HttpStatusCode.fromValue(objects[0] as? Int ?: HttpStatusCode.InternalServerError.value)
                            ) {
                                inputStream.transferTo(this)
                            }
                        } ?: errorResp(call)
                    }
                }
            } catch (e: IOException) {
                // 静默处理预期内的IO异常
            } catch (e: Exception) {
                log.error("proxy处理失败", e)
            }
        }
        get("/proxy/m3u8") {
            val url = call.request.queryParameters["url"] ?: run {
                errorResp(call, "URL参数缺失")
                return@get
            }
            // 1. 构建完整的请求头（直接从浏览器复制）
            var header: Map<String, String> = mapOf(
                "Accept" to "application/json, text/javascript, */*; q=0.01",
                "Accept-Encoding" to "gzip, deflate, br, zstd",
                "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "DNT" to "1",
                "Origin" to "https://hhjx.hhplayer.com",
                "Priority" to "u=1, i",
                "Sec-Ch-Ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"",
                "Sec-Ch-Ua-Mobile" to "?0",
                "Sec-Ch-Ua-Platform" to "\"Windows\"",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "same-origin",
                "Sec-Fetch-Storage-Access" to "active",
                "Sec-Gpc" to "1",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0",
                "X-Requested-With" to "XMLHttpRequest"
            )
            try {
                // 使用use自动关闭资源
                val content = Http.newCall(url, headers = header.toHeaders())
                    .execute()
                    .use { response ->
                        response.body.string()
                    }

                // 定义需要检测的图片扩展名
                val imageExtensions = listOf("png", "jpg", "jpeg", "webp", "bmp", "gif")
                val extensionPattern = imageExtensions.joinToString("|")

                // 关键修正：调整正则表达式捕获组
                val regex = Regex("""(https?://[^\s"']+?\.)($extensionPattern)(?=[\s"'>]|$)""")

                // 执行替换（保留完整URL路径，只替换扩展名）
                val processedContent = content.replace(regex) {
                    "${it.groupValues[1]}ts"  // groupValues[1]是URL路径+点号，groupValues[2]是原扩展名
                }

                // 返回处理后的内容
                call.respondText(processedContent, ContentType.Application.OctetStream)
            } catch (e: Exception) {
                log.error("处理M3U8代理失败", e)
                errorResp(call, "处理M3U8文件失败")
            }
        }

//        get("/multi") {
//            val params = call.request.queryParameters
//            val url = params["url"]
//            if (StringUtils.isEmpty(url) || url!!.startsWith("http")) {
//                errorResp(call, "url不合法")
//                return@get
//            }
//            var thread = 5
//            if (StringUtils.isNotEmpty(params.get("thread"))) {
//                thread = params.get("thread")!!.toInt()
//            }
//            multiThreadDownload(url, thread, call)
//        }
    }
}

suspend fun errorResp(call: ApplicationCall) {
    call.respondText(
        text = HttpStatusCode.InternalServerError.description,
        contentType = ContentType.Application.OctetStream,
        status = HttpStatusCode.InternalServerError,
        {})
}

suspend fun errorResp(call: ApplicationCall, msg: String) {
    call.respondText(
        text = HttpStatusCode.InternalServerError.description,
        contentType = ContentType.Application.OctetStream,
        status = HttpStatusCode.InternalServerError,
        {})
}
