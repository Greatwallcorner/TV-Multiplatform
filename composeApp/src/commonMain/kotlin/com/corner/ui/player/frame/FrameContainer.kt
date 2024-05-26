package com.corner.ui.player.frame

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.player.vlcj.VlcjFrameController
import com.seiko.imageloader.Bitmap
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FrameContainer(
    modifier: Modifier = Modifier,
    controller: VlcjFrameController,
    imageSize: IntSize,
    bytes: ByteArray?,
) {
    val bitmap by remember(imageSize) {
        derivedStateOf {
            if (imageSize.width > 0 && imageSize.height > 0) Bitmap().apply {
                allocN32Pixels(imageSize.width, imageSize.height, true)
            }
            else null
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier.background(Color.Black)
        .combinedClickable(enabled = true,
            onDoubleClick = {
                controller.togglePlayStatus()
            },
            interactionSource = interactionSource,
            indication = null
        ){
            // onClick
        }
        .onPointerEvent(PointerEventType.Scroll){ e->
            val y = e.changes.first().scrollDelta.y
            if (y < 0) {
                controller.volumeUp()
            } else {
                controller.volumeDown()
            }
        }
        .onKeyEvent { k->
            when(k.key){
                Key.DirectionRight -> {
                    if(k.type == KeyEventType.KeyDown){
                        controller.fastForward()
                    }else if(k.type == KeyEventType.KeyUp){
                        controller.stopForward()
                    }
                    if(k.isTypedEvent){
                        controller.forward()
                    }
                }
                Key.DirectionLeft ->{
                    if(k.isTypedEvent){
                        controller.backward()
                    }
                }
                Key.Spacebar -> if(k.type == KeyEventType.KeyDown) controller.togglePlayStatus()
                Key.DirectionUp -> if(k.type == KeyEventType.KeyDown) controller.volumeUp()
                Key.DirectionDown -> if(k.type == KeyEventType.KeyDown) controller.volumeDown()
                Key.Escape -> if(k.type == KeyEventType.KeyDown && GlobalModel.videoFullScreen.value) controller.toggleFullscreen()
            }
            true
        }, contentAlignment = Alignment.Center) {
        if (bitmap == null){
            Icon(painter = painterResource("pic/TV-icon-s.png"), contentDescription = "TV Icon")
        }else{
            androidx.compose.foundation.Canvas(modifier = Modifier.border(5.dp, color = Color.Yellow)){
                val imageWidth = imageSize.width
                val imageHeight = imageSize.height
                val width = controller.playerSize.first.toFloat()
                val height = controller.playerSize.second.toFloat()

                val sx: Float = width / imageWidth
                val sy: Float = height / imageHeight

                val sf = min(sx.toDouble(), sy.toDouble()).toFloat()

                val scaledW: Float = imageWidth * sf
                val scaledH: Float = imageHeight * sf

                // 计算图像在 Canvas 上的位置
                val left = (width - scaledW) / 2
                val top = (height - scaledH) / 2

                // 绘制缩放后的图像
                withTransform({
                    translate((-width / 2)+left, (-height / 2)+top)
                    scale(sf, sf)
                }){
                    drawImage(
                        image = bitmap!!.run {
                            installPixels(bytes)
                            asComposeImageBitmap()
                        },
                    )
                }
            }
        }
//        bitmap?.let { bitmap ->
//            bytes?.let { bytes ->
//                Image(
//                    contentScale = ContentScale.FillWidth,
//                    bitmap = bitmap.run {
//                        installPixels(bytes)
//                        asComposeImageBitmap()
//                    },
//                    contentDescription = "frame"
//                )
//            }
//        } ?: CircularProgressIndicator()
    }
}