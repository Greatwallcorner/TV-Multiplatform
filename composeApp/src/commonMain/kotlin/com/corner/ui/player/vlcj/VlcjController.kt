package com.corner.ui.player.vlcj

import com.corner.bean.PlayerStateCache
import com.corner.bean.SettingStore
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.database.entity.History
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.player.MediaInfo
import com.corner.ui.player.PlayState
import com.corner.ui.player.PlayerController
import com.corner.ui.player.PlayerLifecycleManager
import com.corner.ui.player.PlayerState
import com.corner.util.catch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.swing.Swing
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.io.File
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val log = LoggerFactory.getLogger("VlcjController")

class VlcjController(val vm: DetailViewModel) : PlayerController {

    private var lifecycleManager: PlayerLifecycleManager? = null

    fun setLifecycleManager(manager: PlayerLifecycleManager) {
        this.lifecycleManager = manager
    }

    var player: EmbeddedMediaPlayer? = null
    private val defferredEffects = mutableListOf<(MediaPlayer) -> Unit>()

    private var isAccelerating = false
    private var originSpeed = 1.0F
    private var currentSpeed = 1.0F
    override var playerReady = false

    override var playerLoading = false

    override var playerPlayering = false

    override var showTip = MutableStateFlow(false)
    override var tip = MutableStateFlow("")

    override var history: MutableStateFlow<History?> = MutableStateFlow(null)

    var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var lastVolume = -1f
    private val VOLUME_THRESHOLD = 0.02f // 提高阈值到2%
    private val LOG_DEBOUNCE_MS = 500L // 日志防抖

    private var cleanupJob: Job? = null  // 添加清理任务跟踪变量

    companion object {
        // 全局标志：插件缓存是否已检查
        private var pluginCacheChecked = false
        private const val VOLUME_LOG_THRESHOLD = 0.05f  // 音量变化阈值
        private const val VOLUME_LOG_DEBOUNCE_MS = 1000L  // 日志防抖时间
    }

    private var lastLoggedVolume = -1f
    private var lastVolumeLogTime = 0L

    // 添加清理状态标志
    private var isCleaned = false

    private val vlcjArgs = mutableListOf<String>(
        "-v",
        "--no-video-on-top",                // 禁用窗口置顶
        ":network-caching=500",            // 设置网络缓存为 单位ms
        ":live-caching=300",               // 减少直播缓存
        ":http-timeout=5000",              // 设置HTTP超时时间 单位ms
        ":tcp-timeout=3000"                // 设置TCP超时时间 单位ms
//        "--file-caching=500",             // 设置文件缓存为
//        "--live-caching=500",             // 设置直播缓存为
//        "--sout-mux-caching=500"          // 设置输出缓存为
    )

    override fun resetOpeningEnding() {
        _state.update { it.copy(opening = -1L, ending = -1L) }
        history.update { it?.copy(opening = -1L, ending = -1L) }
    }

    internal lateinit var factory: MediaPlayerFactory

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

    private val stateListener = object : MediaPlayerEventAdapter() {

        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            log.info("播放器初始化完成")
            playerReady = true
            _state.update { it.copy(duration = mediaPlayer.status().length(), state = PlayState.PLAY) }
            play()
        }

        override fun videoOutput(mediaPlayer: MediaPlayer?, newCount: Int) {
            val trackInfo = mediaPlayer?.media()?.info()?.videoTracks()?.first()
            if (trackInfo != null) {
                _state.update {
                    it.copy(
                        mediaInfo = MediaInfo(
                            url = mediaPlayer.media()?.info()?.mrl() ?: "",
                            height = trackInfo.height(),
                            width = trackInfo.width()
                        )
                    )
                }
            }
        }

        override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
            if (newCache != 100F) {
                _state.update { it.copy(state = PlayState.BUFFERING, bufferProgression = newCache) }
            } else {
                _state.update { it.copy(state = PlayState.PLAY, bufferProgression = newCache) }
            }
        }

        override fun corked(mediaPlayer: MediaPlayer?, corked: Boolean) {
            log.debug("corked： $corked")
        }

        override fun opening(mediaPlayer: MediaPlayer?) {
            playerLoading = true
            _state.update { it.copy(state = PlayState.BUFFERING) }
        }


        override fun playing(mediaPlayer: MediaPlayer) {
            playerLoading = false
            playerPlayering = true
            _state.update { it.copy(state = PlayState.PLAY) }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(state = PlayState.PAUSE) }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            log.info("stopped")
            playerPlayering = false
            _state.update { it.copy(state = PlayState.PAUSE) }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            log.info("finished")
            playerPlayering = false
            _state.update { it.copy(state = PlayState.PAUSE) }
            scope.launch {
                try {
                    if (checkEnd(mediaPlayer)) {
                        return@launch
                    }
                    vm.nextEP()
                } catch (e: Exception) {
                    log.error("finished error", e)
                }
            }
        }

        override fun muted(mediaPlayer: MediaPlayer, muted: Boolean) {
            _state.update { it.copy(isMuted = muted) }
        }

        override fun volumeChanged(mediaPlayer: MediaPlayer, volume: Float) {
            // 防抖处理：检查音量变化阈值和时间间隔
            val currentTime = System.currentTimeMillis()
            if (abs(volume - lastLoggedVolume) < VOLUME_LOG_THRESHOLD &&
                currentTime - lastVolumeLogTime < VOLUME_LOG_DEBOUNCE_MS) {
                return
            }

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

            // 只在音量显著变化时记录日志
            if (abs(volume - lastLoggedVolume) >= VOLUME_LOG_THRESHOLD) {
                log.debug("volume:{}", volume)
                lastLoggedVolume = volume
                lastVolumeLogTime = currentTime
            }

            _state.update { it.copy(volume = volume) }
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            scope.launch {
                if (history.value == null) {
//                    println("history is null")
                    return@launch
                }
                if (history.value?.ending != null && history.value?.ending != -1L && history.value?.ending!! <= newTime) vm.nextEP()
                if ((newTime / 1000 % 25).toInt() == 0) history.emit(history.value?.copy(position = newTime))
            }
            _state.update { it.copy(timestamp = newTime) }
        }


        override fun error(mediaPlayer: MediaPlayer?) {
            log.error("播放错误: ${mediaPlayer?.media()?.info()?.mrl()}")
            _state.update { it.copy(state = PlayState.ERROR, msg = "播放错误") }
            vm.nextFlag()
            scope.launch {
                history.value?.let { vm.updateHistory(it) }
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
                log.info("playable: " + mediaPlayer?.status()?.isPlayable)
                if (mediaPlayer?.status()?.isPlayable == false) {
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
        isCleaned = false
        // 仅在第一次初始化时检查插件缓存
        if (!pluginCacheChecked) {
            // 添加插件缓存检查和重建逻辑
            val vlcDir = File("vlcdir")
            val pluginsDir = File(vlcDir, "plugins")
            val pluginCacheFile = File(pluginsDir, "plugins.dat")

            // 添加缓存检查日志
            log.info("[VLC Cache Check] 插件缓存路径: ${pluginCacheFile.absolutePath}")
            log.info("[VLC Cache Check] 缓存文件存在: ${pluginCacheFile.exists()}")

            // 检查缓存是否存在或需要重置
            if (!pluginCacheFile.exists()) {
                log.warn("[VLC Cache Check] 缓存文件不存在，添加 --reset-plugins-cache 参数")
                vlcjArgs.add("--reset-plugins-cache")
                // 确保插件目录存在
                if (pluginsDir.mkdirs()) {
                    log.info("[VLC Cache Check] 插件目录已创建: ${pluginsDir.absolutePath}")
                } else {
                    log.warn("[VLC Cache Check] 插件目录已存在或创建失败: ${pluginsDir.absolutePath}")
                }
            } else {
                log.info("[VLC Cache Check] 缓存文件存在，无需重建")
            }

            // 标记已检查过插件缓存
            pluginCacheChecked = true
        } else {
            log.info("[VLC Cache Check] 插件缓存已检查过，跳过")
        }

        try {
            factory = MediaPlayerFactory(vlcjArgs)
            player = factory.mediaPlayers()?.newEmbeddedMediaPlayer()?.apply {
                events().addMediaPlayerEventListener(stateListener)
                video().setScale(0.0f)
            }
        } catch (e: Exception) {
            // 处理异常
            // dispose()
            log.error("vlcj初始化失败", e)
        }
    }

    // 添加异步初始化方法
    override suspend fun initAsync() = withContext(Dispatchers.IO) {
        try {
            isCleaned = false
            // 仅在第一次初始化时检查插件缓存
            if (!pluginCacheChecked) {
                val vlcDir = File("vlcdir")
                val pluginsDir = File(vlcDir, "plugins")
                val pluginCacheFile = File(pluginsDir, "plugins.dat")

                if (!pluginCacheFile.exists()) {
                    vlcjArgs.add("--reset-plugins-cache")
                    pluginsDir.mkdirs()
                }
                pluginCacheChecked = true
            }

            factory = MediaPlayerFactory(vlcjArgs)
            player = factory.mediaPlayers()?.newEmbeddedMediaPlayer()?.apply {
                events().addMediaPlayerEventListener(stateListener)
                video().setScale(0.0f)
            }
        } catch (e: Exception) {
            log.error("vlcj异步初始化失败", e)
            throw e
        }
    }

    /**
     * 改进的异步清理方法，确保线程安全和资源完整释放
     */
    override suspend fun cleanupAsync() = withContext(Dispatchers.IO) {
        if (isCleaned) return@withContext
        isCleaned = true
        try {
            log.debug("开始异步清理播放器资源")

            player?.let { p ->
                try {
                    // 快速停止，不等待缓冲完成
                    p.controls()?.stop()
                } catch (e: Exception) {
                    log.warn("停止播放时出错", e)
                }
            }

            // 异步清理，不等待
            scope.cancel("播放器清理")
            defferredEffects.clear()

            log.debug("异步清理完成")
        } catch (e: Exception) {
            log.warn("清理超时")
        }
    }

    override fun load(url: String): PlayerController {
        log.debug("加载：$url")
        if (StringUtils.isBlank(url)) {
            SnackBar.postMsg("播放地址为空", type = SnackBar.MessageType.WARNING)
            return this
        }

        val optionsList = mutableListOf("http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0")

        catch {
            player?.media()?.prepare(url, *buildList {
                add("http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0")
            }.toTypedArray())
        }
        return this
    }

    // 添加带超时的异步加载方法
    override suspend fun loadAsync(url: String, timeoutMillis: Long): PlayerController = withContext(Dispatchers.IO) {
        // 确保清理完成
        if (isCleaned) {
            cleanupJob?.join() // 等待清理完成
        }

        if (StringUtils.isBlank(url)) {
            withContext(Dispatchers.Swing) {
                SnackBar.postMsg("播放地址为空", type = SnackBar.MessageType.WARNING)
            }
            return@withContext this@VlcjController
        }

        try {
            val optionsList = mutableListOf("http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:136.0) Gecko/20100101 Firefox/136.0")

            player?.media()?.prepare(url, *optionsList.toTypedArray())

        } catch (e: TimeoutCancellationException) {
            log.error("媒体加载超时: ${e.message}")
            withContext(Dispatchers.Swing) {
                SnackBar.postMsg("媒体加载超时，请检查网络连接", type = SnackBar.MessageType.WARNING)
            }
        } catch (e: Exception) {
            log.error("媒体加载失败", e)
            withContext(Dispatchers.Default) {
                SnackBar.postMsg("媒体加载失败: ${e.message}", type = SnackBar.MessageType.ERROR)
            }
        }

        this@VlcjController
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
                    vm.syncHistory()
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

    fun showTips(text: String) {
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
        log.debug("dispose - 释放播放器资源")
        stop()
        player?.events()?.removeMediaPlayerEventListener(stateListener)
        player?.release()
        factory.release()
        player = null
        log.debug("dispose - 释放成功")
    }

    override fun seekTo(timestamp: Long) = catch {
        _state.update { it.copy(timestamp = timestamp) }
        player?.controls()?.setTime(timestamp)
    }

    override fun setVolume(value: Float) = catch {
        val now = System.currentTimeMillis()

        // 1. 音量防抖：只有当音量变化超过阈值时才设置
        if (abs(value - lastVolume) > VOLUME_THRESHOLD) {
            lastVolume = value
            player?.audio()?.setVolume((value * 100).toInt().coerceIn(0..150))
            _state.update { it.copy(volume = value) }

            // 2. 日志防抖：限制日志输出频率
            if (now - lastVolumeLogTime > LOG_DEBOUNCE_MS) {
                showTips("音量：${player?.audio()?.volume()}")
                lastVolumeLogTime = now
            }
        }
    }

    private val volumeStep = 5

    override fun volumeUp() {
        val currentVolume = player?.audio()?.volume() ?: 0
        val newVolume = (currentVolume + volumeStep).coerceIn(0..150)
        player?.audio()?.setVolume(newVolume)
        _state.update { it.copy(volume = newVolume / 100f) }
        showTips("音量：$newVolume")
    }

    override fun volumeDown() {
        val currentVolume = player?.audio()?.volume() ?: 0
        val newVolume = (currentVolume - volumeStep).coerceIn(0..150)
        player?.audio()?.setVolume(newVolume)
        _state.update { it.copy(volume = newVolume / 100f) }
        showTips("音量：$newVolume")
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
        val videoFullScreen = GlobalAppState.toggleVideoFullScreen()
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
            log.info("Playback rate: $currentSpeed x")
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
    /**
     * 清理播放器资源
     */
    fun cleanup() {

        if (isCleaned) {
            log.debug("控制器已清理，跳过重复清理")
            return
        }
        try {
            player?.let { p ->
                // 1. 先停止播放
                try {
                    p.controls()?.stop()
                } catch (e: Exception) {
                    log.warn("停止播放失败", e)
                }

                // 2. 只在播放器未释放时释放
                try {
                    p.release()
                } catch (e: Exception) {
                    log.warn("释放播放器失败", e)
                }
            }

            player = null
            isCleaned = true

            // 4. 清理协程作用域
            scope.cancel()

            // 5. 清理缓存和回调
            defferredEffects.clear()

            log.debug("VlcjController cleanup completed")
        } catch (e: Exception) {
            log.error("Cleanup error", e)
        }
    }

    suspend fun safePlayerOperation(
        operation: suspend () -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        try {
            operation()
        } catch (e: Exception) {
            log.error("播放器操作失败", e)
            withContext(Dispatchers.Swing) {
                SnackBar.postMsg("播放器操作失败: ${e.message}", type = SnackBar.MessageType.ERROR)
            }
            onError(e)
        }
    }

    override fun setAspectRatio(aspectRatio: String) = catch {
        player?.video()?.setAspectRatio(aspectRatio)
        _state.update { it.copy(aspectRatio = aspectRatio) }
        showTips("视频比例: ${getAspectRatioDisplayName(aspectRatio)}")
    }

    override fun getAspectRatio(): String {
        return player?.video()?.aspectRatio() ?: ""
    }

    private fun getAspectRatioDisplayName(ratio: String): String {
        return when (ratio) {
            "16:9" -> "16:9"
            "4:3" -> "4:3"
            "1:1" -> "1:1"
            "16:10" -> "16:10"
            "21:9" -> "21:9"
            "2.35:1" -> "2.35:1"
            "2.39:1" -> "2.39:1"
            "5:4" -> "5:4"
            "" -> "原始比例"
            else -> ratio
        }
    }
}