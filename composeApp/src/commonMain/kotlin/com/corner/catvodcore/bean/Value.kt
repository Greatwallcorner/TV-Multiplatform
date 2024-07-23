package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable

@Serializable
data class Value(val n:String?=null, var v:String?=null, var selected:Boolean = false)