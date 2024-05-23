package com.corner.ui.player.vlcj

import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.player.PlayerController
import com.corner.ui.player.PlayerState
import com.corner.ui.scene.SnackBar
import com.corner.util.catch
import com.sun.jna.NativeLibrary.addSearchPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val log = LoggerFactory.getLogger("PlayerController")

class VlcjController(component: DetailComponent) : PlayerController {
    private var player:MediaPlayer? = null
    private val defferredEffects = mutableListOf<(MediaPlayer) -> Unit>()

    private var isAccelerating = false
    private var originSpeed = 1.0F
    private var currentSpeed = 1.0F
    private var playerReady = false

    override fun doWithMediaPlayer(block: (MediaPlayer) -> Unit){
        player?.let {
            block(it)
        }?:run {
            defferredEffects.add(block)
        }
    }

    override fun onMediaPlayerReady(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer) {
        this.player = mediaPlayer
        _state.update { it.copy(duration = player?.status()?.length() ?: 0L) }
        defferredEffects.forEach { block ->
            block(mediaPlayer)
        }
        defferredEffects.clear()
    }

    init {
        addSearchPath(
            RuntimeUtil.getLibVlcLibraryName(),
            Paths.get(System.getProperty("user.dir"), "lib", "libvlc.dll").pathString
        )
        NativeDiscovery().discover()
    }

    val stateListener = object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            log.info("播放器初始化完成")
            playerReady = true
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
            component.nextEP()
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

        override fun error(mediaPlayer: MediaPlayer?) {
            log.error("播放错误: ${mediaPlayer?.media()?.info()?.mrl()}")
            SnackBar.postMsg("播放错误")
            super.error(mediaPlayer)
        }
    }

    private val _state = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    override fun init(){
    }

    override fun load(url: String): PlayerController {
        log.debug("加载：$url")
        catch {
            player?.apply {
                media().prepare(url)
            }
        }
        return this
    }

    override fun play() = catch {
        log.debug("play")
        player?.controls()?.play()
    }

    override fun play(url:String) = catch {
        log.debug("play: $url")
        player?.media()?.play(url)
    }

    override fun pause() = catch {
        player?.controls()?.setPause(true)
    }

    override fun stop() = catch {
        player?.controls()?.stop()
    }

    override fun dispose() = catch {
        log.debug("dispose")
        player?.run {
            controls().stop()
            events().removeMediaPlayerEventListener(stateListener)
            release()
        }
    }

    override fun seekTo(timestamp: Long) = catch {
        player?.controls()?.setTime(timestamp)
    }

    override fun setVolume(value: Float) = catch {
        player?.audio()?.setVolume((value * 100).toInt().coerceIn(0..100))
    }

    private val volumeStep = 5

    fun volumeUp() {
        player?.audio()?.setVolume(((player?.audio()?.volume() ?: 0) + volumeStep).coerceIn(0..100))
    }

    fun volumeDown() {
        player?.audio()?.setVolume(((player?.audio()?.volume() ?: 0) - volumeStep).coerceIn(0..100))
    }

    /**
     * 快进 单位 秒
     */
    fun forward(time: Long = 15L) {
        player?.controls()?.skipTime(Duration.parse("15s").toLong(DurationUnit.MILLISECONDS))
    }

    fun backward(time: Long = 15L) {
        player?.controls()?.skipTime(-Duration.parse("15s").toLong(DurationUnit.MILLISECONDS))
    }

    override fun toggleSound() = catch {
        player?.audio()?.mute()
    }

    override fun toggleFullscreen() = catch {
        GlobalModel.toggleVideoFullScreen()
    }

    override fun togglePlayStatus() {
        if (player?.status()?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    override fun speed(speed: Float) = catch {
        player?.controls()?.setRate(speed)
    }
    fun stopForward() {
        isAccelerating = false
        speed(originSpeed)
    }

    fun fastForward() {
        if (!isAccelerating) {
            currentSpeed = player?.status()?.rate() ?: 1.0f
            originSpeed = currentSpeed.toDouble().toFloat()
            isAccelerating = true
        }
        acceleratePlayback()
    }

    private val maxSpeed = 8.0f

    private fun acceleratePlayback() {
        if (isAccelerating) {
            currentSpeed += 0.5f
            currentSpeed = Math.min(currentSpeed, maxSpeed)
            speed(currentSpeed)
            println("Playback rate: $currentSpeed x")
        }
    }

}
