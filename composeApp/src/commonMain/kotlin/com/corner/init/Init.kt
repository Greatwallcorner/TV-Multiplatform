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
import com.corner.database.entity.Config
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
    log.info("initConfig start")
    JarLoader.clear()
    ApiConfig.clear()
    GlobalAppState.clear.update {!it}

    val vod = SettingStore.getSettingItem(SettingType.VOD.id)
    if(StringUtils.isBlank(vod)) return
    var siteConfig:Config?
    // todo 清空点播设置后 仍然可以在数据库中查询到旧的配置
    runBlocking { withContext(Dispatchers.IO){
        siteConfig = Db.Config.findOneByType(ConfigType.SITE.ordinal.toLong())
    } }
    if(siteConfig == null) return
    try {
        ApiConfig.parseConfig(siteConfig!!, false).init()
    } catch (e: Exception) {
        log.error("initConfig error 尝试使用json解析", e)
        ApiConfig.parseConfig(siteConfig!!, true).init()
    }
    log.info("initConfig end")
}