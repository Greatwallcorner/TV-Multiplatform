package com.corner.ui.scene

import AppTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.corner.catvodcore.enum.Menu

private var showProgress by mutableStateOf(false)

fun showProgress() {
    showProgress = true
}

fun hideProgress() {
    showProgress = false
}

fun isShowProgress(): Boolean {
    return showProgress
}

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
    val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(500))
    if (showProgress) {
        Box(
            Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp).align(Alignment.Center).alpha(alpha),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondaryContainer,
            )

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
                modifier = Modifier.fillMaxWidth(),
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
fun Dialog(modifier: Modifier, showDialog: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    if (showDialog) {
        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(0.6f))
                .clickable(interactionSource = interactionSource, indication = null, onClick = {
                    onClose()
                })
        ) {
            Surface(
                modifier = modifier
                    .shadow(2.dp, shape = RoundedCornerShape(10.dp))
                    .align(Alignment.Center)
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
fun ToolTipText(text: String, textStyle: TextStyle) {
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
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        delayMillis = 600
    ) {
        Text(text = text, maxLines = 1, style = textStyle.copy(MaterialTheme.colorScheme.onSurface), overflow = TextOverflow.Ellipsis)
    }
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

//@Composable
//expect fun getScreenSize(): Dimension