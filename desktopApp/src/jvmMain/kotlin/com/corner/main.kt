package com.corner

import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.corner.bean.SettingStore
import com.corner.init.Init
import com.corner.init.generateImageLoader
import com.corner.ui.SwingUtil
import com.corner.ui.Util
import com.corner.ui.decompose.DefaultRootComponent
import com.corner.ui.decompose.RootContent
import com.seiko.imageloader.LocalImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.*


private val log = LoggerFactory.getLogger("main")
@OptIn(ExperimentalDecomposeApi::class)
fun main(){
    launchErrorCatcher()
    application {
        val lifecycle = LifecycleRegistry()
        val root = SwingUtil.runOnUiThread {
            DefaultRootComponent(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
            )
        }

        LaunchedEffect(Unit) {
            launch(Dispatchers.Default){
                Init.start()
            }
        }
        val windowState = rememberWindowState(
            size = Util.getPreferWindowSize(800, 800), position = WindowPosition.Aligned(Alignment.Center)
        )

        LifecycleController(lifecycle, windowState)

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
                RootContent(component = root, modifier =  Modifier.fillMaxSize(), windowState){
                    SettingStore.write()
                    exitApplication()
                }
            }
        }
    }
}

private fun launchErrorCatcher() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Dialog(Frame(), e.message ?: "Error").apply {
            log.error("启动异常", e)
            layout = FlowLayout()
            val label = Label(e.message)
            val text = TextArea(e.stackTraceToString())
            add(label)
            add(text)
            val button = Button("OK").apply {
                addActionListener { dispose() }
            }
            add(button)
            setSize(300, 300)
            isVisible = true
        }
    }
}