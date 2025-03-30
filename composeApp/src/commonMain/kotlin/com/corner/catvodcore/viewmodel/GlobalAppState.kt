package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.jupnp.UpnpService

object GlobalAppState{
    val showProgress = MutableStateFlow(false)
    var windowState:WindowState? = null
    val hotList = MutableStateFlow(listOf<HotData>())
    val chooseVod = mutableStateOf<Vod>(Vod())
    var detailFrom  = DetailFromPage.HOME
    val home = MutableStateFlow<Site>(Site.get("",""))
    val clear = MutableStateFlow<Boolean>(false)
    val closeApp = MutableStateFlow<Boolean>(false)
    var videoFullScreen = MutableStateFlow<Boolean>(false)
        private set
    var upnpService = mutableStateOf<UpnpService?>(null)
    var DLNAUrl = MutableStateFlow<String?>(null)

    fun clearHome(){
        home.value = Site.get("","")
    }

    fun toggleVideoFullScreen():Boolean{
        toggleWindowFullScreen()
        videoFullScreen.value = !videoFullScreen.value
        return videoFullScreen.value
    }

    private fun toggleWindowFullScreen(){
        if(windowState?.placement == WindowPlacement.Fullscreen){
            windowState?.placement = WindowPlacement.Floating
        }else{
            windowState?.placement = WindowPlacement.Fullscreen
        }
    }

    fun showProgress(){
        showProgress.update { true }
    }

    fun hideProgress(){
        showProgress.update { false }
    }

    fun isShowProgress():Boolean{
        return showProgress.value
    }
}

enum class DetailFromPage(){
    SEARCH, DLNA, HOME
}