import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cn.hutool.core.util.SystemPropsUtil
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.corner.RootContent
import com.corner.bean.SettingStore
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.init.Init
import com.corner.init.generateImageLoader
import com.corner.ui.SwingUtil
import com.corner.ui.Util
import com.corner.ui.decompose.DefaultRootComponent
import com.corner.ui.scene.SnackBar
import com.corner.util.SysVerUtil
import com.seiko.imageloader.LocalImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.slf4j.LoggerFactory
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.TV_icon_s
import java.awt.Dimension


private val log = LoggerFactory.getLogger("main")

@OptIn(ExperimentalDecomposeApi::class)
fun main() {
    launchErrorCatcher()
    printSystemInfo()
    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Performing cleanup before exiting...")
        Init.stop()
    })
//    System.setProperty("java.net.useSystemProxies", "true");
    application {
        val lifecycle = LifecycleRegistry()
        val root = SwingUtil.runOnUiThread {
            DefaultRootComponent(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
            )
        }

        val windowState = rememberWindowState(
            size = Util.getPreferWindowSize(600, 500), position = WindowPosition.Aligned(Alignment.Center)
        )
        GlobalModel.windowState = windowState

        LaunchedEffect(Unit) {
            launch(Dispatchers.Default) {
                Init.start()
            }
        }

        LifecycleController(lifecycle, windowState)

        val transparent = rememberUpdatedState(!SysVerUtil.isWin10())

        val contextMenuRepresentation =
            if (isSystemInDarkTheme()) DarkDefaultContextMenuRepresentation else LightDefaultContextMenuRepresentation
        Window(
            onCloseRequest = ::exitApplication, icon = painterResource(Res.drawable.TV_icon_s), title = "TV",
            state = windowState,
            undecorated = true,
            transparent = false,
        ) {
            window.minimumSize = Dimension(700, 600)
            CompositionLocalProvider(
                LocalImageLoader provides remember { generateImageLoader() },
                LocalContextMenuRepresentation provides remember { contextMenuRepresentation }
            ) {
                RootContent(component = root, modifier = Modifier.fillMaxSize())
            }
            GlobalModel.closeApp.subscribe {
                if(it){
                    try {
                        window.isVisible = false
                        SettingStore.write()
                    }catch(e: Exception){
                        log.error("关闭应用异常", e)
                    } finally {
                        exitApplication()
                    }
                }
            }
        }

 
    }
}

fun printSystemInfo() {
    val s = StringBuilder("\n")
    getSystemPropAndAppend("java.version", s)
    getSystemPropAndAppend("java.home", s)
    getSystemPropAndAppend("os.name", s)
    getSystemPropAndAppend("os.arch", s)
    getSystemPropAndAppend("os.version", s)
    getSystemPropAndAppend("user.dir", s)
    getSystemPropAndAppend("user.home", s)
    log.info("系统信息：{}", s.toString())
}

private fun getSystemPropAndAppend(key: String, s: StringBuilder) {
    val v = SystemPropsUtil.get(key)
    if (v.isNotBlank()) {
        s.append(key).append(":").append(v).append("\n")
    }
}

private fun launchErrorCatcher() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        SnackBar.postMsg("未知异常， 请查看日志")
        log.error("未知异常", e)
//        Dialog(Frame(), e.message ?: "Error").apply {
//            log.error("启动异常", e)
//            layout = FlowLayout()
//            val label = Label(e.message)
//            val text = TextArea(e.stackTraceToString())
//            add(label)
//            add(text)
//            val button = Button("OK").apply {
//                addActionListener { dispose() }
//            }
//            add(button)
//            setSize(300, 300)
//            isVisible = true
//        }
    }
}