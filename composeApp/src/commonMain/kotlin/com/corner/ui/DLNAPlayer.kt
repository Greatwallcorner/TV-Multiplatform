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
import com.corner.bean.SettingType
import com.corner.bean.enums.PlayerType
import com.corner.bean.getPlayerSetting
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.catvodcore.viewmodel.GlobalAppState.hideProgress
import com.corner.catvodcore.viewmodel.GlobalAppState.showProgress
import com.corner.ui.nav.vm.DetailViewModel
import com.corner.ui.scene.BackRow
import com.corner.ui.scene.ControlBar
import com.corner.ui.scene.ToolTipText
import com.corner.ui.scene.emptyShow
import com.corner.util.play.Play
import kotlinx.coroutines.launch


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
        scope.launch{
            GlobalAppState.DLNAUrl.collect{
                vm.setPlayUrl(it!!)
            }
        }
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
                SettingStore.getSettingItem(SettingType.PLAYER).getPlayerSetting().first() == PlayerType.Innie.id
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
                LaunchedEffect(internalPlayer.value) {
                    if(!internalPlayer.value) {
                        Play.start(mrl.value, "")
                    }
                }
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight().background(Color.Black)
                ) {
                    // 使用 emptyShow 方法替换原有的 Column 布局
                    emptyShow(
                        modifier = Modifier.align(Alignment.Center),
                        title = "使用外部播放器",
                        subtitle = "",
                        showRefresh = false
                    )
                }
            }
        }
    }
}