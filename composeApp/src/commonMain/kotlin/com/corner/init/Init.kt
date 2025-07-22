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
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.runtime.State

private val isInitialized = AtomicBoolean(false)

private val log = LoggerFactory.getLogger("Init")
class Init {
    companion object {
        private val _isInitializedSuccessfully = mutableStateOf(false)
        val isInitializedSuccessfully: State<Boolean> = _isInitializedSuccessfully
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
                GlobalAppState.upnpService = TVMUpnpService().apply {
                    startup()
                    sendAlive()
                }
            } finally {
                hideProgress()
            }
        }

        fun stop() {
            // 1. 先停止业务逻辑
            GlobalAppState.cancelAllOperations("Application shutdown")

            // 2. 按依赖顺序释放资源
            try {
                KtorD.stop() // 先停止网络服务
                instance?.close() // 关闭应用级资源
                VlcJInit.release() // 最后释放VLC,避免出现Invalid memory access问题
            } catch (e: Throwable) {
                log.error("Cleanup error", e)
            }
        }


        private fun initKoin() {
            instance = startKoin {
//                logger()
                modules(appModule)
            }
        }

        fun initConfig() {
            if (isInitialized.get()) {
                log.warn("配置已初始化，跳过重复操作")
                _isInitializedSuccessfully.value = true  // 假设已初始化即成功
                return
            }

            val siteConfig = runBlocking {
                withContext(Dispatchers.IO) {
                    Db.Config.findOneByType(ConfigType.SITE.ordinal.toLong())
                }
            } ?: run {
                log.warn("未找到站点配置")
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
                    log.info("未配置点播源，跳过初始化")
                    _isInitializedSuccessfully.value = false  // 初始化失败
                    return
                }

                ApiConfig.parseConfig(
                    cfg = siteConfig,
                    isJson = false,
                    onSuccess = { _isInitializedSuccessfully.value = true },
                    onError = { e ->
                        _isInitializedSuccessfully.value = false
                        log.error("配置解析失败", e)
                    }
                ).init()
                log.info("初始化完成!")
                _isInitializedSuccessfully.value = true  // 初始化成功

            } catch (e: Exception) {
                log.error("初始化失败", e)
                log.error("尝试使用json解析", e)
                try {
                    ApiConfig.parseConfig(
                        cfg = siteConfig,
                        isJson = true,
                        onSuccess = { _isInitializedSuccessfully.value = true },
                        onError = { e ->
                            _isInitializedSuccessfully.value = false
                            log.error("配置解析失败", e)
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