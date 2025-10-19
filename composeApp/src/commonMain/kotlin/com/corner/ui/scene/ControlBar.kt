package com.corner.ui.scene

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.corner.catvodcore.viewmodel.GlobalAppState
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ControlBar")

@Composable
fun ControlBar(
    title: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier.height(64.dp),
    leading: @Composable (() -> Unit)? = null,
    center: @Composable() (() -> Unit?)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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
                    if (leading != null && title != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    title?.invoke()
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
            thickness = 0.3.dp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}


@Composable
private fun RowScope.WindowControlButtons() {
    // 最小化按钮
    WindowControlButton(
        icon = Icons.Default.Minimize,
        description = "Minimize",
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), // 半透明表面变体,
        contentColor = MaterialTheme.colorScheme.primary,
        onClick = {
            GlobalAppState.windowState?.isMinimized = !GlobalAppState.windowState?.isMinimized!!
        }
    )

    // 最大化/还原按钮
    WindowControlButton(
        icon = if (GlobalAppState.windowState?.placement == WindowPlacement.Maximized)
            Icons.Default.KeyboardArrowUp else Icons.Default.CropSquare,
        description = "Maximize/Restore",
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), // 半透明表面变体,,
        contentColor = MaterialTheme.colorScheme.primary,
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
        containerColor = Color.Red,
        contentColor = MaterialTheme.colorScheme.error,
        iconTint = Color.White,
        onClick = { // 在关闭应用时
            log.info("Close App")
            GlobalAppState.closeApp.value = true
        }
    )
}

@Composable
private fun RowScope.WindowControlButton(
    icon: ImageVector,
    description: String,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = contentColor,
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
            tint = iconTint,  // 独立控制图标颜色
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