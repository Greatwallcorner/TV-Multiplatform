package com.corner.ui.decompose

import com.corner.util.createCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseComponent(dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    val scope: CoroutineScope by lazy { createCoroutineScope(dispatcher) }
}