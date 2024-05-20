package com.corner.ui.player

import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val state: StateFlow<PlayerState>
    fun load(url: String): PlayerController
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