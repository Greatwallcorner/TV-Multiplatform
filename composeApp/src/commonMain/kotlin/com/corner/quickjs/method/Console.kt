//package com.corner.quickjs.method
//
//import com.whl.quickjs.wrapper.QuickJSContext
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//
//class Console : QuickJSContext.Console {
//    override fun log(info: String) {
//        log.debug(info)
//    }
//
//    override fun info(info: String) {
//        log.info(info)
//    }
//
//    override fun warn(info: String) {
//        log.warn(info)
//    }
//
//    override fun error(info: String) {
//        log.error(info)
//    }
//
//    companion object {
//        private const val TAG = "quickjs"
//        private val log: Logger = LoggerFactory.getLogger(Console::class.java)
//    }
//}