package com.corner.ui.decompose.component

import SiteViewModel
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.viewmodel.GlobalModel
import com.corner.ui.decompose.VideoComponent
import com.corner.ui.scene.hideProgress
import com.corner.ui.scene.showProgress
import com.corner.util.isEmpty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DefaultVideoComponent(componentContext: ComponentContext):VideoComponent, ComponentContext by componentContext, BackHandlerOwner {

    private val _log = LoggerFactory.getLogger("Video")

    override var log: Logger = _log

    private val _model = MutableValue(VideoComponent.Model())

    override var model: MutableValue<VideoComponent.Model> = _model

    private var promptJob: Job? = null;

    init {
        GlobalModel.home.observe {
            homeLoad()
        }
        GlobalModel.hotList.observe {
            if(it.isEmpty()) return@observe
            searchBarPrompt()
        }

        lifecycle.subscribe(callbacks = object :Lifecycle.Callbacks{
            override fun onCreate() {
                println("onCreate")
            }

            override fun onStart() {
                println("onStart")
//                searchBarPrompt()
//                homeLoad()
            }

            override fun onDestroy() {
                println("onDestroy")
            }

            override fun onPause() {
                println("onPause")
            }

            override fun onResume() {
                println("onResume")
            }

            override fun onStop() {
                println("onStop")
//                promptJob?.cancel()
            }
        })
    }

    override fun homeLoad() {
        val home = GlobalModel.home
        SiteViewModel.viewModelScope.launch {
            showProgress()
            try {
                if (!_model.value.homeLoaded) {
                    if (home.value.isEmpty()) return@launch
                    var list = SiteViewModel.homeContent().list.toMutableSet()
                    var classList = SiteViewModel.result.value.types.toMutableSet()

                    // 有首页内容
                    if (list.isNotEmpty()) {
                        classList = (mutableSetOf(Type.home()) + classList).toMutableSet()
                    } else {
                        val types = SiteViewModel.result.value.types
                        if (types.isEmpty()) return@launch
                        SiteViewModel.categoryContent(
                            home.value.key ,
                            types.get(0).typeId,
                            _model.value.page.toString(),
                            true,
                            HashMap()
                        )
                        list = SiteViewModel.result.value.list.toMutableSet()
                    }
                    val currentClass = classList.firstOrNull()
                    _model.value.homeLoaded = true
                    _model.update { it.copy(homeVodResult = list, currentClass = currentClass, classList = classList) }
                }
            } catch (e: Exception) {
                log.error("homeLoad", e)
            } finally {
                hideProgress()
            }
        }
        _model.value.page += 1
    }

    fun searchBarPrompt(){
        promptJob = SiteViewModel.viewModelScope.launch {
            if (model.value.isRunning) return@launch
            model.update { it.copy(isRunning = true) }
            delay(1500) // 等待获取热门数据列表
            var idx = 0

            while (true) {
                delay(2000)
                if (idx >= GlobalModel.hotList.value.size) idx = 0
                model.update { it.copy(prompt = GlobalModel.hotList.value[idx++].title) }
            }
        }
        promptJob?.invokeOnCompletion {
            println("scroll invoke complete")
            model.value.isRunning = false
        }
    }
}