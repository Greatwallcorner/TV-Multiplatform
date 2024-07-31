package com.corner.ui.player.vlcj

import com.corner.bean.PlayerStateCache
import com.corner.bean.SettingStore
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.database.History
import com.corner.ui.decompose.DetailComponent
import com.corner.ui.player.MediaInfo
import com.corner.ui.player.PlayState
import com.corner.ui.player.PlayerController
import com.corner.ui.player.PlayerState
import com.corner.ui.scene.SnackBar
import com.corner.util.catch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val log = LoggerFactory.getLogger("PlayerController")

class VlcjController(val component: DetailComponent) : PlayerController {
    var player: EmbeddedMediaPlayer? = null
        private set
    private val defferredEffects = mutableListOf<(MediaPlayer) -> Unit>()

    private var isAccelerating = false
    private var originSpeed = 1.0F
    private var currentSpeed = 1.0F
    private var playerReady = false

    override var showTip = MutableStateFlow(false)
    override var tip = MutableStateFlow("")
    override var history: MutableStateFlow<History?> = MutableStateFlow(null)
    var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val vlcjArgs = listOf(
        "--no-video-title-show",           // 禁用视频标题显示
        "--no-snapshot-preview",           // 禁用快照预览
        "--no-autoscale",                  // 禁用自动缩放
        "--no-disable-screensaver",        // 禁用屏保
        "--avcodec-fast",                  // 使用快速解码模式
        "--network-caching=10000",          // 设置网络缓存为 10000ms
        "--file-caching=3000",             // 设置文件缓存为 3000ms
        "--live-caching=3000",             // 设置直播缓存为 3000ms
        "--sout-mux-caching=3000"          // 设置输出缓存为 3000ms
    )

    internal lateinit var factory:MediaPlayerFactory

    override fun doWithMediaPlayer(block: (MediaPlayer) -> Unit) {
        player?.let {
            block(it)
        } ?: run {
            defferredEffects.add(block)
        }
    }

    override fun onMediaPlayerReady(mediaPlayer: EmbeddedMediaPlayer) {
        this.player = mediaPlayer
        _state.update { it.copy(duration = player?.status()?.length() ?: 0L) }
        defferredEffects.forEach { block ->
            block(mediaPlayer)
        }
        defferredEffects.clear()
    }

    val stateListener = object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            log.info("播放器初始化完成")
            playerReady = true
            _state.update { it.copy(duration = mediaPlayer.status().length()) }
            play()
        }

        override fun videoOutput(mediaPlayer: MediaPlayer?, newCount: Int) {
            val trackInfo = mediaPlayer?.media()?.info()?.videoTracks()?.first()
            if(trackInfo != null){
                _state.update { it.copy(mediaInfo = MediaInfo(url = mediaPlayer.media()?.info()?.mrl() ?: "", height = trackInfo.height(), width = trackInfo.width())) }
            }
        }

        override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
            if(newCache != 100F){
                _state.update { it.copy(state = PlayState.BUFFERING, bufferProgression = newCache) }
            }else{
                _state.update { it.copy(state = PlayState.PLAY, bufferProgression = newCache) }
            }
        }

        override fun corked(mediaPlayer: MediaPlayer?, corked: Boolean) {
            log.debug("corked： $corked")
        }

        override fun opening(mediaPlayer: MediaPlayer?) {
            _state.update { it.copy(state = PlayState.BUFFERING) }
        }


        override fun playing(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(state = PlayState.PLAY) }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(state = PlayState.PAUSE) }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            println("stopped")
            _state.update { it.copy(state = PlayState.PAUSE) }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            println("finished")
            _state.update { it.copy(state = PlayState.PAUSE) }
            scope.launch {
                try {
                    if (checkEnd(mediaPlayer)) {
                        return@launch
                    }
                    component.nextEP()
                } catch (e: Exception) {
                    log.error("finished error", e)
                }
            }
        }

        override fun muted(mediaPlayer: MediaPlayer, muted: Boolean) {
            _state.update { it.copy(isMuted = muted) }
        }

        override fun volumeChanged(mediaPlayer: MediaPlayer, volume: Float) {
            if (volume > 0f) {
                SettingStore.doWithCache {
                    var state = it["playerState"]
                    if (state == null) {
                        state = PlayerStateCache()
                        it["playerState"] = state
                    }
                    (state as PlayerStateCache).add("volume", volume.toString())
                }
            }
            log.debug("volume:{}", volume)
            _state.update { it.copy(volume = volume) }
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            scope.launch {
                if (history.value == null) {
                    println("histiry is null")
                    return@launch
                }
                if (history.value?.ending != null && history.value?.ending != -1L && history.value?.ending!! <= newTime) component.nextEP()
                if ((newTime / 1000 % 25).toInt() == 0) history.emit(history.value?.copy(position = newTime))
            }
            _state.update { it.copy(timestamp = newTime) }
        }

        override fun error(mediaPlayer: MediaPlayer?) {
            log.error("播放错误: ${mediaPlayer?.media()?.info()?.mrl()}")
            _state.update { it.copy(state = PlayState.ERROR, msg = "播放错误") }
            component.nextFlag()
            scope.launch {
                try {
                    if (checkEnd(mediaPlayer)) {
                        return@launch
                    }
                } catch (e: Exception) {
                    log.error("error ", e)
                }
            }
            super.error(mediaPlayer)
        }

        private fun checkEnd(mediaPlayer: MediaPlayer?): Boolean {
            try {
                val len = mediaPlayer?.status()?.length() ?: 0
                println("playable: " + mediaPlayer?.status()?.isPlayable)
                if(mediaPlayer?.status()?.isPlayable == false){
                    return true
                }
//                if (len <= 0 /*|| mediaPlayer?.status()?.time() != len*/ || mediaPlayer?.status()?.isPlayable == false) {
//                    component.nextFlag()
//                    return true
//                }
                return false
            } catch (e: Exception) {
                log.error("checkEnd error:", e)
                return false
            }
        }
    }

    private val _state = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    override fun init() {
        catch {
//            dispose()
            factory = MediaPlayerFactory(vlcjArgs)
            player = factory.mediaPlayers()?.newEmbeddedMediaPlayer()?.apply {
                events().addMediaPlayerEventListener(stateListener)
                video().setScale(1.0f)
            }
        }
    }

    override fun load(url: String): PlayerController {
        log.debug("加载：$url")
        if (StringUtils.isBlank(url)) {
            SnackBar.postMsg("播放地址为空")
            return this
        }
        catch {
            player?.media()?.prepare(url, ":http-user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.33")
        }
        return this
    }

    private val stateList = listOf(State.ENDED, State.ERROR)
    override fun play() {
        catch {
            log.debug("play")
            showTips("播放")
            if (stateList.contains(player?.status()?.state())) {
                val mrl = player?.media()?.info()?.mrl()
                if (StringUtils.isNotBlank(mrl)) {
                    load(mrl!!)
                    component.syncHistory()
                } else {
                    log.error("视频播放完毕或者播放错误， 重新加载时 url为空")
                }
            }
            player?.controls()?.play()
        }
    }

    override fun play(url: String) = catch {
        showTips("播放")
        log.debug("play: $url")
        player?.media()?.play(url)
    }

    override fun pause() = catch {
        showTips("暂停")
        player?.controls()?.setPause(true)
    }

    private fun showTips(text: String) {
        runBlocking {
            tip.emit(text)
            showTip.emit(true)
        }
    }

    override fun stop() = catch {
        showTips("停止")
        player?.controls()?.stop()
    }

    override fun dispose() = catch {
        log.debug("dispose")
        stop()
        player?.release()
        factory.release()
    }

    override fun seekTo(timestamp: Long) = catch {
        _state.update { it.copy(timestamp = timestamp) }
        player?.controls()?.setTime(timestamp)
    }

    override fun setVolume(value: Float) = catch {
        player?.audio()?.setVolume((value * 100).toInt().coerceIn(0..150))
        _state.update { it.copy(volume = value) }
        showTips("音量：${player?.audio()?.volume()}")
    }

    private val volumeStep = 5

    override fun volumeUp() {
        player?.audio()?.setVolume((((player?.audio()?.volume() ?: 0) + volumeStep).coerceIn(0..150)))
        _state.update { it.copy(volume = (player?.audio()?.volume() ?: 80) / 100f) }
        showTips("音量：${player?.audio()?.volume()}")
    }

    override fun volumeDown() {
        player?.audio()?.setVolume((((player?.audio()?.volume() ?: 0) - volumeStep).coerceIn(0..150)))
        _state.update { it.copy(volume = (player?.audio()?.volume() ?: 80) / 100f) }
        showTips("音量：${player?.audio()?.volume()}")
    }

    /**
     * 快进 单位 秒
     */
    override fun forward(time: String) {
        showTips("快进：$time")
        player?.controls()?.skipTime(Duration.parse(time).toLong(DurationUnit.MILLISECONDS))
        _state.update { it.copy(timestamp = (player?.status()?.time() ?: 0)) }
    }

    override fun backward(time: String) {
        showTips("快退：$time")
        player?.controls()?.skipTime(-Duration.parse(time).toLong(DurationUnit.MILLISECONDS))
        _state.update { it.copy(timestamp = (player?.status()?.time() ?: 0)) }
    }

    override fun toggleSound() = catch {
        player?.audio()?.mute()
    }

    override fun toggleFullscreen() = catch {
        val videoFullScreen = GlobalModel.toggleVideoFullScreen()
        runBlocking {
            if (videoFullScreen) showTips("[ESC]退出全屏")
        }
    }

    override fun togglePlayStatus() {
        if (player?.status()?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    override fun speed(speed: Float) = catch {
        showTips("倍速：$speed")
        player?.controls()?.setRate(speed)
    }

    override fun stopForward() {
        isAccelerating = false
        speed(originSpeed)
    }

    override fun fastForward() {
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

    override fun updateEnding(detail: Vod?) {
        _state.update { it.copy(ending = player?.status()?.time() ?: -1) }
//        if (_state.value.ending == -1L) {
//        } else {
//            _state.update { it.copy(ending = -1) }
//        }
        history.update { it?.copy(ending = player?.status()?.time() ?: -1) }
    }

    override fun updateOpening(detail: Vod?) {
        _state.update { it.copy(opening = player?.status()?.time() ?: -1) }
//        if (_state.value.opening == -1L) {
//        } else {
//            _state.update { it.copy(opening = -1) }
//        }
        history.update { it?.copy(opening = player?.status()?.time() ?: -1) }
    }

    override fun doWithPlayState(func: (MutableStateFlow<PlayerState>) -> Unit) {
        runBlocking {
            func(_state)
        }
    }

    override fun setStartEnding(opening: Long, ending: Long) {
        _state.update { it.copy(opening = opening, ending = ending) }
    }

}
