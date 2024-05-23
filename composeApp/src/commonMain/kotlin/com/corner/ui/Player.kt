package com.corner.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.player.DefaultControls
import com.corner.ui.player.VideoPlayer
import com.corner.ui.player.vlcj.VlcjController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun Player(mrl: String, controller: VlcjController, modifier: Modifier, component: DetailComponent) {
    val scope = rememberCoroutineScope()
    val showControllerBar = remember(mrl) { mutableStateOf(true) }
    val controlBarDuration = 5000L
    val hideJob = remember { mutableStateOf<Job?>(null) }
    DisposableEffect(mrl){
        println("mrl修改：$mrl")
//        changeShowState(scope, showControllerBar)
        onDispose {  }
    }
    Box(modifier.onPointerEvent(PointerEventType.Move){
            showControllerBar.value = true
            hideJob.value?.cancel()
        hideJob.value = scope.launch {
            delay(controlBarDuration)
            showControllerBar.value = false
        }
    }) {
        VideoPlayer(
            mrl = mrl,
            state = controller,
            modifier = Modifier.fillMaxSize()
        )
        AnimatedVisibility(showControllerBar.value,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = slideInVertically(initialOffsetY = {-it}) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = {it}) + fadeOut()
        ){
            DefaultControls(Modifier.background(Color.Gray.copy(alpha = 0.45f)).align(Alignment.BottomEnd), controller, component)
        }
    }
}
