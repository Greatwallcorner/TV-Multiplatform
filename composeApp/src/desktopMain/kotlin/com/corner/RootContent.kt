package com.corner

import AppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.corner.catvodcore.enum.Menu
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.DetailScene
import com.corner.ui.HistoryScene
import com.corner.ui.SettingScene
import com.corner.ui.decompose.RootComponent
import com.corner.ui.scene.LoadingIndicator
import com.corner.ui.scene.SnackBar
import com.corner.ui.scene.isShowProgress
import com.corner.ui.search.SearchScene
import com.corner.ui.video.VideoScene
import com.corner.util.FirefoxGray


@Composable
fun WindowScope.RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    val isFullScreen = GlobalModel.videoFullScreen.subscribeAsState()
    val modifierVar = derivedStateOf {
        if (isFullScreen.value) {
            Modifier.fillMaxSize().border(border = BorderStroke(0.dp, Color.Transparent))
        } else {
            Modifier.fillMaxSize().border(BorderStroke(1.dp, Color.FirefoxGray)).shadow(15.dp)
//            if(SysVerUtil.isWin10()){
//                Modifier.fillMaxSize().border(BorderStroke(1.dp, Color.FirefoxGray)).shadow(15.dp)
//            }else{
//                Modifier.fillMaxSize().border(BorderStroke(1.dp, Color.FirefoxGray), shape = RoundedCornerShape(10.dp))
//                    .clip(RoundedCornerShape(10.dp)).shadow(elevation = 8.dp, ambientColor = Color.DarkGray, spotColor = Color.DarkGray)
//            }
        }

    }
//    val isDebug = derivedStateOf { System.getProperty("org.gradle.project.buildType") == "debug" }


    AppTheme(useDarkTheme = true) {
        val url = LocalUriHandler.current
        Column(
            modifier = modifierVar.value
        ) {
//            Button(onClick = {
//                url.openUri("magnet:?xt=urn:btih:c2452263a6f43fdf9b0ef584d0c9dbe96c21d8a7&dn=%E9%9A%8F%E8%BA%AB%E5%8D%B1%E6%9C%BA.6v%E7%94%B5%E5%BD%B1%20%E5%9C%B0%E5%9D%80%E5%8F%91%E5%B8%83%E9%A1%B5%20www.6v123.net%20%E6%94%B6%E8%97%8F%E4%B8%8D%E8%BF%B7%E8%B7%AF")
//            }){
//                Text("下载")
//            }
            Children(stack = component.childStack, modifier = modifier.background(Color.Transparent), animation = stackAnimation(fade())) {
                when (val child = it.instance) {
                    is RootComponent.Child.VideoChild -> VideoScene(
                        child.component,
                        modifier = Modifier,
                        { component.showDetail(it) }) { menu ->
                        when (menu) {
                            Menu.SEARCH -> component.onClickSearch()
                            Menu.HOME -> component.backToHome()
                            Menu.SETTING -> component.onClickSetting()
                            Menu.HISTORY -> component.onClickHistory()
                        }
                    }

                    is RootComponent.Child.SearchChild -> SearchScene(
                        child.component,
                        { component.showDetail(it, true) }) { component.onClickBack() }

                    is RootComponent.Child.HistoryChild -> HistoryScene(
                        child.component,
                        { component.showDetail(it) }) { component.onClickBack() }

                    is RootComponent.Child.SettingChild -> SettingScene(child.component) { component.onClickBack() }
                    is RootComponent.Child.DetailChild -> DetailScene(child.component) { component.onClickBack() }
                }

                SnackBar.SnackBarList()
                LoadingIndicator(showProgress = isShowProgress())
            }
        }
//        if(isDebug.value){
//            FpsMonitor(Modifier)
//        }
    }
}