package com.corner.ui.scene

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.util.FirefoxGray

@Composable
fun ControlBar(
    title: @Composable () -> Unit = {},
    modifier: Modifier = Modifier.height(50.dp).padding(1.dp),
    leading: @Composable (() -> Unit)? = null,
    center: @Composable() (() -> Unit?)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isFullScreen = GlobalAppState.videoFullScreen.collectAsState()
    if (!isFullScreen.value) {
        Column(modifier = modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                if (leading != null) {
                    Row(Modifier.align(alignment = Alignment.CenterStart)) {
                        leading()
                        title()
                    }
                }

                if (center != null) {
                    Row(Modifier.align(alignment = Alignment.Center).wrapContentWidth()) {
                        center()
                    }
                }

                Row(Modifier.align(alignment = Alignment.CenterEnd)) {
                    actions()
                    CustomActionButton(modifier = Modifier.fillMaxHeight(), onClick = {
                        GlobalAppState.windowState?.isMinimized = !GlobalAppState.windowState?.isMinimized!!
                    }) {
                        Icon(
                            Icons.Default.Minimize,
                            contentDescription = "minimize",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(2.dp).fillMaxHeight(),
                        )
                    }
                    CustomActionButton(modifier = Modifier.fillMaxHeight(), onClick = {
                        GlobalAppState.windowState?.placement =
                            if (WindowPlacement.Maximized == GlobalAppState.windowState?.placement) WindowPlacement.Floating else WindowPlacement.Maximized
                    }) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "maximize",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(2.dp).fillMaxHeight()
                        )
                    }
                    CustomActionButton(modifier = Modifier.fillMaxHeight(), onClick = {
                        GlobalAppState.closeApp.value = true
                    }, color = Color.Red) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "close",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(2.dp).fillMaxHeight()
                        )
                    }
                }
            }
            Divider(color = Color.FirefoxGray, thickness = 1.5.dp)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun CustomActionButton(
    modifier: Modifier = Modifier.wrapContentWidth(),
    onClick: () -> Unit,
    color: Color = Color.Gray,
    content: @Composable () -> Unit
) {
    var isHover by remember { mutableStateOf(0) }
    val hoverColor = color.copy(alpha = 0.8f)
    val clickColor = color.copy(alpha = 0.6f)
    Box(
        modifier = modifier
            .onClick(onClick = onClick)
            .onPointerEvent(PointerEventType.Enter, onEvent = { isHover = 1 })
            .onPointerEvent(PointerEventType.Exit, onEvent = { isHover = 0 })
            .onPointerEvent(PointerEventType.Press, onEvent = { isHover = 2 })
            .background(if (isHover == 0) MaterialTheme.colorScheme.background else if (isHover == 1) hoverColor else clickColor)
    ) {
        content()
    }
}

@Composable
@Preview
fun previewControlBar() {
    MaterialTheme {
//        ControlBar{
//        }
    }
}