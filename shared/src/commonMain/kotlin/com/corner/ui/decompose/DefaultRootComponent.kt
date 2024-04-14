package com.corner.ui.decompose

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.corner.ui.decompose.component.DefaultHistoryComponentComponent
import com.corner.ui.decompose.component.DefaultSearchComponentComponent
import com.corner.ui.decompose.component.DefaultSettingComponent
import com.corner.ui.decompose.component.DefaultVideoComponent
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

    override fun onClickBack() {
        navigation.pop()
    }

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Video -> RootComponent.Child.VideoChild(DefaultVideoComponent(componentContext))
            is Config.Search -> RootComponent.Child.SearchChild(DefaultSearchComponentComponent(componentContext))
            is Config.History -> RootComponent.Child.HistoryChild(DefaultHistoryComponentComponent(componentContext))
            is Config.Setting -> RootComponent.Child.SettingChild(DefaultSettingComponent(componentContext))
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
}