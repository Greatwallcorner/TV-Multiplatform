package com.corner.init

import com.corner.bean.Hot
import com.corner.bean.SettingStore
import com.corner.catvodcore.config.init
import com.corner.catvodcore.config.parseConfig
import com.corner.catvodcore.enum.ConfigType
import com.corner.database.Db
import com.corner.database.appModule
import com.corner.server.KtorD
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import org.koin.core.context.startKoin

class Init {
    companion object {
        suspend fun start() {
            showProgress()
            try {
                initKoin()
                initConfig()
                //Http Server
                KtorD.init()
                initPlatformSpecify()
                Hot.getHotList()
            } finally {
                hideProgress()
            }
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
    val siteConfig = Db.Config.findOneByType(ConfigType.SITE.ordinal.toLong()) ?: return
    try {
        parseConfig(siteConfig, false)?.init()
    } catch (e: Exception) {
        parseConfig(siteConfig, true)?.init()
    }
}