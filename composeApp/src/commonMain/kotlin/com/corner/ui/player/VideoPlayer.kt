package com.rjuszczyk.compose


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(
    mrl: String,
//    videoInfo: VideoInfo,
    state: VideoPlayerState,
    modifier: Modifier = Modifier,
)
