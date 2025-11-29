package com.corner.ui.nav

import androidx.lifecycle.ViewModel
import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.util.thisLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class BaseViewModel(dispatcher: CoroutineDispatcher = Dispatchers.Default):ViewModel() {
    val scope: CoroutineScope by lazy {
        // 使用全局的 CoroutineScope，最好保持适当整合
        CoroutineScope(GlobalAppState.rootScope.coroutineContext + dispatcher)
    }
    val log: org.slf4j.Logger = thisLogger()
}