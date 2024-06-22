package com.corner.ui.player.frame

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.player.vlcj.VlcjFrameController
import uk.co.caprica.vlcj.player.base.State

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FrameContainer(
    modifier: Modifier = Modifier,
    controller: VlcjFrameController,
) {
    val playerState = rememberUpdatedState(controller.getPlayer()?.status()?.state())
    val bitmap by remember { controller.imageBitmapState }
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier.background(Color.Black)
        .combinedClickable(
            enabled = true,
            onDoubleClick = {
                controller.togglePlayStatus()
            },
            interactionSource = interactionSource,
            indication = null
        ) {
            // onClick
        }
        .onPointerEvent(PointerEventType.Scroll) { e ->
            val y = e.changes.first().scrollDelta.y
            if (y < 0) {
                controller.volumeUp()
            } else {
                controller.volumeDown()
            }
        }
        .onKeyEvent { k ->
            when (k.key) {
                Key.DirectionRight -> {
                    if (k.type == KeyEventType.KeyDown) {
                        controller.fastForward()
                    } else if (k.type == KeyEventType.KeyUp) {
                        controller.stopForward()
                    }
                    if (k.isTypedEvent) {
                        controller.forward()
                    }
                }

                Key.DirectionLeft -> {
                    if (k.isTypedEvent) {
                        controller.backward()
                    }
                }

                Key.Spacebar -> if (k.type == KeyEventType.KeyDown) controller.togglePlayStatus()
                Key.DirectionUp -> if (k.type == KeyEventType.KeyDown) controller.volumeUp()
                Key.DirectionDown -> if (k.type == KeyEventType.KeyDown) controller.volumeDown()
                Key.Escape -> if (k.type == KeyEventType.KeyDown && GlobalModel.videoFullScreen.value) controller.toggleFullscreen()
            }
            true
        }, contentAlignment = Alignment.Center
    ) {
        if(bitmap != null){
            bitmap?.let {
                Image(bitmap = it, contentDescription = "Video", modifier = Modifier.fillMaxSize())
            }
        }else{
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                when (playerState.value?.name ?: "") {
                    State.BUFFERING.name ->
                            androidx.compose.material3.CircularProgressIndicator(Modifier.align(Alignment.Center))
                    else ->
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource("/pic/TV-icon-x.png"),
                            contentDescription = "nothing here",
                            contentScale = ContentScale.Crop
                        )
                }
            }
        }
    }
}
