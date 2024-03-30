import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.corner.catvodcore.enum.Menu
import com.corner.database.appModule
import com.corner.ui.*
import com.corner.ui.scene.*
import com.corner.ui.video.videoScene

actual fun getPlatformName(): String = "Desktop"

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WindowScope.MainView(state: WindowState, onClose: () -> Unit) {
    AppTheme(useDarkTheme = true) {
        var currentChoose by remember { mutableStateOf(Menu.HOME) }
        // 最外层 - 窗口边框 阴影
        Column(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).border(
                border = BorderStroke(1.dp, Color(59, 59, 60)), // firefox的边框灰色
            ).shadow(5.dp, shape = RoundedCornerShape(8.dp),
            ambientColor = Color.DarkGray, spotColor = Color.DarkGray
        )
        ) {
            WindowDraggableArea {
                ControlBar(onClickMinimize = {state.isMinimized = !state.isMinimized },
                    onClickMaximize = {
                        state.placement =
                            if (WindowPlacement.Maximized == state.placement) WindowPlacement.Floating else WindowPlacement.Maximized
                    },
                    onClickClose = { onClose() })
            }
//            val state = rememberVideoPlayerState()
//            /*
//             * Could not use a [Box] to overlay the controls on top of the video.
//             * See https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Swing_Integration
//             * Related issues:
//             * https://github.com/JetBrains/compose-multiplatform/issues/1521
//             * https://github.com/JetBrains/compose-multiplatform/issues/2926
//             */
//            Column {
//                VideoPlayer(
//                    url = VIDEO_URL,
//                    state = state,
//                    onFinish = state::stopPlayback,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(400.dp)
//                )
//                Slider(
//                    value = state.progress.value.fraction,
//                    onValueChange = { state.seek = it },
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text("Timestamp: ${state.progress.value.timeMillis} ms", modifier = Modifier.width(180.dp))
//                    IconButton(onClick = state::toggleResume) {
//                        Icon(
//                            painter = painterResource("${if (state.isResumed) "pause" else "play"}.svg"),
//                            contentDescription = "Play/Pause",
//                            modifier = Modifier.size(32.dp)
//                        )
//                    }
//                    IconButton(onClick = state::toggleFullscreen) {
//                        Icon(
//                            painter = painterResource("${if (state.isFullscreen) "exit" else "enter"}-fullscreen.svg"),
//                            contentDescription = "Toggle fullscreen",
//                            modifier = Modifier.size(32.dp)
//                        )
//                    }
//                    Speed(
//                        initialValue = state.speed,
//                        modifier = Modifier.width(104.dp)
//                    ) {
//                        state.speed = it ?: state.speed
//                    }
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Icon(
//                            painter = painterResource("volume.svg"),
//                            contentDescription = "Volume",
//                            modifier = Modifier.size(32.dp)
//                        )
//                        // TODO: Make the slider change volume in logarithmic manner
//                        //  See https://www.dr-lex.be/info-stuff/volumecontrols.html
//                        //  and https://ux.stackexchange.com/q/79672/117386
//                        //  and https://dcordero.me/posts/logarithmic_volume_control.html
//                        Slider(
//                            value = state.volume,
//                            onValueChange = { state.volume = it },
//                            modifier = Modifier.width(100.dp)
//                        )
//                    }
//                }
//            }
            Box {
                AnimatedContent(currentChoose,
                    modifier = Modifier.background(color = MaterialTheme.colors.background),
                     transitionSpec = {
                         fadeIn(initialAlpha = 0.3f) togetherWith fadeOut()
//                         slideInVertically (
//                             animationSpec = tween(150),
//                             initialOffsetY = { fullHeight -> fullHeight }
//                         ) togetherWith
//                                 slideOutVertically(
//                                     animationSpec = tween(200),
//                                     targetOffsetY = { fullHeight -> -fullHeight }
//                                 )
                     }) {
                    when (currentChoose) {
                        Menu.HOME -> videoScene(
                            modifier = Modifier,
                            onClickSwitch = {menu-> currentChoose = menu})

                        Menu.SETTING -> SettingScene(modifier = Modifier, onClickBack = { currentChoose = Menu.HOME })
                        Menu.SEARCH -> SearchScene(onClickBack = { currentChoose = Menu.HOME })
                        Menu.HISTORY -> HistoryScene { currentChoose = Menu.HOME }
                    }
                }
                SnackBar.SnackBarList()
                LoadingIndicator(showProgress = isShowProgress())
            }
        }
    }
}

/**
 * See [this Stack Overflow post](https://stackoverflow.com/a/67765652).
 */
@Composable
fun Speed(
    initialValue: Float,
    modifier: Modifier = Modifier,
    onChange: (Float?) -> Unit
) {
    var input by remember { mutableStateOf(initialValue.toString()) }
    OutlinedTextField(
        value = input,
        modifier = modifier,
        singleLine = true,
        leadingIcon = {
            Icon(
                painter = painterResource("speed.svg"),
                contentDescription = "Speed",
                modifier = Modifier.size(28.dp)
            )
        },
        onValueChange = {
            input = if (it.isEmpty()) {
                it
            } else if (it.toFloatOrNull() == null) {
                input // Old value
            } else {
                it // New value
            }
            onChange(input.toFloatOrNull())
        }
    )
}


@Preview
@Composable
fun AppPreview() {
    appModule()
}