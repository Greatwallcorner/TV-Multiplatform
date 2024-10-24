package com.corner.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.bean.PlayerStateCache
import com.corner.bean.SettingStore
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.player.DefaultControls
import com.corner.ui.player.PlayerState
import com.corner.ui.player.frame.FrameContainer
import com.corner.ui.player.vlcj.VlcjFrameController
import com.corner.ui.scene.Dialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
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
    val showTip = controller.showTip.collectAsState()
    val tip = controller.tip.collectAsState()
    val videoFullScreen = GlobalModel.videoFullScreen.subscribeAsState()
    val showMediaInfoDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit){
        val volume = SettingStore.getCache("playerState")
        if(volume != null){
            val v = (volume as PlayerStateCache).get("volume")?.toFloat()
            controller.doWithPlayState { it.update { it.copy(volume = v ?: .8f) }}
        }
    }

    DisposableEffect(videoFullScreen.value, showControllerBar.value){
        try {
            keepScreenOnJob?.cancel()
            if(videoFullScreen.value && !showControllerBar.value){
                var time = 1
                if (videoFullScreen.value) {
                    focusRequester.requestFocus()
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
            }
        } catch (e: Exception) {
            log.error("keep screen on timer err:",e)
        }
        onDispose {
            keepScreenOnJob?.cancel()
        }
    }

    val showCursor = remember { mutableStateOf(true) }
    DisposableEffect(mrl) {
        scope.launch {
            if(StringUtils.isNotBlank(mrl)){
                controller.load(mrl)
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
    }.pointerHoverIcon(PointerIcon(if (!showCursor.value) createEmptyCursor() else Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)))) {
        FrameContainer(Modifier.fillMaxSize().focusTarget().focusable().focusRequester(focusRequester), controller){
            showControllerBar.value = !showControllerBar.value
        }
        AnimatedVisibility(showControllerBar.value,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn(),
            exit = fadeOut()){
            Row (Modifier.height(40.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End){
                IconButton(onClick = {
                    showMediaInfoDialog.value = true
                }){
                    Icon(Icons.Default.Info, contentDescription = "media info", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        AnimatedVisibility(
            showControllerBar.value,
            modifier = Modifier.align(Alignment.BottomEnd),
//            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
//            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DefaultControls(
                Modifier.background(Color.Gray.copy(alpha = 0.45f))
                    .height(80.dp)
                    .align(Alignment.BottomEnd), controller, component
            )
        }
        LaunchedEffect(tip.value){
            delay(1500)
            controller.tip.emit("")
            controller.showTip.emit(false)
        }
        AnimatedVisibility(showTip.value) {
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
        MediaInfoDialog(Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.4f), controller.state.value, showMediaInfoDialog.value){
            showMediaInfoDialog.value = false
        }
    }
}

@Composable
fun MediaInfoDialog(modifier: Modifier, playerState: PlayerState, show:Boolean, onClose:()->Unit){
    val mediaInfo = rememberUpdatedState(playerState.mediaInfo)
    Dialog(modifier, showDialog = show, onClose = onClose){
        val scrollbar = rememberLazyListState(0)
        Box{
            LazyColumn(verticalArrangement = Arrangement.spacedBy(30.dp), modifier = Modifier.padding(30.dp), state = scrollbar,
                horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    SelectionContainer {
                        Text(text = AnnotatedString(mediaInfo.value?.url ?: ""))
                    }
                    Spacer(Modifier.size(40.dp))
                    Text("${mediaInfo.value?.width ?: ""} * ${mediaInfo.value?.height ?: ""}")
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scrollbar),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 5.dp, horizontal = 8.dp),
                style = defaultScrollbarStyle().copy(
                    unhoverColor = Color.Gray.copy(0.45F),
                    hoverColor = Color.DarkGray
                ))
        }
    }
}

//@Preview
//@Composable
//fun previewMediaInfoDialog(){
//    AppTheme {
//        MediaInfoDialog(Modifier.fillMaxSize(), MediaInfo(800, 1200, "http://xxxxxx.com/dddd"), true){
//
//        }
//    }
//}

private fun createEmptyCursor(): Cursor {
    return Toolkit.getDefaultToolkit().createCustomCursor(
        BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB),
        Point(1, 1),
        "Empty Cursor"
    )
}
