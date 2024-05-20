package com.corner.ui.player.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import java.awt.Component

@Composable
fun ComponentContainer(modifier: Modifier = Modifier, component: Component) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        SwingPanel(
            background = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            factory = { component }
        )
    }
}