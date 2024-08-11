package com.corner.ui.player.frame

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.player.PlayState
import com.corner.ui.player.vlcj.VlcjFrameController

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FrameContainer(
    modifier: Modifier = Modifier,
    controller: VlcjFrameController,
    onClick: () -> Unit
) {
    val playerState = controller.state.collectAsState()
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
            onClick()
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
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            bitmap?.let {
                Image(bitmap = it, contentDescription = "Video", modifier = Modifier.fillMaxSize())
            }
            when (playerState.value.state) {
                PlayState.BUFFERING -> {
                    if (bitmap != null) {
                        ProgressIndicator(
                            Modifier.align(Alignment.Center),
                            progression = playerState.value.bufferProgression
                        )
                    } else {
                        ProgressIndicator(
                            Modifier.align(Alignment.Center)
                        )
                    }
                }

                PlayState.ERROR -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = "error icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(playerState.value.msg, color = MaterialTheme.colorScheme.primary)
                    }
                }

                else -> {
                    if (bitmap == null) {
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource("/pic/TV-icon-x.png"),
                            contentDescription = "nothing here",
                            contentScale = ContentScale.Crop
                        )
                    }
                }
//                }
            }
        }
    }
}

@Composable
fun ProgressIndicator(modifier: Modifier, text: String = "加载中...", progression: Float = -1f) {
    Column(modifier) {
        if (progression != -1f) {
            CircularProgressIndicator(
                progress = { progression / 100},
            )
        } else {
            CircularProgressIndicator()
        }
        Text(
            if (progression != -1f) "%.2f".format(progression) + "%" else text, style = TextStyle(
                color = MaterialTheme.colorScheme.primary, shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(8f, 8f),
                    blurRadius = 8f
                ),
                fontWeight = FontWeight.Bold
            )
        )
    }
}
