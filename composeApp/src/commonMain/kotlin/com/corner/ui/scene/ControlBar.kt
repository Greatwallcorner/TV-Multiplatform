package com.corner.ui.scene

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.corner.catvodcore.viewmodel.GlobalAppState

@Composable
fun ControlBar(
    title: @Composable () -> Unit = {},
    modifier: Modifier = Modifier.height(64.dp),
    leading: @Composable (() -> Unit)? = null,
    center: @Composable() (() -> Unit?)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(Modifier.fillMaxWidth().padding(end = 5.dp)) {
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

            Row(
                Modifier.align(alignment = Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()

                // 最小化按钮
                FilledTonalIconButton(
                    modifier = Modifier
                        .size(48.dp).padding(top = 2.dp, end = 5.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        GlobalAppState.windowState?.isMinimized = !GlobalAppState.windowState?.isMinimized!!
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        tint = Color.White,
                        imageVector = Icons.Default.Minimize,
                        contentDescription = "minimize",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 最大化/还原按钮
                FilledTonalIconButton(
                    modifier = Modifier
                        .size(48.dp).padding(top = 2.dp, end = 5.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        GlobalAppState.windowState?.placement =
                            if (WindowPlacement.Maximized == GlobalAppState.windowState?.placement)
                                WindowPlacement.Floating
                            else
                                WindowPlacement.Maximized
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        tint = Color.White,
                        imageVector = if (GlobalAppState.windowState?.placement == WindowPlacement.Maximized)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.CropSquare,
                        contentDescription = "maximize/restore",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 关闭按钮（红色强调）
                FilledTonalIconButton(
                    modifier = Modifier
                        .size(48.dp).padding(top = 2.dp, end = 5.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    onClick = {
                        GlobalAppState.closeApp.value = true
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        tint = Color.White,
                        imageVector = Icons.Default.Close,
                        contentDescription = "close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun CustomActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable () -> Unit
) {
    FilledTonalIconButton(
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        onClick = onClick,
        shape = shape
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