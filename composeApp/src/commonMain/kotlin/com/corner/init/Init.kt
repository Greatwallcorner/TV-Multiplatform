package com.corner.init

import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.runtime.State
import com.corner.catvodcore.viewmodel.GlobalAppState.resetAllStates
import com.corner.util.BrowserUtils

private val log = LoggerFactory.getLogger("Init")

class Init {
    companion object {
        private val _isInitializedSuccessfully = mutableStateOf(false)
        val isInitializedSuccessfully: State<Boolean> = _isInitializedSuccessfully
        private var instance: KoinApplication? = null
        suspend fun start() {
            showProgress()
            try {
                //Koin
                initKoin()
                //Http Server
                KtorD.init()
                //点播源配置
                initConfig()
                //一致性初始化
                initPlatformSpecify()
                //热搜
                Hot.getHotList()
                //播放器
                VlcJInit.init()
                //DLNA
                initDLNA()
            } finally {
                hideProgress()
            }
        }

        fun stop() {
            GlobalAppState.cancelAllOperations("Application shutdown")
            try {
                resetAllStates()        //reset all states
                BrowserUtils.cleanup()  //stop webSocket
                VlcJInit.release()      //release VlcJ
                KtorD.stop()            //stop KtorD
                stopKoin()              //stop Koin
                stopDLNA()              //stop DLNA
                JarLoader.clear()       //clear Jar
            } catch (e: Throwable) {
                log.error("Cleanup error", e)
            }
        }

        private fun initKoin() {
            instance = startKoin {
                modules(appModule)
            }
        }

        private fun  stopKoin() {
            log.info("Stop Koin")
            instance?.close()
            instance = null
        }

        private fun initDLNA() {
            GlobalAppState.upnpService = TVMUpnpService().apply {
                startup()
                sendAlive()
            }
        }

        private fun stopDLNA() {
            log.info("Stop DLNA Service")
            GlobalAppState.upnpService?.shutdown()
            GlobalAppState.upnpService = null
        }

        fun initConfig() {
            if (_isInitializedSuccessfully.value) {
                log.warn("配置已初始化，跳过重复操作")
                return
            }

            val siteConfig = runBlocking {
                withContext(Dispatchers.IO) {
                    Db.Config.findOneByType(ConfigType.SITE.ordinal.toLong())
                }
            } ?: run {
                log.error("未找到站点配置")
                _isInitializedSuccessfully.value = false  // 初始化失败
                return
            }

            try {
                log.info("初始化开始....")
                JarLoader.clear()
                ApiConfig.clear()
                GlobalAppState.clear.update { !it }

                val vod = SettingStore.getSettingItem(SettingType.VOD.id)

                if (StringUtils.isBlank(vod)) {
                    log.warn("未配置点播源，跳过初始化")
                    _isInitializedSuccessfully.value = false  // 初始化失败
                    return
                }

                ApiConfig.parseConfig(
                    cfg = siteConfig,
                    isJson = false,
                    onSuccess = { _isInitializedSuccessfully.value = true },
                    onError = { e ->
                        _isInitializedSuccessfully.value = false
                    }
                ).init()
                log.info("初始化完成!")
                _isInitializedSuccessfully.value = true  // 初始化成功

            } catch (e: Exception) {
                log.error("初始化失败，尝试使用json解析", e)
                try {
                    ApiConfig.parseConfig(
                        cfg = siteConfig,
                        isJson = true,
                        onSuccess = { _isInitializedSuccessfully.value = true },
                        onError = { e ->
                            _isInitializedSuccessfully.value = false
                        }
                    ).init()
                    _isInitializedSuccessfully.value = true  // 回退方式初始化成功
                } catch (e2: Exception) {
                    log.error("JSON 解析也失败", e2)
                    _isInitializedSuccessfully.value = false  // 完全失败
                }
            }
        }
    }
}

expect fun initPlatformSpecify()