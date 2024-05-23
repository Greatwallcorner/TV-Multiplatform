package com.corner.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.corner.ui.player.vlcj.VlcjController

@Composable
expect fun VideoPlayer(
    mrl: String,
//    videoInfo: VideoInfo,
    state: VlcjController,
    modifier: Modifier = Modifier,
)
