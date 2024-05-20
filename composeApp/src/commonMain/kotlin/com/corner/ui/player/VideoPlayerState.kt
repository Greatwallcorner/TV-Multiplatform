package com.rjuszczyk.compose

import androidx.compose.runtime.Composable

expect class VideoPlayerState {
    fun doWithMediaPlayer(block: (MediaPlayer) -> Unit)
}

@Composable
expect fun rememberVideoPlayerState(): VideoPlayerState