package com.corner.ui

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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DLNAPlayer")

@OptIn(FlowPreview::class)
@Composable
fun WindowScope.DLNAPlayer(vm:DetailViewModel, onClickBack:() -> Unit) {
    val model = vm.state.collectAsState()

    val scope = rememberCoroutineScope()

    val detail by rememberUpdatedState(model.value.detail)

    val controller = rememberUpdatedState(vm.controller)

    val isFullScreen = GlobalAppState.videoFullScreen.collectAsState()

    val mrl = derivedStateOf { model.value.currentPlayUrl }

    val focus = remember { FocusRequester() }

    val isUrlReady = remember { mutableStateOf(false) }

    val setUrlMutex = remember { Mutex() }

    LaunchedEffect(model.value.isLoading) {
        if (model.value.isLoading) {
            showProgress()
        } else {
            hideProgress()
        }
    }

    LaunchedEffect(isFullScreen.value) {
        if (isFullScreen.value) {
            delay(100) // 确保组件已渲染
            focus.requestFocus()
        }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            GlobalAppState.DLNAUrl
                .debounce(1000)
                .collect { newUrl ->
                    if (newUrl.isNotEmpty()) {
                        setUrlMutex.withLock {
                            isUrlReady.value = true
                            vm.setPlayUrl(newUrl)
                        }
                    }
                }
        }

        onDispose {
            job.cancel()
            if (vm.vmPlayerType.first() == PlayerType.Innie.id && !GlobalAppState.closeApp.value){
                log.debug("DLNA - 调用DetailViewMode销毁播放器")
                vm.clear()
            }
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

        Row(
            modifier = Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val internalPlayer = derivedStateOf {
                SettingStore.getSettingItem(SettingType.PLAYER).getPlayerSetting().first() == PlayerType.Innie.id
            }
            log.debug("internalPlayer:${internalPlayer.value}")
            if (internalPlayer.value && isUrlReady.value) {
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