package com.corner.ui.nav.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
object DialogState {

    // 新增标志位，记录用户是否选择在浏览器打开
    var userChoseOpenInBrowser by mutableStateOf(false)
    var showPngDialog = false
        private set

    var currentM3U8Url = ""
        private set

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