package com.corner.ui.player.vlcj

import androidx.compose.ui.window.WindowPlacement
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.player.PlayerController
import com.corner.ui.player.PlayerState
import com.corner.util.catch
import com.sun.jna.NativeLibrary.addSearchPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.Logo
import uk.co.caprica.vlcj.player.base.LogoPosition
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.fullscreen.FullScreenStrategy
import java.awt.Color
import java.awt.Component
import java.awt.event.*
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val log = LoggerFactory.getLogger("PlayerController")

class VlcjController : PlayerController {

    private var isAccelerating = false
    private var originSpeed = 1.0F
    private var currentSpeed = 1.0F
    private var playerReady = false

    init {
        addSearchPath(
            RuntimeUtil.getLibVlcLibraryName(),
            Paths.get(System.getProperty("user.dir"), "lib", "libvlc.dll").pathString
        )
        NativeDiscovery().discover()
    }

    private fun initializeMediaPlayerComponent(): Component {
        NativeDiscovery().discover()
        return if (RuntimeUtil.isMac()) {
            CallbackMediaPlayerComponent()
        } else {
            EmbeddedMediaPlayerComponent()
        }
    }

//    internal val factory by lazy { MediaPlayerFactory() }

    val component: Component = init(initializeMediaPlayerComponent())
//    private val surface by lazy { factory.videoSurfaces().newVideoSurface(component) }

    private val stateListener = object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            log.info("播放器初始化完成")
            playerReady = true
            catch {
                mediaPlayer.audio().setVolume(50)
//                play()
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
    internal var player: EmbeddedMediaPlayer? = init(component.mediaPlayer())
        private set

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
        player?.fullScreen()?.toggle()
        state.value.isFullScreen
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

    private fun Component.mediaPlayer() = when (this) {
        is CallbackMediaPlayerComponent -> mediaPlayer()
        is EmbeddedMediaPlayerComponent -> mediaPlayer()
        else -> error("mediaPlayer() can only be called on vlcj player components")
    }

    fun init(player:EmbeddedMediaPlayer): EmbeddedMediaPlayer {
        log.debug("player init")
        player.events()?.addMediaPlayerEventListener(stateListener)
        player.logo().set(
            Logo.logo()
                .file(Utils::class.java.getResource("pic/TV-icon-s.png")?.toString() ?: "")
                .position(LogoPosition.CENTRE)
                .opacity(0.5f)
                .enable()
        )

        if (RuntimeUtil.isWindows()) {
            player.input().enableKeyInputHandling(false)
            player.input().enableMouseInputHandling(false)
        }
        player.fullScreen().strategy(object : FullScreenStrategy {
            override fun enterFullScreenMode() {
                state.value.isFullScreen = true
                GlobalModel.windowState?.placement = WindowPlacement.Fullscreen
            }

            override fun exitFullScreenMode() {
                state.value.isFullScreen = false
                GlobalModel.windowState?.placement = WindowPlacement.Floating
            }

            override fun isFullScreenMode(): Boolean {
                return GlobalModel.windowState?.placement == WindowPlacement.Fullscreen
            }

        })

        return player
    }

    fun init(comp: Component):Component {
        log.debug("component init")
        var component:Component? = null
        when(comp){
            is CallbackMediaPlayerComponent ->
                component = (comp as CallbackMediaPlayerComponent).videoSurfaceComponent()
            is EmbeddedMediaPlayerComponent ->
                component = (comp as EmbeddedMediaPlayerComponent).videoSurfaceComponent()
        }
        component?.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                if ((e?.clickCount ?: 1) < 2) return
                togglePlayStatus()
            }

            override fun mousePressed(e: MouseEvent?) {
            }

            override fun mouseReleased(e: MouseEvent?) {
            }

            override fun mouseEntered(e: MouseEvent?) {
                println("mouseEntered")
                component.requestFocus()
            }

            override fun mouseExited(e: MouseEvent?) {
            }

        })

        // 音量
        component?.addMouseWheelListener { e ->
            if (e?.scrollType == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                if (e.wheelRotation < 0) {
                    volumeUp()
                } else {
                    volumeDown()
                }
            }
        }
        component?.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {
                /*if (e?.keyCode == KeyEvent.VK_RIGHT) {
                    println("keyType right")
                    controller.forward()
                } else if (e?.keyCode == KeyEvent.VK_LEFT) {
                    println("keyType right")
                    controller.backward()
                } else */if (e?.keyChar?.code == KeyEvent.VK_SPACE) {
                    togglePlayStatus()
                }
            }

            override fun keyPressed(e: KeyEvent?) {
                log.debug("keyPressed")
                if (e?.keyCode == KeyEvent.VK_RIGHT) {
                    // 按下右键开始加速
                    if (!isAccelerating) {
                        currentSpeed = player?.status()?.rate() ?: 1.0f
                        originSpeed = currentSpeed.toDouble().toFloat()
                        isAccelerating = true
                    }
                    acceleratePlayback()
                } else if (e?.keyCode == KeyEvent.VK_LEFT) {
                    if (!isAccelerating) {
                        currentSpeed = player?.status()?.rate() ?: 1.0f
                        originSpeed = currentSpeed.toDouble().toFloat()
                        isAccelerating = true
                    }
                }
            }

            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_RIGHT) {
                    isAccelerating = false
                    speed(originSpeed)
                }
            }
        })
        component?.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) {
                // 当组件可见时,设置背景颜色
                setBackgroundColor(Color.BLUE)
            }

            override fun componentHidden(e: ComponentEvent) {
                // 当组件不可见时,设置背景颜色
                setBackgroundColor(Color.DARK_GRAY)
            }

            private fun setBackgroundColor(color: Color) {
                component.setBackground(color)
            }

        })
        return comp
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
