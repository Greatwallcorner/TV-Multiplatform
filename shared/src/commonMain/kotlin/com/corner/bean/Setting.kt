package com.corner.bean

import kotlinx.serialization.Serializable

/**
@author heatdesert
@date 2023-12-16 12:10
@description
 */
//class Setting {
//
//}
//
@Serializable
data class Setting(val id:String, val label:String, var value:String?)

enum class EditType{
    INPUT,
    CHOOSE
}
