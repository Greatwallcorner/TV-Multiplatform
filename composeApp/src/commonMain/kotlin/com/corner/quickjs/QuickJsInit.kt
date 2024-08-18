package com.corner.quickjs

import com.corner.util.Constants
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


object QuickJsInit {
    private val log: Logger = LoggerFactory.getLogger(QuickJsInit::class.java)

    fun init(){
        val resPath = System.getProperty(Constants.resPathKey)
        if(resPath.isNullOrBlank()) {
            log.error("资源路径为空")
            val resource = this.javaClass.getResource("/lib/qjs")
            if (resource != null) {
                if(!resource.file.isNullOrBlank()){
                    val file = File(resource.file)
                    System.load(file.path)
                }
            }
        }else{
            val qjs = File(resPath).resolve("gjs")
            if(qjs.exists()){
                try {
                    System.load(qjs.path)
                } catch (e: Exception) {
                    log.error("加载quickjs失败", e)
                }
            }else{
                log.error("quickjs文件不存在")
            }
        }
    }
}