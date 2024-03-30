package com.github.catvod.crawler

import org.slf4j.LoggerFactory

/**
@author heatdesert
@date 2023-12-24 23:11
@description
 */
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