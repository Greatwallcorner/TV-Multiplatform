package com.corner.ui.decompose

import com.arkivanov.decompose.value.Value

interface DialogComponent {
    val model: Value<Model>

    fun onClickClose()

    data class Model(
        val showDialog:Boolean = false,
    )
}