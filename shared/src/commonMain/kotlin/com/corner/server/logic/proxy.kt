package com.corner.server.logic

import com.corner.catvodcore.loader.JarLoader
import com.corner.catvodcore.util.KtorClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

suspend fun proxy(params: Map<String, String>): Array<Any>? {
    if (params.containsKey("do") && params.get("do")?.equals("js") ?: false) {

    } else if (params.containsKey("do") && params.get("do")?.equals("py") ?: false) {

    } else {
        return JarLoader.proxyInvoke(params)
    }
    return null
}

const val segmentSize = 1024 * 1024 // 1MB
val logger = LoggerFactory.getLogger("multiThreadDownload")
fun multiThreadDownload(url: String, thread: Int, call: ApplicationCall?) {
    runBlocking {
//        val semaphore = Semaphore(thread)
        // 获取需要下载的内容长度
        val client = KtorClient.client

        val head = client.head(url)
        if (!head.headers.contains(HttpHeaders.AcceptRanges)) {
            logger.error("不支持range请求 url:$url")
            call?.respond(HttpStatusCode.InternalServerError, "不支持range请求")
            return@runBlocking
        }
        val contentLen = head.contentLength()
        if (contentLen == null || contentLen.toInt() == 0) {
            logger.warn("contentLength为空:${contentLen}")
            return@runBlocking
        }
        var segmentCount = contentLen.div(segmentSize)
        if (contentLen % segmentSize > 0) segmentCount++
        val mutex = Mutex()
        val segments = (0 until segmentCount).map { i ->
            val start = if((i * segmentSize).toInt() == 0) 0 else i * segmentSize - 1
            val end = if (start + segmentSize > contentLen) contentLen - 1 else start + segmentSize
            Pair(start, end)
        }.toMutableList()
        for (i in 0..thread) {
            launch {
                while (true) {
                    if (segments.isEmpty()) return@launch
                    var segment: Pair<Long, Long>? = null
                    mutex.withLock {
                        segment = segments.removeFirst()
                    }
                    if (segment == null) {
                        return@launch
                    } else {
                        downloadSegment(url, segment!!, call)
                    }
                }
            }
        }
    }


}

suspend fun downloadSegment(url: String, segment: Pair<Long, Long>, call: ApplicationCall?) {
    val response = KtorClient.client.get {
        url(url)
        header(HttpHeaders.Range, "bytes=${segment.first}-${segment.second}")
    }
//    call.respondOutputStream {
//        write(response.body<ByteArray>())
//        headers { response.headers }
//    }
    println("${response.contentLength()} ${response.headers.get(HttpHeaders.ContentRange)}")
}
