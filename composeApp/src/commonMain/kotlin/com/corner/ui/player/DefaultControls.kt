package com.corner.ui.player

import AppTheme
import PlayerControlsTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.util.Utils
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.ui.player.vlcj.VlcjFrameController
import com.corner.util.formatTimestamp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import lumentv_compose.composeapp.generated.resources.Res
import lumentv_compose.composeapp.generated.resources.speed
import org.slf4j.LoggerFactory
import kotlin.math.roundToLong

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DefaultControls(
    modifier: Modifier = Modifier,
    controller: VlcjFrameController,
    vod: Vod,
    onClickChooseEp: () -> Unit
) {
    val playerState by controller.state.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val isShowPreviewTime = interactionSource.collectIsHoveredAsState()
    var sliderWidth by remember { mutableStateOf(1f) }
    var sliderStart by remember { mutableStateOf(0f) }
    var hoveredValue by remember { mutableStateOf(0f) }
    val mousePosition = remember { mutableStateOf(IntOffset(0, 0)) }
    val previewTimeText by remember {
        derivedStateOf {
            hoveredValue.roundToLong().formatTimestamp()
        }
    }
    val animatedTimestamp by animateFloatAsState(playerState.timestamp.toFloat())
    // 收集全屏状态
    val isFullScreen = GlobalAppState.videoFullScreen.collectAsState()
    var showAspectRatioDropdown by remember { mutableStateOf(false) }
    // 视频比例选项
    val aspectRatios = listOf(
        "" to "原始比例",
        "16:9" to "16:9",
        "4:3" to "4:3",
        "16:10" to "16:10",
        "21:9" to "21:9",
        "18:9" to "18:9"
    )

    PlayerControlsTheme {
        Box(modifier.background(Color.Black.copy(alpha = 0.7f))) {
            SliderPreviewPopup(isShowPreviewTime.value, { mousePosition.value.x }, previewTimeText)
            Column(
                modifier.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 进度条
                androidx.compose.material3.Slider(
                    value = animatedTimestamp,
                    onValueChange = { controller.seekTo(it.roundToLong()) },
                    valueRange = 0f..playerState.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                        .height(30.dp)
                        .hoverable(interactionSource)
                        .onGloballyPositioned { layoutCoordinates ->
                            layoutCoordinates.parentLayoutCoordinates
                            sliderStart = layoutCoordinates.positionInParent().x
                        }
                        .onSizeChanged { sliderWidth = it.width.toFloat() }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val position = event.changes.first().position
                                    mousePosition.value = IntOffset(position.x.toInt(), position.y.toInt())
                                    val relativeX = (position.x - sliderStart).coerceIn(0f, sliderWidth)
                                    val percent = relativeX / sliderWidth
                                    // 偏差 2-3秒
                                    hoveredValue = percent * playerState.duration
                                }
                            }
                        },
                    track = {
                        androidx.compose.material3.SliderDefaults.Track(
                            sliderState = it,
                            modifier = Modifier.height(10.dp),
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                        )
                    },
                )
                // 使用 FlowRow 替代 Row，当空间不足时自动换行
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 左侧按钮组
                    Row(
                        modifier = Modifier.width(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // 在原有片头片尾按钮旁边添加重置按钮
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 片头按钮
                            TextButtonTransparent(
                                if (playerState.opening == -1L) "片头" else Utils.formatMilliseconds(playerState.opening)
                            ) {
                                controller.updateOpening(vod)
                            }

                            // 片尾按钮
                            TextButtonTransparent(
                                if (playerState.ending == -1L) "片尾" else Utils.formatMilliseconds(playerState.ending)
                            ) {
                                controller.updateEnding(vod)
                            }

                            // 重置按钮
                            IconButton(
                                onClick = {
                                    controller.resetOpeningEnding()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "重置片头片尾",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Box {
                                IconButton(onClick = { showAspectRatioDropdown = true }) {
                                    Icon(
                                        Icons.Default.AspectRatio,
                                        contentDescription = "视频比例",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showAspectRatioDropdown,
                                    onDismissRequest = { showAspectRatioDropdown = false }
                                ) {
                                    aspectRatios.forEach { (ratio, displayName) ->
                                        DropdownMenuItem(
                                            text = { Text(displayName) },
                                            onClick = {
                                                controller.setAspectRatio(ratio)
                                                showAspectRatioDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.weight(0.3f).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "${playerState.timestamp.formatTimestamp()} / ${playerState.duration.formatTimestamp()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(
                        modifier = Modifier.widthIn( min = 20.dp, max = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playerState.state == PlayState.PLAY) {
                            IconButton(controller::togglePlayStatus) {
                                Icon(Icons.Rounded.Pause, "pause media", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(controller::togglePlayStatus) {
                                Icon(Icons.Rounded.PlayArrow, "play media", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.width(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (playerState.isMuted || playerState.volume == -1.0f || playerState.volume == 0f) {
                            IconButton(controller::toggleSound) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeOff,
                                    "volume off",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            val icon = if (playerState.volume < .5f)
                                Icons.AutoMirrored.Rounded.VolumeDown
                            else
                                Icons.AutoMirrored.Rounded.VolumeUp
                            IconButton(controller::toggleSound) {
                                Icon(icon, "volume", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        // 音量滑块
                        Slider(
                            value = playerState.volume,
                            onValueChange = controller::setVolume,
                            modifier = Modifier
                                .width(90.dp)
                                .height(15.dp),
                            valueRange = 0f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Spacer(Modifier.size(5.dp))
                        // 倍速控件
                        Speed(
                            initialValue = playerState.speed,
                            Modifier.width(70.dp)
                                .height(45.dp)
                                .height(30.dp)
                        ) {
                            controller.speed(
                                it ?: 1F
                            )
                        }
                        IconButton({ controller.toggleFullscreen() }) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "fullScreen/UnFullScreen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // 仅在全屏状态下显示选集按钮
                        AnimatedVisibility(visible = isFullScreen.value) {
                            TextButtonTransparent("选集") {
                                onClickChooseEp()
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TextButtonTransparent(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(2),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.primary,
            fontSize = TextUnit(12f, TextUnitType.Sp),
            style = MaterialTheme.typography.labelLarge.copy(shadow = Shadow(offset = Offset(2f, 2f), blurRadius = 2f))
        )
    }
}

@Composable
fun SliderPreviewPopup(isShow: Boolean, offsetX: () -> Int, text: String) {
    if (isShow) {
        val density = LocalDensity.current
        val popupPositionProviderState = remember {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val anchor = IntRect(
                        offset = IntOffset(
                            offsetX(),
                            with(density) { -8.dp.toPx().toInt() },
                        ) + anchorBounds.topLeft,
                        size = IntSize.Zero,
                    )
                    val tooltipArea = IntRect(
                        IntOffset(
                            anchor.left - popupContentSize.width,
                            anchor.top - popupContentSize.height,
                        ),
                        IntSize(
                            popupContentSize.width * 2,
                            popupContentSize.height * 2,
                        ),
                    )
                    val position = Alignment.TopCenter.align(popupContentSize, tooltipArea.size, layoutDirection)

                    return IntOffset(
                        x = (tooltipArea.left + position.x).coerceIn(0, windowSize.width - popupContentSize.width),
                        y = (tooltipArea.top + position.y).coerceIn(0, windowSize.height - popupContentSize.height),
                    )
                }
            }
        }
        Popup(popupPositionProviderState/*offset = offset*/) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .animateContentSize()
            ) {
                Box(
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    Alignment.Center
                ) {
                    Text(text)
                }
            }
        }
    }
}

/**
 * See [this Stack Overflow post](https://stackoverflow.com/a/67765652).
 */

@Composable
fun Speed(
    initialValue: Float,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    var currentSpeed by remember { mutableStateOf(initialValue) }
    val speedOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 5.0f)
    val displayValue = remember(currentSpeed) {
        if (currentSpeed in speedOptions) currentSpeed.toString() + "x"
        else "%.1fx".format(currentSpeed)
    }

    Box(
        modifier = modifier,
        // 设置内容在水平和垂直方向上居中
        contentAlignment = Alignment.Center
    ) {
        // 倍速选择器
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.speed),
                contentDescription = "Speed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = displayValue,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }

        // 下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(120.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
        ) {
            // 预设倍速选项
            speedOptions.forEach { speed ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${speed}x",
                            color = if (currentSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        currentSpeed = speed
                        onChange(speed)
                        expanded = false
                    }
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // 自定义输入区
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = customInput,
                    onValueChange = {
                        customInput = it.takeWhile { c -> c.isDigit() || c == '.' }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .fillMaxWidth()// 确保输入框宽度占满下拉菜单项
                        .heightIn(min = 40.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    placeholder = { Text("自定义") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        customInput.toFloatOrNull()?.let {
                            if (it > 0) {
                                currentSpeed = it.coerceAtMost(10f)
                                onChange(currentSpeed)
                                expanded = false
                            }
                        }
                        customInput = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("确认")
                }
            }
        }
    }
}