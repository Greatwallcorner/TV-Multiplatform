package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.corner.bean.SettingStore
import com.corner.ui.decompose.SettingComponent

class DefaultSettingComponent(componentContext: ComponentContext):SettingComponent, ComponentContext by componentContext {
    private val _model = MutableValue(SettingComponent.Model())

    override val model: MutableValue<SettingComponent.Model> = _model
    override fun sync() {
        _model.update { it.copy(settingList = SettingStore.getSettingList(), version = _model.value.version + 1) }
    }
}