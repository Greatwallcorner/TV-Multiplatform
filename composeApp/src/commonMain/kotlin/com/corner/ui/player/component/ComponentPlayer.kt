package com.corner.ui.player.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.corner.ui.player.PlayerController
import player.DefaultControls
import java.awt.Component

@Composable
fun ComponentPlayer(
    modifier: Modifier = Modifier,
    url: String,
    component: Component,
    controller: PlayerController,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
//        VideoPlayer(url,  )
        ComponentContainer(Modifier.weight(1f).background(Color.Transparent), component)
//        AnimatedVisibility(){
            DefaultControls(Modifier.fillMaxWidth(), controller)
//        }
        DisposableEffect(url) {
            controller.play(url)
            onDispose {  }
        }
    }

}