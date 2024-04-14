package com.corner.ui.decompose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.corner.catvodcore.enum.Menu
import com.corner.ui.AppTheme
import com.corner.ui.ControlBar
import com.corner.ui.HistoryScene
import com.corner.ui.SettingScene
import com.corner.ui.scene.LoadingIndicator
import com.corner.ui.scene.SnackBar
import com.corner.ui.scene.isShowProgress
import com.corner.ui.search.SearchScene
import com.corner.ui.video.videoScene


@Composable
fun WindowScope.RootContent(component: RootComponent, modifier: Modifier = Modifier, state:WindowState, onClose:()->Unit) {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).border(
                border = BorderStroke(1.dp, Color(59, 59, 60)), // firefox的边框灰色
            ).shadow(
                5.dp, shape = RoundedCornerShape(8.dp),
                ambientColor = Color.DarkGray, spotColor = Color.DarkGray
            )
        ) {
            WindowDraggableArea {
                ControlBar(onClickMinimize = { state.isMinimized = !state.isMinimized },
                    onClickMaximize = {
                        state.placement =
                            if (WindowPlacement.Maximized == state.placement) WindowPlacement.Floating else WindowPlacement.Maximized
                    },
                    onClickClose = { onClose() })
            }
            Children(stack = component.childStack, modifier = modifier, animation = stackAnimation(fade())){
                when (val child = it.instance) {
                    is RootComponent.Child.VideoChild -> videoScene(child.component, modifier = Modifier){menu->
                        when(menu){
                            Menu.SEARCH -> component.onClickSearch()
                            Menu.HOME -> component.backToHome()
                            Menu.SETTING -> component.onClickSetting()
                            Menu.HISTORY -> component.onClickHistory()
                        }
                    }
                    is RootComponent.Child.SearchChild -> SearchScene(child.component){component.onClickBack()}
                    is RootComponent.Child.HistoryChild -> HistoryScene(child.component){component.onClickBack()}
                    is RootComponent.Child.SettingChild -> SettingScene(child.component, modifier){component.onClickBack()}
                }

//            Box {
//                AnimatedContent(currentChoose,
//                    modifier = Modifier.background(color = MaterialTheme.colors.background),
//                    transitionSpec = {
//                        fadeIn(initialAlpha = 0.3f) togetherWith fadeOut()
////                         slideInVertically (
////                             animationSpec = tween(150),
////                             initialOffsetY = { fullHeight -> fullHeight }
////                         ) togetherWith
////                                 slideOutVertically(
////                                     animationSpec = tween(200),
////                                     targetOffsetY = { fullHeight -> -fullHeight }
////                                 )
//                    }) {
//                    when (currentChoose) {
//                        Menu.HOME -> videoScene(
//                            modifier = Modifier,
//                            onClickSwitch = { menu -> currentChoose = menu })
//
//                        Menu.SETTING -> SettingScene(modifier = Modifier, onClickBack = { currentChoose = Menu.HOME })
//                        Menu.SEARCH -> SearchScene(onClickBack = { currentChoose = Menu.HOME })
//                        Menu.HISTORY -> HistoryScene { currentChoose = Menu.HOME }
//                    }
//                }
                SnackBar.SnackBarList()
                LoadingIndicator(showProgress = isShowProgress())
            }
        }
    }
}