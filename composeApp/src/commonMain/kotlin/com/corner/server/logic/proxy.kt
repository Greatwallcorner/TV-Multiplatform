package com.corner.server.logic

import com.corner.catvodcore.loader.JarLoader
import com.corner.util.KtorClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


fun proxy(params: Map<String, String>): Array<Any>? {
    when (params["do"]) {
        "js" -> { /* js */ }
        "py" -> { /* py */ }
        else -> return JarLoader.proxyInvoke(params)
    }
    return null
}

private val logger = LoggerFactory.getLogger("multiThreadDownload")

suspend fun multiThreadDownload(url: String, thread: Int, call: ApplicationCall?) {
    coroutineScope {
        val client = KtorClient.client

        // 直接使用head请求，Ktor 2.x会自动管理资源
        val response = client.head(url)

        if (!response.headers.contains(HttpHeaders.AcceptRanges)) {
            logger.error("服务器不支持Range请求: $url")
            call?.respond(HttpStatusCode.BadRequest, "Range not supported")
            return@coroutineScope
        }

        val contentLen = response.contentLength() ?: run {
            logger.warn("内容长度未知")
            call?.respond(HttpStatusCode.BadRequest, "Unknown content length")
            return@coroutineScope
        }

        val segmentSize = calculateSegmentSize(contentLen)
        val segments = createSegments(contentLen, segmentSize)
        val segmentChannel = Channel<Pair<Long, Long>>(Channel.UNLIMITED)

        segments.forEach { segmentChannel.send(it) }
        segmentChannel.close()

        repeat(thread) {
            launch(Dispatchers.IO) {
                for (segment in segmentChannel) {
                    try {
                        downloadSegment(url, segment, call)
                    } catch (e: Exception) {
                        logger.error("分段下载失败 [$segment]", e)
                    }
                }
            }
        }
    }
}

private fun calculateSegmentSize(contentLength: Long): Long {
    return when {
        contentLength > 1_000_000_000 -> 10 * 1024 * 1024
        else -> 1 * 1024 * 1024
    }
}

private fun createSegments(contentLength: Long, segmentSize: Long): List<Pair<Long, Long>> {
    val segmentCount = (contentLength + segmentSize - 1) / segmentSize
    return (0 until segmentCount).map { i ->
        val start = i * segmentSize
        val end = minOf(start + segmentSize - 1, contentLength - 1)
        start to end
    }
}

suspend fun downloadSegment(url: String, segment: Pair<Long, Long>, call: ApplicationCall?) {
    // 依赖协程作用域自动管理资源
    val data = KtorClient.client.get(url) {
        header(HttpHeaders.Range, "bytes=${segment.first}-${segment.second}")
    }.body<ByteArray>()

    logger.debug("下载完成: ${segment.first}-${segment.second}, 大小: ${data.size}")
    // TODO: 处理数据
}

//suspend fun downloadSegment(url: String, segment: Pair<Long, Long>, call: ApplicationCall?) {
//    val response = KtorClient.client.get {
//        url(url)
//        header(HttpHeaders.Range, "bytes=${segment.first}-${segment.second}")
//    }
////    call.respondOutputStream {
////        write(response.body<ByteArray>())
////        headers { response.headers }
////    }
//    println("${response.contentLength()} ${response.headers.get(HttpHeaders.ContentRange)}")
//}