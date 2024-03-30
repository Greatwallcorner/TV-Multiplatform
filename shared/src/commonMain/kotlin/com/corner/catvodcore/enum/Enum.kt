package com.corner.catvodcore.enum

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class ConfigType {
    SITE, LIVE
}

enum class Changeable{
    NO , YES
}

enum class Searchable{
    NO , YES
}

enum class QuickSearch{
    NO , YES
}

enum class Menu(val desc:String, val icon: ImageVector){
    HOME("首页", Icons.Outlined.Home),
    SETTING("设置", Icons.Outlined.Settings),
    SEARCH("搜索", Icons.Outlined.Search),
    HISTORY("历史", Icons.Outlined.History)
}