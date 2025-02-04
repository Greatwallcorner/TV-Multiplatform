package com.corner.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun createCoroutineScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob())
}