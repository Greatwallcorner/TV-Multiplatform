package com.corner.ui.player

data class PlayerState(
    val state:PlayState = PlayState.PAUSE,
    val bufferProgression: Float = 0.0f,
    val isMuted: Boolean = false,
    var isFullScreen: Boolean = false,
    val volume: Float = .5f,
    val timestamp: Long = 0L,
    val duration: Long = 0L,
    val speed: Float = 1F,
    var opening:Long = -1,
    val ending:Long = -1,
    val mediaInfo: MediaInfo? = null,
    val msg:String = "",
    val aspectRatio: String = ""
)

data class MediaInfo(
    val height: Int,
    val width: Int,
    val url: String,
    val videoCodec: String,
    val bitRate: Int,
    val duration: Long,
    val codecDescription: String
)

enum class PlayState {
    PLAY, BUFFERING, ERROR, PAUSE
}