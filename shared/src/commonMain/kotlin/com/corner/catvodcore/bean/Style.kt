package com.corner.catvod.enum.bean

import kotlinx.serialization.Serializable

@Serializable
data class Style (
    var type: String,
    var ratio: Float = 1f
)
