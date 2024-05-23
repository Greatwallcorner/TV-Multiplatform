package com.corner.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap

@Composable
fun FrameContainer(
    modifier: Modifier = Modifier,
    size: IntSize,
    bytes: ByteArray?,
) {
    val bitmap by remember(size) {
        derivedStateOf {
            if (size.width > 0 && size.height > 0) Bitmap().apply {
                allocN32Pixels(size.width, size.height, true)
            }
            else null
        }
    }
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        bitmap?.let { bitmap ->
            bytes?.let { bytes ->
                Image(
                    bitmap = bitmap.run {
                        installPixels(bytes)
                        asComposeImageBitmap()
                    },
                    contentDescription = "frame"
                )
            }
        } ?: CircularProgressIndicator()
    }
}