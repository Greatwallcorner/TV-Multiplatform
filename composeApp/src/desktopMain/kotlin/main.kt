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
import com.corner.ui.UpdateDialog
import com.corner.ui.Util
import com.corner.ui.scene.SnackBar
import com.corner.util.update.DownloadProgress
import com.corner.util.update.UpdateDownloader
import com.corner.util.update.UpdateLauncher
import com.corner.util.update.UpdateManager
import com.corner.util.update.UpdateResult
import com.seiko.imageloader.LocalImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lumentv_compose.composeapp.generated.resources.LumenTV_icon_png
import org.jetbrains.compose.resources.painterResource
import org.slf4j.LoggerFactory
import lumentv_compose.composeapp.generated.resources.Res
import java.io.File
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

        val scope = rememberCoroutineScope()

        var showUpdateDialog by remember { mutableStateOf(false) }
        var updateResult by remember { mutableStateOf<UpdateResult.Available?>(null) }
        var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }

        LaunchedEffect(Unit) {
            launch(Dispatchers.Default) {
                Init.start()
            }
            launch(Dispatchers.IO) {
                val result = UpdateManager.checkForUpdate()
                if (result is UpdateResult.Available) {
                    updateResult = result
                    showUpdateDialog = true
                }
            }
        }

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
                            window.isVisible = false
                            SettingStore.write()
                            Init.stop()
                        } catch (e: Exception) {
                            log.error("关闭应用异常", e)
                        } finally {
                            exitApplication()
                        }
                    }
                }
            }

            if (showUpdateDialog && updateResult != null) {
                UpdateDialog(
                    currentVersion = updateResult!!.currentVersion,
                    latestVersion = updateResult!!.latestVersion,
                    downloadProgress = downloadProgress,
                    onDismiss = {
                        showUpdateDialog = false
                        downloadProgress = null
                    },
                    onUpdate = {
                        scope.launch(Dispatchers.IO) {
                            downloadProgress = DownloadProgress.Starting
                            val tempDir = System.getProperty("java.io.tmpdir")
                            val zipFile = File(tempDir, "LumenTV-update.zip")

                            try {
                                UpdateDownloader.downloadUpdateSync(
                                    updateResult!!.downloadUrl,
                                    zipFile
                                ).onSuccess {
                                    downloadProgress = DownloadProgress.Completed(zipFile)
                                    UpdateLauncher.launchUpdater(zipFile)
                                    UpdateLauncher.exitApplication()
                                }.onFailure { e ->
                                    downloadProgress = DownloadProgress.Failed(e.message ?: "下载失败")
                                }
                            } catch (e: Exception) {
                                downloadProgress = DownloadProgress.Failed(e.message ?: "下载失败")
                            }
                        }
                    }
                )
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