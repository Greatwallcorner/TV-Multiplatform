package com.corner.ui.decompose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.database.History
import com.corner.ui.decompose.component.*
import kotlinx.serialization.Serializable

class DefaultRootComponent(componentContext: ComponentContext): RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Video,
        handleBackButton = true,
        childFactory = ::child)

    override val childStack: Value<ChildStack<*, RootComponent.Child>>
        get() = stack


//    private val dialogSlotNavigation = SlotNavigation<DialogConfig>()
//
//    private val _dialogSlot =
//        childSlot(source = dialogSlotNavigation,
//            serializer = null,
//            handleBackButton = true){  config, childComponentContext ->
//            when(config){
//                is DialogConfig.DetailDialog -> DefaultDetailComponent(childComponentContext){
//                    dialogSlotNavigation.dismiss() as DialogComponent
//                }
//                else -> DefaultDialogComponent(childComponentContext){
//                    dialogSlotNavigation.dismiss()
//                }
//            }
//        }
//    override val dialogSlot: Value<ChildSlot<DialogConfig, DialogComponent>> = _dialogSlot as Value<ChildSlot<DialogConfig, DialogComponent>>
    override fun onClickSearch() {
        navigation.bringToFront(Config.Search)
    }

    override fun onClickHistory() {
        navigation.bringToFront(Config.History)
    }

    override fun backToHome() {
        navigation.bringToFront(Config.Video)
    }

    override fun onClickSetting() {
        navigation.bringToFront(Config.Setting)
    }

    override fun showChooseHomeDialog() {
//        dialogSlotNavigation.activate(DialogConfig.HomeChooseDialog)
    }

    override fun showDetail(vod: Vod, fromSearch:Boolean) {
        GlobalModel.chooseVod.value = vod
        GlobalModel.detailFromSearch = fromSearch
        navigation.bringToFront(Config.Detail)
    }

    override fun onClickBack() {
        navigation.pop()
    }

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Video -> RootComponent.Child.VideoChild(DefaultVideoComponent(componentContext))
            is Config.Search -> RootComponent.Child.SearchChild(DefaultSearchPagesComponent(componentContext))
            is Config.History -> RootComponent.Child.HistoryChild(DefaultHistoryComponent(componentContext))
            is Config.Setting -> RootComponent.Child.SettingChild(DefaultSettingComponent(componentContext))
            is Config.Detail -> RootComponent.Child.DetailChild(DefaultDetailComponent(componentContext))
        }
}

@Serializable
private sealed interface Config{
    @Serializable
    data object Video:Config

    @Serializable
    data object Search:Config

    @Serializable
    data object History:Config

    @Serializable
    data object Setting:Config

    @Serializable
    data object Detail:Config
}
