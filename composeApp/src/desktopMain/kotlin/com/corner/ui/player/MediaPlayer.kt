package com.corner.ui.player

import com.corner.util.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

actual class MediaPlayer(private val mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
    val player = mediaPlayer

    private val _state = MutableStateFlow(PlayerState())

    val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    val stateListener = object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            println("播放器初始化完成")
            catch {
                mediaPlayer.audio().setVolume(50)
            }
            _state.update { it.copy(duration = mediaPlayer.status().length()) }
        }

        override fun playing(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = true) }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun muted(mediaPlayer: MediaPlayer, muted: Boolean) {
            _state.update { it.copy(isMuted = muted) }
        }

        override fun volumeChanged(mediaPlayer: MediaPlayer, volume: Float) {
            _state.update { it.copy(volume = volume) }
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            _state.update { it.copy(timestamp = newTime) }
        }
    }

    actual fun play() {
        mediaPlayer.controls().play()
    }
    actual fun pause() {
        mediaPlayer.controls().pause()
    }
    actual val isPlaying: Boolean
        get() = mediaPlayer.status().isPlaying

    actual fun setRate(rate: Float) {
        mediaPlayer.controls().setRate(rate)
    }
    actual fun setTime(millis: Long) {
        mediaPlayer.controls().setTime(millis)
    }

    actual fun setTimeAccurate(millis: Long) {
        mediaPlayer.controls().setTime(millis)
    }

    actual fun getTimeMillis(): Long {
        return mediaPlayer.status().time()
    }

    actual fun getLengthMillis(): Long {
        return mediaPlayer.status().length()
    }

    actual fun addOnTimeChangedListener(listener: OnTimeChangedListener) {
        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun timeChanged(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?, newTime: Long) {
                super.timeChanged(mediaPlayer, newTime)
                listener.onTimeChanged(newTime)
            }
        })
    }

    actual fun dispose() {
        mediaPlayer.release()
    }

    actual fun toggleSound() {
    }

    actual fun toggleFullscreen() {
    }

    actual fun togglePlayStatus() {
    }
}