package com.corner.init

import com.corner.bean.Hot
import com.corner.catvodcore.config.api
import com.corner.catvodcore.config.init
import com.corner.catvodcore.config.parseConfig
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.loader.JarLoader
import com.corner.database.Db
import com.corner.database.appModule
import com.corner.server.KtorD
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger("Init")
class Init {
    companion object {
        suspend fun start() {
            showProgress()
            try {
                initKoin()
                //Http Server
                KtorD.init()
                initConfig()
                initPlatformSpecify()
                Hot.getHotList()
            } finally {
                hideProgress()
            }
        }

        fun stop(){
            KtorD.stop()
        }


        private fun initKoin() {
            startKoin {
//                logger()
                modules(appModule())
            }
        }
    }
}

expect fun initPlatformSpecify();

fun initConfig() {
    log.info("initConfig start")
    JarLoader.clear()
    api = null

    val siteConfig = Db.Config.findOneByType(ConfigType.SITE.ordinal.toLong()) ?: return
    try {
        parseConfig(siteConfig, false)?.init()
    } catch (e: Exception) {
        parseConfig(siteConfig, true)?.init()
    }
    log.info("initConfig end")
}