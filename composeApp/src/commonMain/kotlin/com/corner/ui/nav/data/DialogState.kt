package com.corner.ui.nav.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.slf4j.LoggerFactory

object DialogState {
    private val log = LoggerFactory.getLogger("DialogState")
    // 记录用户是否选择在浏览器打开
    var userChoseOpenInBrowser by mutableStateOf(false)
    var showPngDialog = false
        private set

    var currentM3U8Url: String = ""
        set(value) {
            if (value.isNotEmpty()) {
                log.debug("DialogState.currentM3U8Url --> {}", value)
            }
            field = value
        }

    // 表明当前播放的视频链接是否是特殊链接，用于判断是否需要弹出弹窗
    var openDialogState by mutableStateOf(false)

    fun changeDialogState(isSpecial: Boolean) {
        openDialogState = isSpecial
    }
    fun showPngDialog(url: String) {
        showPngDialog = true
        currentM3U8Url = url
    }

    fun dismissPngDialog() {
        showPngDialog = false
        currentM3U8Url = ""
    }

    // 切换视频时重置标志位
    fun resetBrowserChoice() {
        log.debug("resetBrowserChoice,userChoseOpenInBrowser -> {}", userChoseOpenInBrowser)
        userChoseOpenInBrowser = false
    }
}