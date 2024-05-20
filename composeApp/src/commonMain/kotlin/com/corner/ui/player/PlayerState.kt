package com.corner.ui.player

data class PlayerState(
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    var isFullScreen: Boolean = false,
    val volume: Float = .5f,
    val timestamp: Long = 0L,
    val duration: Long = 0L,
    val speed: Float = 1F,
)