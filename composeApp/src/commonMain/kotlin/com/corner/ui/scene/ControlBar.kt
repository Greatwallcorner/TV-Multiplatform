package com.corner.ui.scene

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.corner.catvodcore.viewmodel.GlobalAppState



@Composable
fun ControlBar(
    title: @Composable () -> Unit = {},
    modifier: Modifier = Modifier.height(64.dp), // 更紧凑的高度
    leading: @Composable (() -> Unit)? = null,
    center: @Composable() (() -> Unit?)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // 左侧内容
            if (leading != null || title != null) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    leading?.invoke()
                    Spacer(modifier = Modifier.width(8.dp).thenIf(leading != null))
                    title()
                }
            }

            // 中央内容
            if (center != null) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    center()
                }
            }

            // 右侧操作按钮
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                actions()

                // 窗口控制按钮组
                WindowControlButtons()
            }
        }

        // 更精细的分割线
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun RowScope.WindowControlButtons() {
    // 最小化按钮
    WindowControlButton(
        icon = Icons.Default.Minimize,
        description = "Minimize",
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        onClick = {
            GlobalAppState.windowState?.isMinimized = !GlobalAppState.windowState?.isMinimized!!
        }
    )

    // 最大化/还原按钮
    WindowControlButton(
        icon = if (GlobalAppState.windowState?.placement == WindowPlacement.Maximized)
            Icons.Default.KeyboardArrowUp else Icons.Default.CropSquare,
        description = "Maximize/Restore",
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        onClick = {
            GlobalAppState.windowState?.placement =
                if (GlobalAppState.windowState?.placement == WindowPlacement.Maximized)
                    WindowPlacement.Floating
                else
                    WindowPlacement.Maximized
        }
    )

    // 关闭按钮
    WindowControlButton(
        icon = Icons.Default.Close,
        description = "Close",
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        onClick = { GlobalAppState.closeApp.value = true }
    )
}

@Composable
private fun RowScope.WindowControlButton(
    icon: ImageVector,
    description: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        modifier = Modifier
            .size(36.dp)
            .padding(2.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        onClick = onClick,
        shape = CircleShape
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp)
        )
    }
}

private inline fun Modifier.thenIf(condition: Boolean): Modifier =
    if (condition) this else Modifier


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