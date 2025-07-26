package com.corner.ui.nav.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.corner.ui.video.log

object DialogState {

    // 新增标志位，记录用户是否选择在浏览器打开
    var userChoseOpenInBrowser by mutableStateOf(false)
    var showPngDialog = false
        private set

    var currentM3U8Url = ""
        set(value) {
            log?.debug("DialogState.currentM3U8Url 更新为: {}", value)
            field = value
        }
//        private set

    // 新增标志位，表明当前播放的视频链接是特殊链接
    var isSpecialVideoLink by mutableStateOf(false)

    // 切换特殊链接状态的方法
    fun toggleSpecialVideoLink(isSpecial: Boolean) {
        isSpecialVideoLink = isSpecial
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
        println("resetBrowserChoice 方法被调用，userChoseOpenInBrowser 将被重置为 false")
        userChoseOpenInBrowser = false
    }
}