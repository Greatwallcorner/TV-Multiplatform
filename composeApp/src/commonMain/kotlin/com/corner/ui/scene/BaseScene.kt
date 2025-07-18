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
import com.corner.catvodcore.viewmodel.GlobalAppState.isShowProgress
import org.jetbrains.compose.resources.painterResource
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.no_data
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp

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

/*
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
fun emptyShow(modifier: Modifier = Modifier, onRefresh: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.size(100.dp))//顶边高度
        EmptyState(
            title = "没有找到内容",
            subtitle = "请检查网络连接或稍后再试",
            onRefresh = onRefresh,
            isLoading = true
        )
        Spacer(Modifier.size(100.dp))//底边高度
    }
}

/*
* 使用isLoading = true设置加载状态
* */

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    title: String = "这里什么都没有...",
    subtitle: String? = null,
    isLoading: Boolean = false // 新增加载状态参数
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(180.dp),
                painter = painterResource(Res.drawable.no_data),
                contentDescription = "empty_state_illustration",
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            )

            Spacer(Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                )

                subtitle?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            onRefresh?.let {
                Spacer(Modifier.height(32.dp))

                FilledTonalButton(
                    onClick = it,
                    modifier = Modifier.widthIn(min = 160.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading, // 禁用按钮当加载中
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
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isLoading) "加载中..." else "重新加载")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolTipText(
    text: String,
    textStyle: TextStyle,
    delayMills: Int = 600,
    modifier: Modifier = Modifier
) {
    TooltipArea(
        tooltip = {
            // ✅ 正确实现卡片式工具提示
            Card(
                modifier = Modifier.shadow(4.dp),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium, // 使用更适合小字号的样式
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = textStyle,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.then(modifier) // 合并外部modifier
        )
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

@Preview
@Composable
fun previewDrawer() {
    AppTheme {
//        val choose = remember { mutableStateOf(Menu.HOME.name) }
//        MenuList(Modifier, choose) {
//            videoList(Modifier, mutableListOf())
//        }
    }
}