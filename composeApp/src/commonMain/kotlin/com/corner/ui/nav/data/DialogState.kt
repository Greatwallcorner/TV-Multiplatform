package com.corner.ui.nav.data

object DialogState {
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
}