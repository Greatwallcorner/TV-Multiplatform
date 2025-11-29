package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.corner.bean.HotData
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.catvodcore.bean.Site
import com.corner.catvodcore.bean.Vod
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jupnp.UpnpService
import org.slf4j.LoggerFactory

object GlobalAppState {
    private val log = LoggerFactory.getLogger(GlobalAppState::class.java)
    val isDarkTheme = MutableStateFlow(try {
        SettingStore.getSettingItem(SettingType.THEME) == "dark"
    } catch (e: Exception) {
        e.printStackTrace()
        false
    })
    var showProgress = MutableStateFlow(false)
    val hotList = MutableStateFlow(listOf<HotData>())
    val chooseVod = mutableStateOf<Vod>(Vod())

    val home = MutableStateFlow<Site>(Site.get("", ""))

    val clear = MutableStateFlow(false)
    val closeApp = MutableStateFlow(false)
    val videoFullScreen = MutableStateFlow(false)
    val DLNAUrl = MutableStateFlow("")

    // App Root CoroutineScope
    private val rootJob = Job()
    val rootScope = CoroutineScope(Dispatchers.IO + rootJob)


    private val upnpServiceLock = Any()
    private var _upnpService: UpnpService? = null
    var upnpService: UpnpService?
        get() = synchronized(upnpServiceLock) { _upnpService }
        set(value) = synchronized(upnpServiceLock) { _upnpService = value }

    var windowState: WindowState? = null
    var detailFrom = DetailFromPage.HOME

    fun cancelAllOperations(reason: String = "Normal shutdown") {
        if (!rootJob.isCancelled) {
            log.info("Cancelling all operations: $reason")
            rootScope.cancel(reason)
        }
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

    fun resetAllStates() {
        showProgress = MutableStateFlow(false)
        hotList.value = emptyList()
        home.value = Site.get("", "")
        clear.value = false
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
enum class DetailFromPage {
    SEARCH, DLNA, HOME
}