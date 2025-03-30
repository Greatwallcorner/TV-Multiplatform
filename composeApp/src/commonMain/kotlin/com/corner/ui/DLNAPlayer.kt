package com.corner.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.corner.bean.SettingStore
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.ControlBar
import com.corner.ui.scene.ToolTipText
import org.jetbrains.compose.resources.painterResource
import tv_multiplatform.composeapp.generated.resources.Res
import tv_multiplatform.composeapp.generated.resources.TV_icon_x


@Composable
fun WindowScope.DLNAPlayer(vm:DetailViewModel, onClickBack:() -> Unit) {
    val model = vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    val detail by rememberUpdatedState(model.value.detail)

    val controller = rememberUpdatedState(vm.controller)

    val isFullScreen = GlobalAppState.videoFullScreen.collectAsState()
//
//    val videoHeight = derivedStateOf { if (isFullScreen.value) 1f else 0.6f }
//    val videoWidth = derivedStateOf { if (isFullScreen.value) 1f else 0.7f }

    LaunchedEffect(model.value.isLoading) {
        if (model.value.isLoading) {
            showProgress()
        } else {
            hideProgress()
        }
    }

    val focus = remember { FocusRequester() }

    LaunchedEffect(isFullScreen.value) {
        focus.requestFocus()
    }

    DisposableEffect(Unit) {
        vm.controller.init()
        onDispose {
            vm.clear()
        }

    }
    Column {
        if (!isFullScreen.value) {
            WindowDraggableArea {
                ControlBar(title = {
                    Text(text = "DLNA", style = MaterialTheme.typography.headlineMedium)
                }, leading = {
                    BackRow(Modifier, onClickBack = {
                        onClickBack()
                    }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.Start) {
                                ToolTipText(
                                    detail.vodName ?: "",
                                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                                    modifier = Modifier.padding(start = 50.dp)
                                )
                            }
                        }
                    }
                })
            }
        }

        val mrl = derivedStateOf { model.value.currentPlayUrl }
        Row(
            modifier = Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val internalPlayer = derivedStateOf {
                SettingStore.getPlayerSetting()[0] as Boolean
            }
            if (internalPlayer.value) {
                SideEffect {
                    focus.requestFocus()
                }
                Player(
                    mrl.value,
                    controller.value,
                    Modifier.fillMaxWidth().focusable(),
                    vm,
                    focusRequester = focus
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight().background(Color.Black)
                ) {
                    Column(
                        Modifier.fillMaxSize().align(Alignment.Center),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            painter = painterResource(Res.drawable.TV_icon_x),
                            contentDescription = "nothing here",
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "使用外部播放器",
                            modifier = Modifier.align(Alignment.CenterHorizontally).focusRequester(focus),
                            fontWeight = FontWeight.Bold,
                            fontSize = TextUnit(23f, TextUnitType.Sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
//            AnimatedVisibility(!isFullScreen.value, modifier = Modifier.fillMaxSize()) {
//                EpChooser(
//                    vm, Modifier.fillMaxSize().background(
//                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
//                        shape = RoundedCornerShape(4.dp)
//                    ).padding(horizontal = 5.dp)
//                )
//            }
        }
    }
}