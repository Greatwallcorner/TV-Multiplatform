package com.corner.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.corner.catvod.enum.bean.Vod.Companion.isEmpty
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.database.Db
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.player.DefaultControls
import com.corner.ui.player.frame.FrameContainer
import com.corner.ui.player.vlcj.VlcjFrameController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.Cursor
import java.awt.Point
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.abs

const val VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

private val log = LoggerFactory.getLogger("Player")

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun Player(
    mrl: String,
    controller: VlcjFrameController,
    modifier: Modifier,
    component: DetailComponent,
    focusRequester: FocusRequester
) {
    val scope = rememberCoroutineScope()
    val showControllerBar = remember(mrl) { mutableStateOf(true) }
    val controlBarDuration = 5000L
    val hideJob = remember { mutableStateOf<Job?>(null) }
    val cursorJob = remember { mutableStateOf<Job?>(null) }
    var keepScreenOnJob: Timer? = remember { null }
    var mousePosition by remember { mutableStateOf(Offset.Zero) }
    val showTip = rememberUpdatedState(controller.showTip)
    val tip = rememberUpdatedState(controller.tip)

//    val isFullScreen = GlobalModel.videoFullScreen.subscribeAsState()
    val showCursor = remember { mutableStateOf(true) }
    DisposableEffect(mrl) {
        focusRequester.requestFocus()
        scope.launch {
            controller.load(mrl)
        }

        GlobalModel.videoFullScreen.observe {
            try {
                var time = 1
                if (it) {
                    keepScreenOnJob = Timer("keepScreenOn")
                    keepScreenOnJob?.scheduleAtFixedRate(timerTask {
                        val robot = Robot()
                        val v = if (time % 2 == 0) 1 else -1
                        robot.mouseMove((mousePosition.x + v).toInt(), mousePosition.y.toInt())
                        time++
                    }, 0, 6000L)
                } else {
                    keepScreenOnJob?.cancel()
                }
            } catch (e: Exception) {
                log.error("keep screen on timer err:",e)
            }
        }
        onDispose {
        }
    }
    Box(modifier.onPointerEvent(PointerEventType.Move) {
        val current = it.changes.first().position
        val cel = mousePosition.minus(current)
        if (abs(cel.x) < 2 || abs(cel.y) < 2) return@onPointerEvent

        showControllerBar.value = true
        mousePosition = current
        hideJob.value?.cancel()
        hideJob.value = scope.launch {
            delay(controlBarDuration)
            showControllerBar.value = false
        }
        cursorJob.value?.cancel()
        showCursor.value = true
        cursorJob.value = scope.launch {
            delay(3000)
            showCursor.value = false
        }
    }.onClick{
        showControllerBar.value = false
    }.onPointerEvent(PointerEventType.Enter) {
        focusRequester.requestFocus()
    }.onKeyEvent {
        focusRequester.requestFocus()
        true
    }.onSizeChanged {
        controller.playerSize = it.width to it.height
    }.pointerHoverIcon(PointerIcon(if (!showCursor.value) createEmptyCursor() else Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)))) {
        FrameContainer(Modifier.fillMaxSize(), controller, controller.size.collectAsState(null).value?.run {
            IntSize(first, second)
        } ?: IntSize.Zero,
            controller.bytes.collectAsState(null).value)
        AnimatedVisibility(
            showControllerBar.value,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            DefaultControls(
                Modifier.background(Color.Gray.copy(alpha = 0.45f))
                    .height(80.dp)
                    .align(Alignment.BottomEnd), controller, component
            )
        }
        val showTipBool =
            derivedStateOf { GlobalModel.videoFullScreen.value && showTip.value && tip.value.isNotBlank() }
        AnimatedVisibility(showTipBool.value) {
            Surface(
                Modifier.padding(start = 10.dp, top = 10.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    tip.value,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp),
                    fontSize = TextUnit(24f, TextUnitType.Sp)
                )
            }
        }
    }
}

private fun createEmptyCursor(): Cursor {
    return Toolkit.getDefaultToolkit().createCustomCursor(
        BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB),
        Point(0, 0),
        "Empty Cursor"
    )
}
