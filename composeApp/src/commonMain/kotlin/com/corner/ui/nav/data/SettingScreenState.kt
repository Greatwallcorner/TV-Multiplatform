package com.corner.ui.nav.data

import com.corner.bean.Setting
import com.corner.database.entity.Config


data class SettingScreenState(
    var settingList: List<Setting> = emptyList(),
    var version: Int = 1,
    var dbConfigList: MutableList<Config> = mutableListOf()
)