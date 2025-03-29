package com.corner.ui.nav

import androidx.lifecycle.ViewModel
import com.corner.util.createCoroutineScope
import com.corner.util.thisLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseViewModel(dispatcher: CoroutineDispatcher = Dispatchers.Default):ViewModel() {
    val scope: CoroutineScope by lazy { createCoroutineScope(dispatcher) }
    val log: org.slf4j.Logger = thisLogger()
}