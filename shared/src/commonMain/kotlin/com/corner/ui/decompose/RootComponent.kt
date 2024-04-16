package com.corner.ui.decompose

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.corner.catvod.enum.bean.Vod
import com.corner.ui.decompose.component.DefaultHistoryComponentComponent
import com.corner.ui.decompose.component.DefaultSearchComponentComponent
import com.corner.ui.decompose.component.DefaultSettingComponent
import com.corner.ui.decompose.component.DefaultVideoComponent

interface RootComponent{
    val childStack:Value<ChildStack<*, Child>>

    fun onClickSearch()

    fun onClickHistory()

    fun backToHome()

    fun onClickSetting()

    fun showChooseHomeDialog()

    fun showDetail(vod: Vod)


    fun onClickBack()


    sealed class Child{
        class VideoChild(val component:DefaultVideoComponent):Child()

        class SearchChild(val component: DefaultSearchComponentComponent):Child()

        class HistoryChild(val component: DefaultHistoryComponentComponent):Child()

        class SettingChild(val component: DefaultSettingComponent):Child()

        class DetailChild(val component: DetailComponent):Child()
    }

//    val dialogSlot: Value<ChildSlot<DialogConfig, DialogComponent>>
}
