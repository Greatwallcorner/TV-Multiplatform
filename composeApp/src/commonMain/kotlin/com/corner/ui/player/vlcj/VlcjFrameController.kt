package com.corner.ui.player.vlcj

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.corner.database.entity.History
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.player.BitmapPool
import com.corner.ui.player.PlayerController
import com.corner.ui.player.PlayerLifecycleManager
import com.corner.ui.player.frame.FrameRenderer
import com.corner.util.BrowserUtils.scope
import com.corner.util.thisLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.roundToInt


class VlcjFrameController(
    component: DetailViewModel,
    private val controller: VlcjController = VlcjController(component),
    private val bitmapPool: BitmapPool = BitmapPool(3)
) : FrameRenderer, PlayerController by controller {
    private val log = thisLogger()
    private var byteArray: ByteArray? = null
    private var info: ImageInfo? = null
    val imageBitmapState: MutableState<ImageBitmap?> = mutableStateOf(null)

    @Volatile
    private var isReleased = false

    // 添加公开的getter方法
    fun isReleased(): Boolean = isReleased

    private var historyCollectJob: Job? = null

    private val _size = MutableStateFlow(0 to 0)
    override val size = _size.asStateFlow()

    private val _bytes = MutableStateFlow<ByteArray?>(null)
    override val bytes = _bytes.asStateFlow()

    private var currentBitmap: Bitmap? = null
    private val pendingRelease = ConcurrentLinkedQueue<Bitmap>()
    private val callbackSurFace = CallbackVideoSurface(
        object : BufferFormatCallback {

            fun estimateFrameRate(width: Int, height: Int): Int {
                val pixels = width * height
                return when {
                    pixels >= 3_000_000 -> 60 // 高分辨率推高帧率（如2K/4K）
                    pixels >= 1_000_000 -> 30 // 主流1080p
                    else -> 24                // 标清或低码率
                }
            }

            private var lastPoolSize = -1
            private var lastWidth = -1
            private var lastHeight = -1

            private fun adjustBitmapPoolSize(width: Int, height: Int) {
                if (width == lastWidth && height == lastHeight) return

                val resolutionFactor = (width * height) / 1_000_000f
                val frameRate = estimateFrameRate(width, height)
                val poolSize = (frameRate * resolutionFactor).roundToInt().coerceIn(2, 12)

                if (poolSize != lastPoolSize) {
                    bitmapPool.setMaxSize(poolSize)
                    log.info("根据 ${frameRate}fps @ ${width}x$height，调整 BitmapPool 大小为 $poolSize")
                    lastPoolSize = poolSize
                }

                lastWidth = width
                lastHeight = height
            }


            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                info = ImageInfo.makeN32(sourceWidth, sourceHeight, ColorAlphaType.OPAQUE)
                adjustBitmapPoolSize(width = sourceWidth, height = sourceHeight)
                return RV32BufferFormat(sourceWidth, sourceHeight)
            }


            override fun newFormatSize(bufferWidth: Int, bufferHeight: Int, displayWidth: Int, displayHeight: Int) {
            }

            override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {
                byteArray = ByteArray(buffers[0].limit())
            }
        }, object : RenderCallback {
            override fun lock(mediaPlayer: MediaPlayer?) {
            }

            override fun display(
                mediaPlayer: MediaPlayer,
                nativeBuffers: Array<out ByteBuffer>,
                bufferFormat: BufferFormat,
                displayWidth: Int,
                displayHeight: Int
            ) {
                val width = bufferFormat.width
                val height = bufferFormat.height
                val byteBuffer = nativeBuffers[0]

                byteBuffer.get(byteArray)
                byteBuffer.rewind()

                // 从池中获取 Bitmap（复用或新建）
                val bmp = bitmapPool.acquire(width, height)
                bmp.installPixels(byteArray)

                currentBitmap?.let {
                    pendingRelease.add(it)
                }

                releasePendingBitmaps()

                currentBitmap = bmp
                imageBitmapState.value = bmp.asComposeImageBitmap()

            }

            override fun unlock(mediaPlayer: MediaPlayer?) {
            }
        }, true,
        VideoSurfaceAdapters.getVideoSurfaceAdapter()
    )

    private fun releasePendingBitmaps() {
        while (pendingRelease.isNotEmpty()) {
            val bitmap = pendingRelease.poll()
            if (!bitmap.isClosed) {
                bitmapPool.release(bitmap)
            }
        }
    }

    /**
     * 加载视频URL。
     * 该方法会异步加载视频URL。
     * 若加载过程中发生异常，会记录错误日志。
     * 加载完成后，会自动播放视频并根据历史记录设置播放速度和位置。
     * 若历史记录中没有位置信息，会默认从视频开头开始播放。
     *
     * @param url 视频URL字符串。
     * @return 返回当前的FrameRenderer实例，用于链式调用。
     */

    override fun load(url: String): PlayerController {
        scope.launch {
            controller.loadAsync(url, 1000) // suspend 函数，自动挂起直到完成
            speed(controller.history.value?.speed?.toFloat() ?: 1f)
            controller.stop()
            controller.play()
            seekTo(max(controller.history.value?.position ?: 0L, history.value?.opening ?: 0L))
        }
        return controller
    }

    override fun vlcjFrameInit() {
        log.info("播放器初始化")

        // 添加窗口绑定检查
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { doInit() }
        } else {
            doInit()
        }
    }

    private fun doInit() {
        try {
            val lifecycleManager = PlayerLifecycleManager(controller, scope)
            controller.setLifecycleManager(lifecycleManager)
            controller.init()

            controller.player?.videoSurface()?.set(callbackSurFace)

            isReleased = false
        } catch (e: Exception) {
            log.error("视频表面初始化失败", e)
            // 回退到独立窗口模式
        }
    }

    fun isPlaying(): Boolean {
        return !isReleased && controller.player?.status()?.isPlayable == true && controller.player?.status()?.isPlaying == true
    }

    fun setStartEnd(opening: Long, ending: Long) {
        controller.setStartEnding(opening, ending)
    }

    fun setControllerHistory(history: History) {
        controller.scope.launch {
            controller.history.emit(history)
        }
        if (historyCollectJob != null) return
        historyCollectJob = controller.scope.launch {
            delay(10)
            controller.history.collect {
                if (it != null) {
                    controller.vm.updateHistory(it)
                }
            }
        }
    }

    fun getControllerHistory(): History? {
        return controller.history.value
    }

    fun doWithHistory(func: (History) -> History) {
        runBlocking {
            if (controller.history.value == null) return@runBlocking
            controller.history.emit(func(controller.history.value!!))
        }
    }

    fun getPlayer(): MediaPlayer? {
        return controller.player
    }

    fun release() {
        if (isReleased) {
            log.debug("播放器已释放，跳过重复释放")
            return
        }

        synchronized(this) {
            if (isReleased) return
            isReleased = true

            try {
                log.debug("开始释放播放器资源")

                // 确保在Swing线程中执行释放操作
                if (!SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeAndWait {
                        doRelease()
                    }
                } else {
                    doRelease()
                }

                log.debug("资源释放成功")
            } catch (e: Throwable) {
                log.error("释放播放器资源时出错：", e)
            }
        }
    }

    private fun doRelease() {
        try {
            // 0. 清理 BitmapPool
            bitmapPool.clear()
            log.debug("已清理 BitmapPool 中的所有 Bitmap 实例")


            // 1. 检查播放器是否存在
            val player = controller.player
            if (player == null) {
                log.debug("播放器对象为null，无需释放")
                return
            }

            // 2. 安全停止播放
            try {
                player.controls()?.stop()
                player.videoSurface()?.set(null)
            } catch (e: Exception) {
                log.warn("停止播放器时出错：", e)
            }

            // 3. 延迟确保VLC内部清理
            Thread.sleep(100)

            // 4. 安全释放
            try {
                player.release()
            } catch (e: Exception) {
                log.warn("释放播放器时出错：", e)
            }

            controller.player = null

        } finally {
            // 清理所有引用
            historyCollectJob?.cancel()
            byteArray = null
            info = null
        }
    }

    fun hasPlayer(): Boolean {
        return controller.player != null
    }

    //代理showTips方法
    fun showTips(tips: String) {
        controller.showTips(tips)
    }

}