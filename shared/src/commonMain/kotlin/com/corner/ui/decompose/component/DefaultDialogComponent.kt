package com.corner.ui.decompose.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.corner.ui.decompose.DialogComponent

class DefaultDialogComponent(componentContext: ComponentContext, onClickClose:()->Unit): DialogComponent, ComponentContext by componentContext {
    private val _model = MutableValue(DialogComponent.Model())

    override val model: Value<DialogComponent.Model> = _model

    override fun onClickClose() {
        onClickClose()
    }

//    fun child(config:DialogConfig, componentContext: ComponentContext){
//        when(config){
//           is DialogConfig.DetailDialog ->
//               DefaultDialogComponent(componentContext = componentContext){
//                   dialogSlotNavigation.dismiss()
//               }
//            is DialogConfig.HomeChooseDialog ->
//                DefaultDialogComponent()
//        }
//    }
}