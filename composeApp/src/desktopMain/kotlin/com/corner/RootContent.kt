package com.corner.ui.decompose

import AppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.catvodcore.enum.Menu
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.ControlBar
import com.corner.ui.DetailScene2
import com.corner.ui.HistoryScene
import com.corner.ui.SettingScene
import com.corner.ui.scene.LoadingIndicator
import com.corner.ui.scene.SnackBar
import com.corner.ui.scene.isShowProgress
import com.corner.ui.search.SearchScene
import com.corner.ui.video.VideoScene


@Composable
fun WindowScope.RootContent(component: RootComponent, modifier: Modifier = Modifier, state:WindowState, onClose:()->Unit) {
    val isFullScreen = GlobalModel.videoFullScreen.subscribeAsState()
    val borderStroke = derivedStateOf { BorderStroke(if(isFullScreen.value) 0.dp else 1.dp, Color(59, 59, 60)) }
    AppTheme(useDarkTheme = true) {
        Column(
            modifier = Modifier.fillMaxSize().border(
                border = borderStroke.value), // firefox的边框灰色
        ) {
            if(!isFullScreen.value){
                WindowDraggableArea {
                    ControlBar(onClickMinimize = { state.isMinimized = !state.isMinimized },
                        onClickMaximize = {
                            state.placement =
                                if (WindowPlacement.Maximized == state.placement) WindowPlacement.Floating else WindowPlacement.Maximized
                        },
                        onClickClose = { onClose() })
                }
            }
            Children(stack = component.childStack, modifier = modifier, animation = stackAnimation(fade())){
                when (val child = it.instance) {
                    is RootComponent.Child.VideoChild -> VideoScene(child.component, modifier = Modifier, {component.showDetail(it)}){menu->
                        when(menu){
                            Menu.SEARCH -> component.onClickSearch()
                            Menu.HOME -> component.backToHome()
                            Menu.SETTING -> component.onClickSetting()
                            Menu.HISTORY -> component.onClickHistory()
                        }
                    }
                    is RootComponent.Child.SearchChild -> SearchScene(child.component, {component.showDetail(it, true)}){component.onClickBack()}
                    is RootComponent.Child.HistoryChild -> HistoryScene(child.component, {component.showDetail(it)}){component.onClickBack()}
                    is RootComponent.Child.SettingChild -> SettingScene(child.component){component.onClickBack()}
                    is RootComponent.Child.DetailChild -> DetailScene2(child.component){component.onClickBack()}
                }

                SnackBar.SnackBarList()
                LoadingIndicator(showProgress = isShowProgress())
            }
        }
    }
}