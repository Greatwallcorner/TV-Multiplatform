package com.corner.ui.player.vlcj

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.corner.database.entity.History
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.player.PlayerController
import com.corner.ui.player.frame.FrameRenderer
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
import kotlin.math.max


class VlcjFrameController(
    component: DetailViewModel,
    private val controller: VlcjController = VlcjController(component),
) : FrameRenderer, PlayerController by controller {
    private val log = LoggerFactory.getLogger(this::class.java)

    private var byteArray: ByteArray? = null
    private var info: ImageInfo? = null
    val imageBitmapState: MutableState<ImageBitmap?> = mutableStateOf(null)

    var isReleased = true
    private set

    private var historyCollectJob: Job? = null


    private val _size = MutableStateFlow(0 to 0)
    override val size = _size.asStateFlow()

    private val _bytes = MutableStateFlow<ByteArray?>(null)
    override val bytes = _bytes.asStateFlow()

    private val callbackSurFace = CallbackVideoSurface(object : BufferFormatCallback {
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

    override fun init() {
        log.info("播放器初始化")
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
        if(historyCollectJob != null) return
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
            if(controller.history.value == null) return@runBlocking
            controller.history.emit(func(controller.history.value!!))
        }
    }

    fun getPlayer(): MediaPlayer? {
        return controller.player
    }

    fun release() {
        controller.player?.controls()?.stop()
        controller.player?.release()
        controller.factory.release()
        isReleased = true
    }
}