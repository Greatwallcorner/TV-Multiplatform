package com.corner.util.play

import com.corner.ui.nav.vm.DetailViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private val log = LoggerFactory.getLogger(VideoEventServer::class.java)

// 定义全局状态
private val _webPlaybackFinishedFlow = MutableSharedFlow<Unit>(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST // 丢弃重复事件
)
val webPlaybackFinishedFlow: SharedFlow<Unit> = _webPlaybackFinishedFlow

// 新增标志位，记录当前视频是否正在播放
private var isVideoPlaying = false

object BrowserUtils {

    // 定义 WebSocket 连接状态流
    val _webSocketConnectionState = MutableStateFlow(false) // 初始状态为未连接
    val webSocketConnectionState: StateFlow<Boolean> = _webSocketConnectionState
    private val lastOpenTime = AtomicLong(0)

    // 定义一个 CoroutineScope 实例
    val scope = CoroutineScope(Dispatchers.Default)
    lateinit var detailViewModel: DetailViewModel

    // 静态 HTML 文件
    private var staticHtmlFile: File? = null

    // 静态 HTML 文件内容
    private var HTML_TEMPLATE: String? = null

    init {
        loadHtmlTemplate()
    }

    private fun loadHtmlTemplate() {
        try {
            // 读取 resources 目录下的 web_player.html 文件
            val inputStream: InputStream = javaClass.classLoader.getResourceAsStream("web_player.html")
                ?: throw IllegalArgumentException("web_player.html 文件未找到")
            HTML_TEMPLATE = inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            log.error("读取 HTML 模板文件失败", e)
        }
    }


    fun init(detailViewModel: DetailViewModel) {
        this.detailViewModel = detailViewModel
    }

    // 启动 WebSocket 服务器
    // 新增标志位，用于记录 WebSocket 服务器是否已经启动

    private val isWebSocketServerStarted = AtomicBoolean(false)

    fun startWebSocketServer() {
        if (isWebSocketServerStarted.compareAndSet(false, true)) {
            VideoEventServer.start()
        }
    }

    private val isSwitching = AtomicBoolean(false)

    /**
     * 处理切换到下一集的逻辑。
     *
     * @param vm 详情页的 ViewModel 实例，用于获取下一集的相关信息。
     */
    fun handleNextEpisode() {
        if (isSwitching.compareAndSet(false, true)) {
            try {
                // 调用切换到下一集并播放的方法
                switchToNextEpisodeAndPlay()
                // 增加延迟，防止短时间内重复处理
                Thread.sleep(1000) // 或使用协程 delay(1000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.error("处理下一集时线程被中断", e)
            } finally {
                // 重置切换标志
                isSwitching.set(false)
            }
        }
    }


    // 模拟切换到下一个选集并播放的方法
    fun switchToNextEpisodeAndPlay() {
        scope.launch {
            val nextEpisodeUrl = detailViewModel.getNextEpisodeUrl()
            nextEpisodeUrl?.let {
                // 从 viewModel 的状态中获取当前选中的剧集 URL
                val currentSelectedEpNumber = detailViewModel.currentSelectedEpNumber
                // 从状态数据里找到对应的剧集
                val currentEpisode = detailViewModel.state.value.detail.subEpisode.find { ep ->
                    ep.number == currentSelectedEpNumber
                }
                val episodeName =detailViewModel.state.value.detail.vodName ?: ""
                val episodeNumber = currentEpisode?.number
                openBrowserWithHtml(it, episodeName, episodeNumber)
            }
        }
    }

    // 启动监听
    fun startListening() {
        // 使用自定义的 CoroutineScope 实例启动协程
        scope.launch {
            webPlaybackFinishedFlow.collect {
                handleNextEpisode()
            }
        }
    }

    // 定义一个接口来获取当前时间
    interface TimeProvider {
        fun currentTimeMillis(): Long
    }

    // 默认实现使用 System.currentTimeMillis()
    class DefaultTimeProvider : TimeProvider {
        override fun currentTimeMillis(): Long = System.currentTimeMillis()
    }


    /**
     * 使用浏览器打开包含指定 M3U8 视频链接的 HTML 文件。
     * 该方法会复用静态 HTML 文件，仅更新文件中的 M3U8 链接，
     * 并使用系统默认浏览器打开该文件。同时会进行防重复打开检查，
     * 并确保 WebSocket 服务器已启动且监听已开启。
     *
     * @param m3u8Url 要播放的 M3U8 视频链接。
     * @param episodeName 剧集名称。
     * @param episodeNumber 集数。
     */
    fun openBrowserWithHtml(
        m3u8Url: String,
        episodeName: String? = null,
        episodeNumber: Int? = null,
        timeProvider: TimeProvider = DefaultTimeProvider()
    ) {
        // 获取当前时间戳
        val now = timeProvider.currentTimeMillis()
        // 检查距离上次打开浏览器是否不足 1 秒，若是则直接返回，避免短时间内重复打开
        if (now - lastOpenTime.get() < 1000) return

        // 使用 CAS 操作更新上次打开时间，确保操作的原子性
        if (lastOpenTime.compareAndSet(lastOpenTime.get(), now)) {
            // 启动 WebSocket 服务器，用于接收浏览器发送的视频播放事件
            startWebSocketServer()
            // 启动监听，当接收到视频播放完成事件时处理下一集
            startListening()
            // 检查当前系统是否支持桌面操作，并且支持使用浏览器打开链接
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    if (staticHtmlFile == null) {
                        // 创建一个临时目录用于存放 HTML 文件
                        val tempDir = Files.createTempDirectory("m3u8-player").toFile()
                        // 程序退出时删除临时目录
                        tempDir.deleteOnExit()

                        // 创建静态 HTML 文件
                        staticHtmlFile = File(tempDir, "player.html").apply {
                            writeText(HTML_TEMPLATE ?: "")
                            // 程序退出时删除临时 HTML 文件
                            deleteOnExit()
                        }
                    }

                    // 更新 HTML 文件中的 M3U8 链接和视频标题
                    val videoTitle = if (episodeNumber != null && episodeNumber != -1) {
                        "$episodeName 第 $episodeNumber 集"
                    } else {
                        episodeName ?: ""
                    }

                    // 更新 HTML 文件中的 M3U8 链接、剧集名称和集数
                    val updatedHtmlContent = HTML_TEMPLATE
                        ?.replace("%M3U8_URL%", m3u8Url)
                        ?.replace("%VIDEO_TITLE%", videoTitle)

                    updatedHtmlContent?.let { staticHtmlFile?.writeText(it) }

                    // 使用系统默认浏览器打开临时 HTML 文件
                    Desktop.getDesktop().browse(staticHtmlFile!!.toURI())
                    // 记录使用浏览器打开本地 HTML 文件的日志
                    log.info("使用浏览器打开本地 HTML 文件: {}", staticHtmlFile!!.absolutePath)
                } catch (e: Exception) {
                    // 捕获异常并打印堆栈信息
                    e.printStackTrace()
                }
            }
        }
    }

    fun cleanup() {
        // 检查WebSocket服务器是否正在运行
        if (VideoEventServer.isRunning()) {
            VideoEventServer.stop()
            log.info("WebSocket 服务器已停止")
        } else {
            log.info("WebSocket 服务器未运行，跳过停止操作")
        }
    }
}

// WebSocket 服务器实现
object VideoEventServer : WebSocketServer(InetSocketAddress("localhost", 8888)) {
    // 添加状态标记
    private var _isRunning = AtomicBoolean(false)

    // 提供公共方法检查运行状态
    fun isRunning(): Boolean = _isRunning.get()

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        log.debug("WebSocket 连接已建立")
        // 连接建立后，重置播放状态
        isVideoPlaying = false
        // 连接建立后，更新 WebSocket 连接状态为已连接
        BrowserUtils._webSocketConnectionState.value = true
//        log.debug("WebSocket 连接状态：{}",BrowserUtils._webSocketConnectionState.value)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        log.debug("WebSocket 连接关闭，关闭码: {}, 关闭原因: {}, 是否远程关闭: {}", code, reason, remote)
        // 若视频正在播放，说明是中途退出，更新 WebSocket 连接状态为未连接
        if (isVideoPlaying) {
            BrowserUtils._webSocketConnectionState.value = false
            log.debug("更新WebSocket 连接状态：{}",BrowserUtils._webSocketConnectionState.value)
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        log.info("收到消息: {}", message)
        // 在这里处理来自浏览器的消息（如播放开始、播放完成事件）
        when (message) {
            "PLAYBACK_STARTED" -> {
                log.info("视频播放开始！")
                // 设置视频正在播放标志
                isVideoPlaying = true
            }
            "PLAYBACK_FINISHED" -> {
                log.info("视频播放完成！")
                // 重置视频正在播放标志
                isVideoPlaying = false
                // 使用自定义的 CoroutineScope 实例启动协程
                BrowserUtils.scope.launch {
                    _webPlaybackFinishedFlow.emit(Unit)
                }
                log.info("当前视频播放完成，尝试切换下一集...")
            }
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        log.error("WebSocket 发生错误", ex)
    }

    // 重写 onStart 方法
    override fun onStart() {
        _isRunning.set(true)
        log.info("WebSocket 服务器已启动")
    }

    // 重写 stop 方法
    override fun stop() {
        super.stop()
        _isRunning.set(false)
    }
}