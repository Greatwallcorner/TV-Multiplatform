package com.corner.ui.player.vlcj

import com.corner.database.History
import com.corner.ui.player.PlayerController
import com.corner.ui.player.frame.FrameRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer


class VlcjFrameController constructor(
    private val controller: VlcjController = VlcjController(),
) : FrameRenderer, PlayerController by controller {

    private fun getPixels(buffer: ByteBuffer, width: Int, height: Int) = runCatching {
        buffer.rewind()
        val pixels = ByteArray(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = buffer.int
                val index = (y * width + x) * 4
                val b = (pixel and 0xff).toByte()
                val g = (pixel shr 8 and 0xff).toByte()
                val r = (pixel shr 16 and 0xff).toByte()
                val a = (pixel shr 24 and 0xff).toByte()
                pixels[index] = b
                pixels[index + 1] = g
                pixels[index + 2] = r
                pixels[index + 3] = a
            }
        }
        pixels
    }.getOrNull()

    private val bufferFormatCallback by lazy {
        object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                return RV32BufferFormat(sourceWidth, sourceHeight)
            }

            override fun allocatedBuffers(buffers: Array<out ByteBuffer>?) = Unit
        }
    }

    private val renderCallback by lazy {
        RenderCallback { m, nativeBuffers, bufferFormat ->
            nativeBuffers?.firstOrNull()?.let { buffer ->
                getPixels(buffer, bufferFormat.width, bufferFormat.height)?.let {
                    _size.value = bufferFormat.width to bufferFormat.height
                    _bytes.value = it
                }
            }
        }
    }

    private val surface by lazy {
        controller.factory.videoSurfaces().newVideoSurface(bufferFormatCallback, renderCallback, false)
    }

    var playerSize = 0 to 0

    private val _size = MutableStateFlow(0 to 0)
    override val size = _size.asStateFlow()

    private val _bytes = MutableStateFlow<ByteArray?>(null)
    override val bytes = _bytes.asStateFlow()

    override fun load(url: String):PlayerController {
        controller.load(url)
        controller.player?.videoSurface()?.set(surface)
        speed(controller.history?.speed?.toFloat() ?: 1f)
        controller.play()
        controller.player?.controls()?.setTime(controller.history?.position ?: 0L)
        return controller
    }

    fun isPlaying():Boolean{
        return controller.player?.status()?.isPlaying ?: false
    }

    fun setStartEnd(opening:Long, ending:Long){
        controller.setStartEnding(opening, ending)
    }

    fun setControllerHistory(history: History){
        controller.history = history
    }

    fun getControllerHistory():History?{
        return controller.history
    }

}