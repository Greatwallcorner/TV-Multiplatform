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
import com.corner.RootContent
import com.corner.bean.SettingStore
import com.corner.catvodcore.util.Utils.printSystemInfo
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.init.Init
import com.corner.init.generateImageLoader
import com.corner.ui.Util
import com.corner.ui.scene.SnackBar
import com.seiko.imageloader.LocalImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lumentv_compose.composeapp.generated.resources.LumenTV_icon_png
import org.jetbrains.compose.resources.painterResource
import org.slf4j.LoggerFactory
import lumentv_compose.composeapp.generated.resources.Res
import java.awt.Dimension

private val log = LoggerFactory.getLogger("main")

fun main() {
    launchErrorCatcher()
    printSystemInfo()

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

        val scope = rememberCoroutineScope()

        val contextMenuRepresentation =
            if (isSystemInDarkTheme()) DarkDefaultContextMenuRepresentation else LightDefaultContextMenuRepresentation
        Window(
            onCloseRequest = ::exitApplication, icon = painterResource(Res.drawable.LumenTV_icon_png), title = "LumenTV",
            state = windowState,
            undecorated = true,
            transparent = false,
        ) {
            window.minimumSize = Dimension(800, 600)
            CompositionLocalProvider(
                LocalImageLoader provides remember { generateImageLoader() },
                LocalContextMenuRepresentation provides remember { contextMenuRepresentation },
            ) {
                RootContent()
            }
            scope.launch {
                GlobalAppState.closeApp.collect {
                    if (it) {
                        try {
                            // 1. 隐藏窗口
                            window.isVisible = false
                            // 2. 保存设置
                            SettingStore.write()
                            // 3. 清理函数
                            Init.stop()
                        } catch (e: Exception) {
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

private fun launchErrorCatcher() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        SnackBar.postMsg("未知异常， 请查看日志", type = SnackBar.MessageType.ERROR)
        log.error("未知异常", e)
        Init.stop()
    }
}