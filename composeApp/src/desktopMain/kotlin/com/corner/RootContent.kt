package com.corner

import AppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.enum.Menu
import com.corner.catvodcore.viewmodel.DetailFromPage
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.ui.DLNAPlayer
import com.corner.ui.DetailScene
import com.corner.ui.HistoryScene
import com.corner.ui.SettingScene
import com.corner.ui.nav.vm.*
import com.corner.ui.navigation.TVScreen
import com.corner.ui.scene.LoadingIndicator
import com.corner.ui.scene.SnackBar
import com.corner.ui.search.SearchScene
import com.corner.ui.video.VideoScene
import com.corner.util.FirefoxGray
import kotlinx.coroutines.launch


@Composable
fun WindowScope.RootContent(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val toDetail = fun(it: Vod, from:DetailFromPage) {
        GlobalAppState.chooseVod.value = it
        GlobalAppState.detailFrom = from
        navController.navigate(TVScreen.DetailScreen.name)
    }

    val isFullScreen = GlobalAppState.videoFullScreen.collectAsState()
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
//    System.setProperty("native.encoding", "UTF-8")
//    val isDebug = derivedStateOf { System.getProperty("org.gradle.project.buildType") == "debug" }

    val scope = rememberCoroutineScope()
    scope.launch{
        GlobalAppState.DLNAUrl.collect{
            if(it.isNullOrBlank()) return@collect
            navController.navigate(TVScreen.DLNAPlayerScreen.name)
        }
    }


    AppTheme {
        Box(
            modifier = modifierVar.value.background(Color.Black)
        ) {
            NavHost(
                navController,
                startDestination = TVScreen.VideoScreen.name
            ) {
                composable(TVScreen.VideoScreen.name) {
                    VideoScene(
                        viewModel { VideoViewModel() },
                        modifier = Modifier,
                        { toDetail(it, DetailFromPage.HOME) }) { menu ->
                        when (menu) {
                            Menu.SEARCH -> navController.navigate(TVScreen.SearchScreen.name)
                            Menu.HOME -> navController.navigate(TVScreen.VideoScreen.name)
                            Menu.SETTING -> navController.navigate(TVScreen.SettingsScreen.name)
                            Menu.HISTORY -> navController.navigate(TVScreen.HistoryScreen.name)
                        }
                    }
                }

                composable(TVScreen.DetailScreen.name) {
                    DetailScene(
                        viewModel { DetailViewModel() }
                    ) { navController.popBackStack() }
                }

                composable(TVScreen.SearchScreen.name) {
                    SearchScene(
                        viewModel { SearchViewModel() },
                        { toDetail(it, DetailFromPage.SEARCH) }) { navController.popBackStack() }
                }

                composable(TVScreen.HistoryScreen.name) {
                    HistoryScene(
                        viewModel { HistoryViewModel() },
                        { toDetail(it, DetailFromPage.HOME) }) { navController.popBackStack() }
                }

                composable(TVScreen.SettingsScreen.name) {
                    SettingScene(viewModel { SettingViewModel() }) { navController.popBackStack() }
                }

                composable(TVScreen.DLNAPlayerScreen.name) {
                    val viewModel = viewModel { DetailViewModel() }
                    viewModel.setPlayUrl(GlobalAppState.DLNAUrl.value ?: "")
                    DLNAPlayer(viewModel){
                        navController.popBackStack()
                    }
                }
            }
            SnackBar.SnackBarList()
            val showProgress = GlobalAppState.showProgress.collectAsState()
            LoadingIndicator(showProgress = showProgress.value,withOverlay = true)
        }
    }
//        if(isDebug.value){
//            FpsMonitor(Modifier)
//        }
}