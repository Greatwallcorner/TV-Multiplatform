package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.loader.JarLoader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jupnp.UpnpService
import org.slf4j.LoggerFactory

object GlobalAppState {
    private val log = LoggerFactory.getLogger(GlobalAppState::class.java)

    // State Flows (保持不变)
    var showProgress = MutableStateFlow(false)
    val hotList = MutableStateFlow(listOf<HotData>())
    val chooseVod = mutableStateOf<Vod>(Vod())

    val home = MutableStateFlow<Site>(Site.get("", ""))

    val clear = MutableStateFlow(false)
    val closeApp = MutableStateFlow(false)
    val videoFullScreen = MutableStateFlow(false)
    val DLNAUrl = MutableStateFlow("")

    // 协程管理改进点1：改用普通Job
    private val mainJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + mainJob)

    // 服务管理改进点2：添加同步控制
    private val upnpServiceLock = Any()
    private var _upnpService: UpnpService? = null
    var upnpService: UpnpService?
        get() = synchronized(upnpServiceLock) { _upnpService }
        set(value) = synchronized(upnpServiceLock) { _upnpService = value }

    // 保持不变
    var windowState: WindowState? = null
    var detailFrom = DetailFromPage.HOME

    /* ========== 新增关键方法 ========== */

    fun cancelAllOperations(reason: String = "Normal shutdown") {
        if (!mainJob.isCancelled) {
            log.info("Cancelling all operations: $reason")
            mainJob.cancel(reason)
        }
    }

    /* ========== 清理流程改进 ========== */

    fun cleanupBeforeExit(onComplete: () -> Unit = {}) {
        if (mainJob.isCancelled) {
            onComplete()
            return
        }

        coroutineScope.launch {
            try {
                log.info("开始执行清理操作...")

                // 1. 取消所有操作
                cancelAllOperations("取消所有操作...")

                // 2. 安全关闭UPnP
                upnpService?.let {
                    it.shutdown()
                    upnpService = null
                    log.info("UPnP服务已关闭")
                }

                // 3. 清理JarLoader
                JarLoader.clear()

                // 4. 重置状态
                resetAllStates()

                log.info("清理操作执行成功！")
            } catch (e: Exception) {
                log.error("清理失败", e)
            } finally {
                onComplete()
            }
        }
    }

    /* ========== 原有方法保持不变 ========== */

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

    private fun resetAllStates() {
        showProgress = MutableStateFlow(false)
        hotList.value = emptyList()
        home.value = Site.get("", "")
        clear.value = false
        closeApp.value = false
        videoFullScreen.value = false
        DLNAUrl.value = ""
        chooseVod.value = Vod()
    }

    private fun toggleWindowFullScreen() {
        windowState?.placement = when (windowState?.placement) {
            WindowPlacement.Fullscreen -> WindowPlacement.Floating
            else -> WindowPlacement.Fullscreen
        }
    }
}

// 保持不变
enum class DetailFromPage {
    SEARCH, DLNA, HOME
}