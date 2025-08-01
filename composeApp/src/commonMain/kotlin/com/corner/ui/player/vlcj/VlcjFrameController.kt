package com.corner.ui.player.vlcj

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.corner.database.entity.History
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.player.PlayerController
import com.corner.ui.player.PlayerLifecycleManager
import com.corner.ui.player.frame.FrameRenderer
import com.corner.util.BrowserUtils.scope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import org.slf4j.LoggerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer
import javax.swing.SwingUtilities
import kotlin.math.max


class VlcjFrameController(
    component: DetailViewModel,
    private val controller: VlcjController = VlcjController(component),
) : FrameRenderer, PlayerController by controller {
    private val log = LoggerFactory.getLogger(this::class.java)

    private var byteArray: ByteArray? = null

    private var info: ImageInfo? = null

    val imageBitmapState: MutableState<ImageBitmap?> = mutableStateOf(null)

    // 添加volatile确保线程安全
    @Volatile
    private var isReleased = false

    // 添加公开的getter方法
    fun isReleased(): Boolean = isReleased

    private var historyCollectJob: Job? = null

    private val _size = MutableStateFlow(0 to 0)
    override val size = _size.asStateFlow()

    private val _bytes = MutableStateFlow<ByteArray?>(null)
    override val bytes = _bytes.asStateFlow()

    private val callbackSurFace = CallbackVideoSurface(
        object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                info = ImageInfo.makeN32(sourceWidth, sourceHeight, ColorAlphaType.OPAQUE)
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
                val byteBuffer = nativeBuffers[0]

                byteBuffer.get(byteArray)
                byteBuffer.rewind()

                val bmp = Bitmap()
                bmp.allocPixels(info!!)
                bmp.installPixels(byteArray)
                imageBitmapState.value = bmp.asComposeImageBitmap()
            }

            override fun unlock(mediaPlayer: MediaPlayer?) {
            }
        }, true,
        VideoSurfaceAdapters.getVideoSurfaceAdapter()
    )

    override fun load(url: String): PlayerController {
        controller.load(url)
        speed(controller.history.value?.speed?.toFloat() ?: 1f)
        controller.stop()
//        if(controller.player?.status()?.isPlaying == true){
//        }
        controller.play()
        seekTo(max(controller.history.value?.position ?: 0L, history.value?.opening ?: 0L))
        return controller
    }

    override fun vlcjFrameInit() {
        log.info("播放器初始化")
        // 创建lifecycleManager并设置给controller
        val lifecycleManager = PlayerLifecycleManager(controller, scope)
        controller.setLifecycleManager(lifecycleManager)
        //初始化播放器
        controller.init()
        controller.player?.videoSurface()?.set(callbackSurFace)
        isReleased = false
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

    /**
     * 原来的代码会出现Invalid memory access问题,尝试修复
     * */
    /**
     * 优化后的资源释放方法，解决音视频同步和流取消问题
     */
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

//    // 同步清理方法
//    // 在VlcjFrameController类中添加cleanup方法
//    fun cleanup() {
//        if (isReleased) return
//
//        try {
//            log.debug("VlcjFrameController开始执行cleanup")
//
//            // 取消历史收集协程
//            historyCollectJob?.cancel()
//
//            // 调用内部的VlcjController进行清理
//            controller.cleanup()
//
//            log.debug("VlcjFrameController cleanup完成")
//        } catch (e: Exception) {
//            log.error("VlcjFrameController cleanup失败", e)
//        }
//    }


//    // 添加异步初始化
//    override suspend fun initAsync() {
//        withContext(Dispatchers.IO) {
//            controller.initAsync()  // 调用VlcjController的异步方法
//            controller.player?.videoSurface()?.set(callbackSurFace)
//        }
//    }
//
//    // 添加异步清理
//    override suspend fun cleanupAsync() {
//        withContext(Dispatchers.IO) {
//            historyCollectJob?.cancel()
//            controller.cleanupAsync()// 调用VlcjController的异步方法
//        }
//    }


}