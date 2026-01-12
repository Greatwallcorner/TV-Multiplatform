package com.corner.util.scope

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun createCoroutineScope(dispatcher: CoroutineDispatcher = Dispatchers.Default): CoroutineScope {
    return CoroutineScope(dispatcher + SupervisorJob())
}