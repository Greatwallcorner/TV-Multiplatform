package com.corner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rjuszczyk.compose.OnTimeChangedListener
import com.rjuszczyk.compose.VideoPlayer
import com.rjuszczyk.compose.rememberVideoPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.math.max
import kotlin.math.min

const val VIDEO_URL = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
@OptIn(ExperimentalResourceApi::class)
@Composable
fun Player(){
        val videoFile = VIDEO_URL



        Box(){
        val videoPlayerState = rememberVideoPlayerState()

        var isPlaying by remember(videoFile) { mutableStateOf(false) }
        val timeMillisStateFlow = remember(videoFile) { MutableStateFlow(-1L) }
        val lengthMillisStateFlow = remember(videoFile) { MutableStateFlow(-1L) }

        val timeMillis by timeMillisStateFlow.collectAsState()
        val lengthMillis by lengthMillisStateFlow.collectAsState()

        LaunchedEffect(videoFile) {
                videoPlayerState.doWithMediaPlayer { mediaPlayer ->
                        lengthMillisStateFlow.value = mediaPlayer.getLengthMillis()
                        mediaPlayer.addOnTimeChangedListener(object : OnTimeChangedListener {
                                override fun onTimeChanged(timeMillis: Long) {
                                        timeMillisStateFlow.value = timeMillis
                                }
                        })
                }
        }

        VideoPlayer(
                mrl = videoFile,
                state = videoPlayerState,
                modifier = Modifier.fillMaxSize()
        )

        Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
        ) {
                Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        Button(
                                onClick = {
                                        videoPlayerState.doWithMediaPlayer { mediaPlayer ->
                                                mediaPlayer.setTimeAccurate( max(mediaPlayer.getTimeMillis() - 5000, 0) )
                                        }
                                }) {

                                Text("Backward")
                        }
                        Button(
                                onClick = {
                                        // show =  !show
                                        videoPlayerState.doWithMediaPlayer { mediaPlayer ->
                                                if(mediaPlayer.isPlaying) {
                                                        isPlaying = false
                                                        mediaPlayer.pause()
                                                } else {
                                                        mediaPlayer.play()
                                                        isPlaying = true
                                                }
                                        }
                                }) {

                                Text(if(isPlaying) "Pause" else "Play")
                        }
                        Button(
                                onClick = {
                                        videoPlayerState.doWithMediaPlayer { mediaPlayer ->
                                                mediaPlayer.setTimeAccurate( min(mediaPlayer.getTimeMillis() + 5000, mediaPlayer.getLengthMillis()) )
                                        }
                                }) {

                                Text("Forward")
                        }
                }

                if(lengthMillis != -1L) {
                        Slider(
                                value = timeMillis/lengthMillis.toFloat(),
                                onValueChange = {
                                        videoPlayerState.doWithMediaPlayer {mediaPlayer ->
                                                timeMillisStateFlow.value = (it*lengthMillis).toLong()
                                                mediaPlayer.setTime((it*lengthMillis).toLong())
                                        }
                                },
                                modifier= Modifier.fillMaxWidth()
                        )
                }

        }


}
//    val state = rememberVideoPlayerState()
    /*
     * Could not use a [Box] to overlay the controls on top of the video.
     * See https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Swing_Integration
     * Related issues:
     * https://github.com/JetBrains/compose-multiplatform/issues/1521
     * https://github.com/JetBrains/compose-multiplatform/issues/2926
     */
//        val controller = remember(VIDEO_URL) { VlcjController() }
//
//        ComponentPlayer(Modifier.background(MaterialTheme.colorScheme.background), VIDEO_URL, controller.component, controller)
//
//        DisposableEffect(VIDEO_URL) {
//                controller.play(VIDEO_URL)
//                onDispose { controller.dispose() }
//        }
//        VideoPlayer(
//            url = VIDEO_URL,
//            state = state,
//            onFinish = state::stopPlayback,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(400.dp)
//        )
//        Slider(
//            value = state.progress.value.fraction,
//            onValueChange = { state.seek = it },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Timestamp: ${state.progress.value.timeMillis} ms", modifier = Modifier.width(180.dp))
//            IconButton(onClick = state::toggleResume) {
//                Icon(
//                    painter = painterResource("pic/${if (state.isResumed) "pause" else "play"}.svg"),
//                    contentDescription = "Play/Pause",
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//            IconButton(onClick = state::toggleFullscreen) {
//                Icon(
//                    painter = painterResource("pic/${if (state.isFullscreen) "exit" else "enter"}-fullscreen.svg"),
//                    contentDescription = "Toggle fullscreen",
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//            Speed(
//                initialValue = state.speed,
//                modifier = Modifier.width(104.dp)
//            ) {
//                state.speed = it ?: state.speed
//            }
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    painter = painterResource("pic/volume.svg"),
//                    contentDescription = "Volume",
//                    modifier = Modifier.size(32.dp),
//                )
//                // TODO: Make the slider change volume in logarithmic manner
//                //  See https://www.dr-lex.be/info-stuff/volumecontrols.html
//                //  and https://ux.stackexchange.com/q/79672/117386
//                //  and https://dcordero.me/posts/logarithmic_volume_control.html
//                Slider(
//                    value = state.volume,
//                    onValueChange = { state.volume = it },
//                    modifier = Modifier.width(100.dp)
//                )
//            }
//        }
}
