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

@Composable
fun LoadingIndicator(showProgress: Boolean) {
    if (showProgress) {
        Box(
            Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp).align(Alignment.Center),
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 5.dp,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
            )

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
        Image(
            modifier = Modifier.size(200.dp),
            painter = painterResource(Res.drawable.no_data),
            contentDescription = "nothing_svg here",
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.size(20.dp))
        Text("这里什么都没有....")
        Spacer(Modifier.size(20.dp))
        onRefresh?.let {
            Button(
                onClick = onRefresh,
                Modifier.width(110.dp).height(50.dp),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                enabled = !isShowProgress()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "refresh", modifier = Modifier.fillMaxSize(0.8f))
            }
            Spacer(Modifier.size(100.dp))//底边高度
        }
    }
}

@Composable
@Preview
fun previewLoadingIndicator() {
    AppTheme {
        LoadingIndicator(true)
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
    Surface(modifier = Modifier.fillMaxWidth().background(background).padding(horizontal = 0.dp, vertical = 8.dp),
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
fun ToolTipText(text: String, textStyle: TextStyle, delayMills: Int = 600, modifier: Modifier = Modifier) {
    TooltipArea(
        tooltip = {
            // composable tooltip content
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(10.dp).size(30.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }, delayMillis = delayMills
    ) {
        Text(
            text = text, maxLines = 1, style = textStyle, overflow = TextOverflow.Ellipsis, modifier = modifier
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