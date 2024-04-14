package com.github.catvod.crawler

import org.slf4j.LoggerFactory

object SpiderDebug {
    private val log = LoggerFactory.getLogger(SpiderDebug::class.java.simpleName)


    @JvmStatic
    fun log(th: Throwable){
        log.error("error:", th)
    }

    @JvmStatic
    fun log(msg: String) {
        log.debug(msg)
    }
}