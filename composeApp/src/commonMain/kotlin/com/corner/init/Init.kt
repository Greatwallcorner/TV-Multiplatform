package com.corner.init

import com.corner.bean.Hot
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.catvodcore.config.ApiConfig

import com.corner.catvodcore.config.init
import com.corner.catvodcore.enum.ConfigType
import com.corner.catvodcore.loader.JarLoader
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.database.Db
import com.corner.database.appModule
import com.corner.dlna.TVMUpnpService
import com.corner.server.KtorD
import com.corner.ui.player.vlcj.VlcJInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

private val isInitialized = AtomicBoolean(false)

private val log = LoggerFactory.getLogger("Init")
class Init {
    companion object {
        private var instance: KoinApplication? = null
        suspend fun start() {
            showProgress()
            try {
                initKoin()
                //Http Server
                KtorD.init()
                initConfig()
                initPlatformSpecify()
                Hot.getHotList()
                VlcJInit.init()
                GlobalAppState.upnpService.value = TVMUpnpService().apply {
                    startup()
                    sendAlive()
                }
            } finally {
                hideProgress()
            }
        }

        fun stop(){
            KtorD.stop()
            VlcJInit.release()
            instance?.close()
        }


        private fun initKoin() {
            instance = startKoin {
//                logger()
                modules(appModule)
            }
        }
    }
}

expect fun initPlatformSpecify()


fun initConfig() {

    if (isInitialized.get()) {
        log.warn("配置已初始化，跳过重复操作")
        return
    }

    val siteConfig = runBlocking {
        withContext(Dispatchers.IO) {
            Db.Config.findOneByType(ConfigType.SITE.ordinal.toLong())
        }
    } ?: run {
        log.warn("未找到站点配置")
        return
    }

    try {
        log.info("初始化开始....")
        JarLoader.clear()
        ApiConfig.clear()
        GlobalAppState.clear.update {!it}

        val vod = SettingStore.getSettingItem(SettingType.VOD.id)

        if (StringUtils.isBlank(vod)) {
            log.info("未配置点播源，跳过初始化")
            return
        }
        ApiConfig.parseConfig(siteConfig!!, false).init()
        log.info("初始化完成!")

    } catch (e: Exception) {
        log.error("初始化失败", e)
        log.error("尝试使用json解析", e)
        ApiConfig.parseConfig(siteConfig!!, true).init()
    }
}