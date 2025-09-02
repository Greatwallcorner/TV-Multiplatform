package com.corner.ui.player

import com.corner.catvod.enum.bean.Vod
import com.corner.database.entity.History
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

interface PlayerController {
    val state: StateFlow<PlayerState>

    var showTip: MutableStateFlow<Boolean>
    var tip: MutableStateFlow<String>

    var history:MutableStateFlow<History?>

    var playerReady: StateFlow<Boolean>

    var endingHandled: Boolean

    var playerLoading:Boolean

    var playerPlayering:Boolean
    fun load(url: String): PlayerController
    fun onMediaPlayerReady(mediaPlayer: EmbeddedMediaPlayer)
    fun doWithMediaPlayer(block: (MediaPlayer) -> Unit)
    suspend fun initAsync()
    suspend fun stopAsync()
    suspend fun loadAsync(url: String, timeoutMillis: Long = 10000): PlayerController
    fun play()
    fun setAspectRatio(aspectRatio: String)
    fun getAspectRatio(): String
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
    fun doWithPlayState(func: (MutableStateFlow<PlayerState>) -> Unit)
    fun resetOpeningEnding()
    fun vlcjFrameInit() {}
}