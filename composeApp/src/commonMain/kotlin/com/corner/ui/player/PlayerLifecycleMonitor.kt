package com.corner.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * 播放器生命周期监控器
 * 用于调试和监控播放器状态变化
 */
class PlayerLifecycleMonitor(
    private val lifecycleManager: PlayerLifecycleManager,
    private val scope: CoroutineScope
) {
    private val log = LoggerFactory.getLogger("PlayerLifecycleMonitor")

    init {
        scope.launch {
            lifecycleManager.lifecycleState.collectLatest { state ->
                log.debug("播放器状态变更: $state")
                // 可以在这里添加性能监控、埋点等
            }
        }
    }

    /**
     * 获取当前状态描述
     */
    fun getCurrentStateDescription(): String {
        return when (lifecycleManager.lifecycleState.value) {
            PlayerLifecycleState.Idle -> "播放器空闲"
            PlayerLifecycleState.Initializing -> "正在初始化播放器"
            PlayerLifecycleState.Initialized -> "播放器已初始化"
            PlayerLifecycleState.Loading -> "正在加载媒体"
            PlayerLifecycleState.Ready -> "媒体加载完成"
            PlayerLifecycleState.Playing -> "正在播放"
            PlayerLifecycleState.Paused -> "已暂停"
            PlayerLifecycleState.Ended -> "播放结束"
            PlayerLifecycleState.Cleaning -> "正在清理资源"
            PlayerLifecycleState.Released -> "播放器已释放"
            PlayerLifecycleState.Error -> "播放器发生错误"
            PlayerLifecycleState.Initializing_Sync -> "同步初始化中"
        }
    }
}