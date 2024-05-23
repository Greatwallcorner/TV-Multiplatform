package com.corner.ui.player

import kotlinx.coroutines.flow.StateFlow
import uk.co.caprica.vlcj.player.base.MediaPlayer

interface PlayerController {
    val state: StateFlow<PlayerState>
    fun load(url: String): PlayerController

    fun onMediaPlayerReady(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer)

    fun doWithMediaPlayer(block: (MediaPlayer) -> Unit)

    fun play()

    fun play(url:String)
    fun pause()
    fun stop()
    fun dispose()
    fun seekTo(timestamp: Long)
    fun setVolume(value: Float)
    fun toggleSound()

    fun toggleFullscreen()

    fun speed(speed: Float)
    fun togglePlayStatus()
    fun init()
}