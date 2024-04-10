package com.corner

import MainView
import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.corner.bean.SettingStore
import com.corner.init.Init
import com.corner.init.generateImageLoader
import com.corner.ui.Util
import com.seiko.imageloader.LocalImageLoader

fun main() = application {
    LaunchedEffect(Unit) {
        Init.start()
    }
    val windowState = rememberWindowState(
        size = Util.getPreferWindowSize(800, 800), position = WindowPosition.Aligned(Alignment.Center)
    )
    val contextMenuRepresentation =
        if (isSystemInDarkTheme()) DarkDefaultContextMenuRepresentation else LightDefaultContextMenuRepresentation
    Window(
        onCloseRequest = ::exitApplication, icon = painterResource("/TV-icon-s.png"), title = "TV",
        state = windowState,
        undecorated = true,
        transparent = true
    ) {
        CompositionLocalProvider(
            LocalImageLoader provides remember { generateImageLoader() },
            LocalContextMenuRepresentation provides remember { contextMenuRepresentation }
        ) {
            MainView(windowState) {
                SettingStore.write()
                exitApplication()
            }
        }
    }
}