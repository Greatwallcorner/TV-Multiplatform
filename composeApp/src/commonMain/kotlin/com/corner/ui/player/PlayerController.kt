package com.corner.ui.player

import com.corner.catvod.enum.bean.Vod
import com.corner.database.History
import kotlinx.coroutines.flow.StateFlow
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

interface PlayerController {
    val state: StateFlow<PlayerState>

    var showTip: Boolean
    var tip: String

    var history:History?

    fun load(url: String): PlayerController

    fun onMediaPlayerReady(mediaPlayer: EmbeddedMediaPlayer)

    fun doWithMediaPlayer(block: (MediaPlayer) -> Unit)

    fun play()

    fun play(url:String)
    fun pause()
    fun stop()
    fun dispose()
    fun seekTo(timestamp: Long)
    fun setVolume(value: Float)

    fun volumeUp()

    fun volumeDown()
    fun toggleSound()

    fun toggleFullscreen()

    fun speed(speed: Float)

    fun stopForward()

    fun fastForward()
    fun togglePlayStatus()
    fun init()
    fun backward(time: String = "15s")
    fun forward(time: String = "15s")

    fun updateOpening(detail: Vod?)

    fun updateEnding(detail: Vod?)
    fun setStartEnding(opening: Long, ending: Long)
}