package com.corner.catvodcore.util

import cn.hutool.core.io.FileUtil
import cn.hutool.core.io.IoUtil
import cn.hutool.core.io.resource.ResourceUtil
import com.corner.server.logic.logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.charset.Charset

object Asset {
    private val log = LoggerFactory.getLogger(this::class.java)
    @JvmStatic
    fun open(fileName: String): InputStream? {
        return try {
            val resource = ResourceUtil.getResource(fileName.replace("assets://", ""))
            resource.openStream()
//            val resourceAsStream = Asset::class.java.getResourceAsStream(fileName.replace("assets://", ""))
//            resourceAsStream
//            FileUtil.getInputStream(fileName.replace("assets://", ""))
        } catch (e: Exception) {
            log.error("asset open fail", e)
            null
        }
    }

    @JvmStatic
    fun read(fileName: String): String {
        return try {
            IoUtil.read(open(fileName), Charset.defaultCharset())
        } catch (e: Exception) {
            log.error("asset read fail", e)
            ""
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val resource = this::class.java.getResource("pic/TV-icon-s.png")
        println(resource.path)
    }
}
