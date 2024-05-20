package com.rjuszczyk.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class VideoPlayerState {
    private var mediaPlayer: MediaPlayer? = null
    private val defferredEffects = mutableListOf<(MediaPlayer) -> Unit>()

    actual fun doWithMediaPlayer(block: (MediaPlayer) -> Unit) {
        mediaPlayer?.let {
            block(it)

        }?:run {
            defferredEffects.add(block)
        }

    }

    internal fun onMediaPlayerReady(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
        val mp = MediaPlayer(mediaPlayer)
        this.mediaPlayer = mp

        defferredEffects.forEach { block ->
            block(mp)
        }
        defferredEffects.clear()
    }
}
@Composable
actual fun rememberVideoPlayerState() = remember { VideoPlayerState() }