package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.bean.Setting

interface SettingComponent {
    val model: MutableValue<Model>

    data class Model(
        var settingList: List<Setting> = emptyList(),
        var version: Int = 1
    )

    fun sync()

}