package com.corner.server.plugins

import cn.hutool.core.io.file.FileNameUtil
import com.corner.server.logic.proxy
import com.corner.util.toSingleValueMap
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLDecoder

private val log = LoggerFactory.getLogger("Routing")

fun Application.configureRouting() {

    routing {
        staticResources("/static", "assets") {
            contentType {
                val suffix = FileNameUtil.getSuffix(it.path)
                when (suffix) {
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
            // 创建 File 对象
            val htmlFile = File("src/commonMain/resources/LumenTV Proxy Placeholder Webpage.html")
            // 检查文件是否存在
            if (htmlFile.exists()) {
                call.respondFile(htmlFile)
            } else {
                // 文件不存在时返回错误信息
                call.respondText("文件未找到", status = HttpStatusCode.NotFound)
            }
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
                                status = HttpStatusCode.fromValue(
                                    objects[0] as? Int ?: HttpStatusCode.InternalServerError.value
                                )
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

        //添加缓存文件本地代理
        get("/proxy/m3u8") {
            val encodedUrl = call.request.queryParameters["url"] ?: run {
                errorResp(call, "URL参数缺失")
                return@get
            }

            // 1. 解码并验证URL安全性
            val decodedUrl = try {
                URLDecoder.decode(encodedUrl, "UTF-8").also { url ->
                    if (url.contains("proxy/m3u8") || !url.startsWith("https://")) {
                        errorResp(call, "非法的目标URL")
                        return@get
                    }
                }
            } catch (e: Exception) {
                errorResp(call, "URL解码失败")
                return@get
            }

            // 2. 发起请求（代理不重复过滤）
            val client = OkHttpClient.Builder().build()
            try {
                val content = client.newCall(Request.Builder().url(decodedUrl).build())
                    .execute().use { response ->
                        if (!response.isSuccessful) {
                            errorResp(call, "上游服务器返回错误: ${response.code}")
                            return@get
                        }
                        response.body?.string() ?: ""
                    }

                // 3. 返回原始内容（拦截器已预先处理过）
                call.respondText(content, ContentType.Application.OctetStream)
            } catch (e: Exception) {
                log.error("代理请求失败: URL=$decodedUrl", e)
                errorResp(call, "代理请求失败: ${e.message}")
            }
        }
        get("/proxy/cached_m3u8") {
            val id = call.request.queryParameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing cache ID")
                return@get
            }

            val content = M3U8Cache.get(id) ?: run {
                call.respond(HttpStatusCode.NotFound, "Cache expired or invalid")
                return@get
            }

            call.respondText(content, ContentType.Application.OctetStream)
        }
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
