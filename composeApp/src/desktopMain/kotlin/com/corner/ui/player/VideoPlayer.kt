package com.corner.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.corner.ui.player.vlcj.VlcjController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun VideoPlayer(
    mrl: String,
//    videoInfo: VideoInfo,
    state: VlcjController,
    modifier: Modifier,
) {
    val exifRotation = remember(mrl) {
        checkRotation(mrl)
    }

    var imageBitmap by remember(mrl) { mutableStateOf<ImageBitmap?>(null) }
    var mediaPlayerRead by remember(mrl) { mutableStateOf(false) }


    if(mediaPlayerRead) {
        val interactionSource = remember { MutableInteractionSource() }
        imageBitmap?.let {
            androidx.compose.foundation.Image(
                bitmap = it,
                contentDescription = "Video",
                modifier = modifier.background(Color.Black)
                    .combinedClickable(enabled = true,
                        onDoubleClick = {
                        state.togglePlayStatus()
                    },
                        interactionSource = interactionSource,
                        indication = null
                    ){
                        // onClick
                    }
                    .onPointerEvent(PointerEventType.Scroll){e->
                        val y = e.changes.first().scrollDelta.y
                        if (y < 0) {
                            state.volumeUp()
                        } else {
                            state.volumeDown()
                        }
                    }
                    .onKeyEvent { k->
                        when(k.key){
                            Key.DirectionRight -> {
                                if(k.type == KeyEventType.KeyDown){
                                    state.fastForward()
                                }else if(k.type == KeyEventType.KeyUp){
                                    state.stopForward()
                                }
                            }
                            Key.Spacebar -> if(k.type == KeyEventType.KeyDown) state.togglePlayStatus()
                            Key.DirectionUp -> if(k.type == KeyEventType.KeyDown) state.volumeUp()
                            Key.DirectionDown -> if(k.type == KeyEventType.KeyDown) state.volumeDown()
                        }
                        true
                    }
//                    .let { m ->
//                        if(exifRotation % 180 == 0) {
//                            m.rotate(exifRotation.toFloat())
//                        } else {
//                            m.rotate(exifRotation.toFloat()).scale(videoInfo.videoHeight.toFloat()/videoInfo.videoWidth,videoInfo.videoWidth.toFloat()/videoInfo.videoHeight)
//                        }
//                    }

            )
        } ?: run {
            Box(modifier = modifier.background(Color.Black))
        }
    } else {
        Box(modifier = modifier.background(Color.Black))
    }

    val mediaPlayer = remember(mrl) {
        var byteArray :ByteArray? = null
        var info: ImageInfo? = null
        val factory = MediaPlayerFactory()
        val embeddedMediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer()
        val callbackVideoSurface = CallbackVideoSurface(
            object : BufferFormatCallback {
                override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {

//                    info = ImageInfo.makeN32(videoInfo.videoWidth, videoInfo.videoHeight, ColorAlphaType.OPAQUE)
//                    return RV32BufferFormat(videoInfo.videoWidth, videoInfo.videoHeight)
                    info = ImageInfo.makeN32(sourceWidth, sourceHeight, ColorAlphaType.OPAQUE)
                    return RV32BufferFormat(sourceWidth, sourceHeight)
                }

                override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {
                    byteArray =  ByteArray(buffers[0].limit())
                }
            },
            object : RenderCallback {
                var pos: Float = -1f

                override fun display(
                    mediaPlayer: MediaPlayer,
                    nativeBuffers: Array<out ByteBuffer>,
                    bufferFormat: BufferFormat?
                ) {
                    if(!mediaPlayer.status().isPlaying && pos == mediaPlayer.status().position()) {
                        return
                    }
                    pos = mediaPlayer.status().position()

                    val byteBuffer = nativeBuffers[0]

                    byteBuffer.get(byteArray)
                    byteBuffer.rewind()

                    val bmp = Bitmap()
                    bmp.allocPixels(info!!)
                    bmp.installPixels(byteArray)
                    imageBitmap = bmp.asComposeImageBitmap()
                }
            },
            true,
            VideoSurfaceAdapters.getVideoSurfaceAdapter(),
        )
        embeddedMediaPlayer.videoSurface().set(callbackVideoSurface)
        embeddedMediaPlayer
    }


    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = mrl) {
        println("Launched effect")
        mediaPlayer.audio().mute()

        if(exifRotation != 0) {
            removeRotation(mrl)
        }

        if(StringUtils.isNotBlank(mrl)){
            mediaPlayer.media().play(mrl)
        }
        mediaPlayer.events().addMediaPlayerEventListener(state.stateListener)

        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {

            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                super.timeChanged(mediaPlayer, newTime)
            }

            override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                super.mediaPlayerReady(mediaPlayer)

                println("mediaPlayerReady ${mediaPlayer.video().videoDimension().width} ${mediaPlayer.video().videoDimension().height}")

                mediaPlayer.submit {
//                    mediaPlayer.audio().mute()
                    //mediaPlayer.controls().setTime(1L)
//                    mediaPlayer.controls().pause()
                    coroutineScope.launch {
                        delay(100)
                        mediaPlayerRead = true
                        state.onMediaPlayerReady(mediaPlayer)
                    }
                }

                if(exifRotation != 0) {
                    setRotation(mrl, exifRotation)
                }
                println("mediaPlayerReady")
            }

        })
    }

    DisposableEffect(key1 = mrl, effect = {
        this.onDispose {
//            mediaPlayer.release()
            state.dispose()
        }
    })
}

fun checkRotation(
    videoFilePath: String,
): Int {
    return 0
//    if(OperatingSystem.isWindows) {
//        return 0 //it works fine on Windows
//    }


//    //for MacOS we need to do that hack and remove exif rotation, rotate it manually
//    val exiftool = AdditionalLibrariesUtil.getExiftoolPath()
//    return Runtime.getRuntime().exec(
//        arrayOf(
//            exiftool,
//            "-Rotation",
//            videoFilePath,
//        )
//    ).let {
//        Reader.slurp(it.inputStream, 1000)
//    }.let {
//        if(it.isBlank()) return 0
//
//        it.lines().let { (rotationLine) ->
//            rotationLine.split(":")[1].trim().toInt()
//        }
//    }
}

private fun removeRotation(videoFilePath: String) {
    setRotation(videoFilePath, 0)
}

private fun setRotation(
    videoFilePath: String,
    rotation: Int,
) {

//    val exiftool = AdditionalLibrariesUtil.getExiftoolPath()
//    return Runtime.getRuntime().exec(
//        arrayOf(
//            exiftool,
//            "-api", "LargeFileSupport=1",
//            "-rotation=$rotation",
//            videoFilePath,
//        )
//    ).let {
//        Reader.slurp(it.inputStream, 1000)
//    }
}