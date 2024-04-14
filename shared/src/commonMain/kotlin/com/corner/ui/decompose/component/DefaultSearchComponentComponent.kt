package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.corner.bean.SettingStore.getHistoryList
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.SearchComponent

class DefaultSearchComponentComponent(componentContext: ComponentContext):SearchComponent, ComponentContext by componentContext, BackHandlerOwner {


    private val _models:MutableValue<SearchComponent.Model> = MutableValue(
        SearchComponent.Model(GlobalModel.hotList.value,
            getHistoryList())
    )

    override val models: MutableValue<SearchComponent.Model> = _models

}