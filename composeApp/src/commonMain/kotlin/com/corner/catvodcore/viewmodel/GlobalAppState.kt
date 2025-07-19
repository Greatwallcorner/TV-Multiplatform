package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.loader.JarLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jupnp.UpnpService
import org.slf4j.LoggerFactory

object GlobalAppState {
    private val log = LoggerFactory.getLogger(GlobalAppState::class.java)
    // State Flows
    val showProgress = MutableStateFlow(false)
    val hotList = MutableStateFlow(listOf<HotData>())
    val chooseVod = mutableStateOf<Vod>(Vod())
    val home = MutableStateFlow<Site>(Site.get("", ""))
    val clear = MutableStateFlow(false)
    val closeApp = MutableStateFlow(false)
    val videoFullScreen = MutableStateFlow(false)
    val DLNAUrl = MutableStateFlow("")
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + supervisorJob)

    // Services
    var upnpService = mutableStateOf<UpnpService?>(null)
        private set

    // Window Management
    var windowState: WindowState? = null

    // Navigation
    var detailFrom = DetailFromPage.HOME

    /* ========== Public Methods ========== */

    fun initWindowState(state: WindowState) {
        windowState = state
    }

    fun clearHome() {
        home.value = Site.get("", "")
    }

    fun toggleVideoFullScreen(): Boolean {
        toggleWindowFullScreen()
        videoFullScreen.value = !videoFullScreen.value
        return videoFullScreen.value
    }

    fun showProgress() {
        showProgress.update { true }
    }

    fun hideProgress() {
        showProgress.update { false }
    }

    fun isShowProgress(): Boolean = showProgress.value

    /**
     * 关闭应用时，应该执行清理函数，虽然写的是Shit
     * */

    fun cleanupBeforeExit(onComplete: () -> Unit = {}) {
        coroutineScope.launch {
            try {
                log.info("Starting application cleanup...")

                // 1. 取消所有子协程
                supervisorJob.cancelChildren()

                // 2. 清理UPnP服务
                upnpService.value?.let {
                    it.shutdown()
                    upnpService.value = null
                    log.info("UPnP service stopped")
                }

                // 3. 终止爬虫运行
                JarLoader.clear()

                // 4. 重置状态
                resetAllStates()

                log.info("Cleanup completed successfully")
                onComplete()
            } catch (e: Exception) {
                log.error("Cleanup failed", e)
                onComplete()
            }
        }
    }

    private fun resetAllStates() {
        // 重置所有StateFlow
        listOf(
            showProgress,
            hotList,
            home,
            clear,
            closeApp,
            videoFullScreen,
            DLNAUrl
        ).forEach { flow ->
            when (flow) {
                is MutableStateFlow<*> -> {
                    when (val currentValue = flow.value) {
                        is Boolean -> (flow as MutableStateFlow<Boolean>).value = false
                        is List<*> -> (flow as MutableStateFlow<List<*>>).value = emptyList<Any>()
                        is String -> (flow as MutableStateFlow<String>).value = ""
                        else -> (flow as MutableStateFlow<Any?>).value = null
                    }
                }
            }
        }
        chooseVod.value = Vod()
    }

    /* ========== Private Methods ========== */

    private fun toggleWindowFullScreen() {
        windowState?.let {
            it.placement = when (it.placement) {
                WindowPlacement.Fullscreen -> WindowPlacement.Floating
                else -> WindowPlacement.Fullscreen
            }
        }
    }
}

enum class DetailFromPage {
    SEARCH, DLNA, HOME
}