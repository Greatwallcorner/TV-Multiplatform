package com.corner.server.plugins

import com.corner.server.logic.multiThreadDownload
import com.corner.server.logic.proxy
import com.corner.ui.scene.SnackBar
import fi.iki.elonen.NanoHTTPD
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

fun Application.configureRouting() {
    val log = LoggerFactory.getLogger("Routing")
    routing {
        staticResources("/static", "assets")

//        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/") {
            call.respondText("hello world!")
        }

        get("/postMsg"){
            val msg = call.request.queryParameters["msg"]
            if(msg?.isBlank() == true){
                call.respond(HttpStatusCode.MultiStatus, "消息不可为空")
                return@get
            }
            SnackBar.postMsg(msg!!);
        }

        get("/proxy") {
            val parameters = call.request.queryParameters
            log.info("proxy", parameters)
            val toMap = parameters.toMap()
            // 0 statusCode 1 content_type 2 body
            val objects: Array<Any> = proxy(parameters.toSingleValueMap()) ?: arrayOf()
            if (objects.isEmpty()) errorResp(call)
            else if (objects[0] is NanoHTTPD.Response) {
//                val response = objects[0] as NanoHTTPD.Response
//                call.respondBytes(response.data.readAllBytes(), ContentType.parse(response.mimeType),
//                    HttpStatusCode.fromValue(response.status.requestStatus),
//                    {
//                        headers =
//                    })


            } else {
                if(HttpStatusCode.Found.value == objects[0]){
                    call.respondRedirect(Url(objects[2] as String), false)
                }else{
                    call.respondBytes(
                        ContentType.parse(objects[1].toString()),
                        HttpStatusCode.fromValue(objects[0] as Int)
                    ) { (objects[2] as ByteArrayInputStream).readBytes() }
                }
            }
        }

        get("/multi") {
            val params = call.request.queryParameters
            val url = params.get("url")
            if (StringUtils.isEmpty(url) || url!!.startsWith("http")) {
                errorResp(call, "url不合法")
                return@get
            }
            var thread = 5
            if (StringUtils.isNotEmpty(params.get("thread"))) {
                thread = params.get("thread")!!.toInt()
            }
            multiThreadDownload(url, thread, call)
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

fun StringValues.toSingleValueMap(): Map<String, String> =
    entries().associateByTo(LinkedHashMap(), { it.key }, { it.value.toList()[0] })
