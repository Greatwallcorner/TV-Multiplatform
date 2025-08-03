package com.corner.ui.scene

import AppTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corner.catvodcore.enum.Menu
import org.jetbrains.compose.resources.painterResource
import lumentv_compose.composeapp.generated.resources.Res
import lumentv_compose.composeapp.generated.resources.no_data
import androidx.compose.animation.core.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

@Composable
fun ExpandedText(text: String, maxLine: Int, textStyle: TextStyle = TextStyle()) {
    var expanded by remember { mutableStateOf(false) }
    SelectionContainer {
        Text(
            text = text,
            maxLines = if (expanded) Int.MAX_VALUE else maxLine,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = { expanded = !expanded }),
            style = textStyle
        )
    }
}

/**
 * 该组件已被重写，可自定义样式，调用时使用withOverlay = true显示遮罩
 * */

@Composable
fun LoadingIndicator(
    showProgress: Boolean,
    modifier: Modifier = Modifier,
    withOverlay: Boolean = true,
    overlayColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 4.dp,
    size: Dp = 64.dp
) {
    if (showProgress) {
        Box(
            modifier = modifier
                .fillMaxSize()
                // 当显示遮罩时，拦截所有指针事件
                .then(if (withOverlay) Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } } else Modifier)
                .then(if (withOverlay) Modifier.background(overlayColor) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            // 组合进度指示器和动画效果
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆形（可选）
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = trackColor,
                        radius = size.toPx() / 2
                    )
                }

                // 进度指示器带旋转动画
                val infiniteTransition = rememberInfiniteTransition()
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .rotate(rotation),
                    color = indicatorColor,
                    strokeWidth = strokeWidth,
                    strokeCap = StrokeCap.Round
                )

                // 可选的内部装饰（如应用logo）
                /*
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.4f),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                */
            }
        }
    }
}

@Composable
fun emptyShow(
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    title: String = "没有找到内容",
    subtitle: String = "请检查网络连接或稍后再试",
    isLoading: Boolean? = false,
    showIcon: Boolean? = true, // 默认显示图标
    showRefresh: Boolean? = true, // 默认显示刷新按钮
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyState(
            title = title,
            subtitle = subtitle,
            onRefresh = onRefresh,
            isLoading = isLoading,
            showIcon = showIcon,
            showRefresh = showRefresh
        )
    }
}

@Composable
fun TopEmptyShow(
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    title: String = "没有找到内容",
    subtitle: String = "请检查网络连接或稍后再试",
    isLoading: Boolean? = false,
    showIcon: Boolean? = false, // 默认不显示图标
    buttonAlignment: ButtonAlignment = ButtonAlignment.CENTER,
    showRefresh: Boolean? = true, // 默认显示刷新按钮
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .height(56.dp), // 限制高度，可根据实际情况调整
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // 垂直居中
    ) {
        EmptyState(
            modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(1.dp), // 高度占满 Column
            title = title,
            subtitle = subtitle,
            onRefresh = onRefresh,
            isLoading = isLoading,
            showIcon = showIcon,
            buttonAlignment = buttonAlignment,
            textAlignment = TextAlignmentOption.LEFT,
            isTopBar = true,
            showRefresh = showRefresh
        )
    }
}

/*
* 使用isLoading = true设置加载状态
* */

// 定义一个枚举类型来表示按钮的对齐方式
enum class ButtonAlignment {
    LEFT,
    CENTER,
    RIGHT
}

// 定义文字对齐方式的枚举
enum class TextAlignmentOption {
    LEFT,
    CENTER,
    RIGHT
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    title: String = "这里什么都没有...",
    subtitle: String? = null,
    isLoading: Boolean? = false,
    showIcon: Boolean? = true, // 默认显示图标
    // 按钮对齐方式参数，默认居中
    buttonAlignment: ButtonAlignment = ButtonAlignment.CENTER,
    // 新增文字对齐方式参数，默认左对齐
    textAlignment: TextAlignmentOption = TextAlignmentOption.LEFT,
    // 新增参数，用于区分是否为顶栏显示
    isTopBar: Boolean = false,
    // 新增参数，用于区分是否显示刷新按钮
    showRefresh: Boolean? = true
) {
    val paddingValue = if (isTopBar) 1.dp else 24.dp
    val iconSize = if (isTopBar) 40.dp else 180.dp
    val titleStyle = if (isTopBar) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
    val subtitleStyle = if (isTopBar) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    val buttonMinWidth = if (isTopBar) 120.dp else 160.dp

    Box(modifier = modifier.fillMaxSize()) {
        if (isTopBar) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValue),
                verticalAlignment = Alignment.CenterVertically,
                // 修改为从左开始排列
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showIcon == true) {
                    Image(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(Res.drawable.no_data),
                        contentDescription = "empty_state_illustration",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                    Spacer(Modifier.width(8.dp))
                }

                // 根据文字对齐枚举设置对应的 TextAlign 值
                val textAlignValue = when (textAlignment) {
                    TextAlignmentOption.LEFT -> TextAlign.Left
                    TextAlignmentOption.CENTER -> TextAlign.Center
                    TextAlignmentOption.RIGHT -> TextAlign.Right
                }

                // 根据文字对齐枚举设置 Column 的水平对齐方式
                val columnAlignment = when (textAlignment) {
                    TextAlignmentOption.LEFT -> Alignment.Start
                    TextAlignmentOption.CENTER -> Alignment.CenterHorizontally
                    TextAlignmentOption.RIGHT -> Alignment.End
                }

                Column(
                    horizontalAlignment = columnAlignment
                ) {
                    Text(
                        modifier = Modifier.padding(start = 15.dp),
                        text = title,
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                        textAlign = textAlignValue
                    )

                    subtitle?.let {
                        Text(
                            modifier = Modifier.padding(start = 15.dp),
                            text = it,
                            style = subtitleStyle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = textAlignValue
                        )
                    }
                }

                onRefresh?.let {
                    isLoading?.let { it1 ->
                        if (showRefresh == true) {
                            FilledTonalButton(
                                onClick = it,
                                modifier = Modifier.widthIn(min = buttonMinWidth).padding(end = 15.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !it1, // 禁用按钮当加载中
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "refresh",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("重新加载")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (showIcon == true) {
                    Image(
                        modifier = Modifier.size(iconSize),
                        painter = painterResource(Res.drawable.no_data),
                        contentDescription = "empty_state_illustration",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                }

                Spacer(Modifier.height(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                    )

                    subtitle?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = subtitleStyle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                onRefresh?.let {
                    Spacer(Modifier.height(32.dp))

                    isLoading?.let { it1 ->
                        if (showRefresh == true) {
                            FilledTonalButton(
                                onClick = it,
                                modifier = Modifier.widthIn(min = buttonMinWidth),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !it1, // 禁用按钮当加载中
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "refresh",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    //                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("重新加载")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun previewLoadingIndicator() {
    AppTheme {
        LoadingIndicator(true, withOverlay = true)
    }
}

@Composable
fun MenuItem(
    modifier: Modifier,
    menu: Menu,
    onClick: () -> Unit,
    chosen: Boolean,
) {
    val background = if (chosen) Color.Gray else MaterialTheme.colorScheme.background
    val primaryColor = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth().background(background).padding(horizontal = 0.dp, vertical = 8.dp),
        onClick = {
            onClick()
        }) {
        Column(modifier.background(background).padding(5.dp)) {
            Icon(menu.icon, menu.desc, modifier = Modifier.align(Alignment.CenterHorizontally), tint = primaryColor)
            Spacer(Modifier.padding(3.dp))
            Text(
                text = menu.desc,
                modifier = Modifier.size(40.dp),
                color = primaryColor,
                style = TextStyle(textAlign = TextAlign.Center),
            )
        }
    }
}

/**
 * @param modifier 弹窗窗体的modifier
 */
@Composable
fun Dialog(
    modifier: Modifier,
    showDialog: Boolean,
    onClose: () -> Unit,
    enter: EnterTransition = scaleIn() + fadeIn(spring()),
    exit: ExitTransition = scaleOut() + fadeOut(spring()),
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = showDialog, enter = enter, exit = exit
    ) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier.fillMaxSize()
                .clickable(interactionSource = interactionSource, indication = null, onClick = {
                    onClose()
                })
        ) {
            Surface(
                modifier = modifier.shadow(2.dp, shape = RoundedCornerShape(10.dp)).align(Alignment.Center)
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(2.dp, Color.Gray)
            ) {
                content()
            }
        }
    }
}

//当文字过长时可以选择滚动或阶段显示省略号
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolTipText(
    text: String,
    textStyle: TextStyle,
    delayMills: Int = 600,
    modifier: Modifier = Modifier,
    enableMarquee: Boolean = true
) {
    TooltipArea(
        tooltip = {
            Card(
                modifier = Modifier.shadow(4.dp),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        delayMillis = delayMills,
        tooltipPlacement = TooltipPlacement.CursorPoint(
            offset = DpOffset(0.dp, 10.dp)
        )
    ) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            if (enableMarquee) {
                BasicText(
                    text = text,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        )
                )
            } else {
                Text(
                    text = text,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
fun HoverableText(text: String, style: TextStyle = TextStyle(), onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val spanStyle = SpanStyle(
        color = if (isHovered) Color.Red else MaterialTheme.colorScheme.primary,
        textDecoration = if (isHovered) TextDecoration.Underline else TextDecoration.None
    )

    ClickableText(
        text = AnnotatedString(text = text, spanStyle = spanStyle),
        onClick = { offset ->
            // Handle click event
            onClick()
        },
        style = style,
        modifier = Modifier.padding(8.dp).background(Color.Transparent).hoverable(interactionSource, true)
    )
}

//@Preview
//@Composable
//fun previewDrawer() {
//    AppTheme {
////        val choose = remember { mutableStateOf(Menu.HOME.name) }
////        MenuList(Modifier, choose) {
////            videoList(Modifier, mutableListOf())
////        }
//    }
//}