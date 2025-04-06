import androidx.compose.foundation.DarkDefaultContextMenuRepresentation
import androidx.compose.foundation.LightDefaultContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cn.hutool.core.util.SystemPropsUtil
import com.corner.RootContent
import com.corner.bean.SettingStore
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.init.Init
import com.corner.init.generateImageLoader
import com.corner.ui.Util
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

fun main() {
    launchErrorCatcher()
    printSystemInfo()
    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Performing cleanup before exiting...")
        Init.stop()
    })
//    System.setProperty("java.net.useSystemProxies", "true");
    application {
        val windowState = rememberWindowState(
            size = Util.getPreferWindowSize(600, 500), position = WindowPosition.Aligned(Alignment.Center)
        )
        GlobalAppState.windowState = windowState

        LaunchedEffect(Unit) {
            launch(Dispatchers.Default) {
                Init.start()
            }
        }

        val transparent = rememberUpdatedState(!SysVerUtil.isWin10())
        val scope = rememberCoroutineScope()

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
                LocalContextMenuRepresentation provides remember { contextMenuRepresentation },
            ) {
                RootContent(modifier = Modifier.fillMaxSize())
            }
            scope.launch {
                GlobalAppState.closeApp.collect{
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
        Init.stop()
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