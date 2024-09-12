package com.corner.catvodcore.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.HotData
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod

object GlobalModel {
    var windowState:WindowState? = null
    val hotList = MutableValue(listOf<HotData>())
    val chooseVod = mutableStateOf<Vod>(Vod())
    var detailFromSearch = false
    val home = MutableValue<Site>(Site.get("",""))
    val clear = MutableValue<Boolean>(false)
    val closeApp = MutableValue<Boolean>(false)
    var keyword = MutableValue<String>("")
    var videoFullScreen = MutableValue<Boolean>(false)
        private set
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
}