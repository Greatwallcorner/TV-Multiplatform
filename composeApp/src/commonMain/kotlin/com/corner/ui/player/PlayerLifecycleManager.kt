package com.corner.ui.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.slf4j.LoggerFactory

/**
 * 统一播放器生命周期管理器
 * 负责协调播放器状态转换和资源管理
 */
class PlayerLifecycleManager(
    private val controller: PlayerController,
    private val scope: CoroutineScope
) {
    private val log = LoggerFactory.getLogger("PlayerLifecycleManager")

    private val _lifecycleState = MutableStateFlow(PlayerLifecycleState.Idle)
    val lifecycleState: StateFlow<PlayerLifecycleState> = _lifecycleState

    // 将lifecycleDispatcher从Dispatchers.Swing改为Dispatchers.IO
    private val lifecycleDispatcher = Dispatchers.IO

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning

    /**
     * 状态转换
     */
    suspend fun transitionTo(newState: PlayerLifecycleState): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                log.debug("播放器状态转换: ${_lifecycleState.value} -> $newState")

                // 状态验证
                if (!isValidTransition(_lifecycleState.value, newState)) {
                    log.warn("无效的状态转换: ${_lifecycleState.value} -> $newState")
                    return@withContext Result.failure(
                        IllegalStateException("无效的状态转换: ${_lifecycleState.value} -> $newState")
                    )
                }

                _lifecycleState.value = newState

                // 执行状态对应的操作
                when (newState) {
                    PlayerLifecycleState.Initializing -> initializeInternal()
                    PlayerLifecycleState.Cleaning -> cleanupInternal()
                    PlayerLifecycleState.Released -> releaseInternal()
                    else -> Result.success(Unit)
                }
            } catch (e: Exception) {
                log.error("状态转换失败", e)
                _lifecycleState.value = PlayerLifecycleState.Error
                Result.failure(e)
            }
        }
    }

    /**
     * 异步初始化
     */
    suspend fun initialize(): Result<Unit> = transitionTo(PlayerLifecycleState.Initializing)

    private suspend fun initializeInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                controller.initAsync()
                Result.success(Unit)
            } catch (e: Exception) {
                log.error("初始化失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 异步清理
     */
    suspend fun cleanup(): Result<Unit> = transitionTo(PlayerLifecycleState.Cleaning)

    private suspend fun cleanupInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                _isCleaning.value = true
                controller.cleanupAsync()
                Result.success(Unit)
            } finally {
                _isCleaning.value = false
            }
        }
    }


    /**
     * 完全释放资源
     */
    suspend fun release(): Result<Unit> = transitionTo(PlayerLifecycleState.Released)

    private suspend fun releaseInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                // 检查当前状态，避免重复释放
                if (_lifecycleState.value == PlayerLifecycleState.Released) {
                    log.debug("资源已释放，跳过重复释放")
                    return@withContext Result.success(Unit)
                }

                // 先设置状态，再执行释放
                _lifecycleState.value = PlayerLifecycleState.Released

                synchronized(controller) {
                    try {
                        controller.dispose()
                    } catch (e: Exception) {
                        // 忽略已释放的错误
                        if (e.message?.contains("Invalid memory access") == true ||
                            e.message?.contains("already released") == true
                        ) {
                            log.debug("资源已释放，忽略错误")
                        } else {
                            throw e
                        }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                log.error("释放失败", e)
                Result.failure(e)
            }
        }
    }


    /**
     * 验证状态转换的合法性
     */
    private fun isValidTransition(from: PlayerLifecycleState, to: PlayerLifecycleState): Boolean {
        return when (from) {
            PlayerLifecycleState.Idle -> to in listOf(
                PlayerLifecycleState.Initializing,
                PlayerLifecycleState.Cleaning,  // 新增：允许从Idle直接清理
                PlayerLifecycleState.Released
            )

            PlayerLifecycleState.Initializing -> to in listOf(
                PlayerLifecycleState.Initialized,
                PlayerLifecycleState.Error,
                PlayerLifecycleState.Cleaning
            )

            PlayerLifecycleState.Initialized -> to in listOf(
                PlayerLifecycleState.Loading,
                PlayerLifecycleState.Cleaning
            )

            PlayerLifecycleState.Loading -> to in listOf(
                PlayerLifecycleState.Ready,
                PlayerLifecycleState.Error,
                PlayerLifecycleState.Cleaning
            )

            PlayerLifecycleState.Ready -> to in listOf(
                PlayerLifecycleState.Playing,
                PlayerLifecycleState.Paused,
                PlayerLifecycleState.Cleaning
            )

            PlayerLifecycleState.Playing -> to in listOf(
                PlayerLifecycleState.Paused,
                PlayerLifecycleState.Ended,
                PlayerLifecycleState.Cleaning
            )

            PlayerLifecycleState.Paused -> to in listOf(
                PlayerLifecycleState.Playing,
                PlayerLifecycleState.Ended,
                PlayerLifecycleState.Cleaning
            )

            PlayerLifecycleState.Ended -> to in listOf(
                PlayerLifecycleState.Ready,
                PlayerLifecycleState.Cleaning
            )

            PlayerLifecycleState.Cleaning -> to in listOf(
                PlayerLifecycleState.Initialized,
                PlayerLifecycleState.Released,
                PlayerLifecycleState.Error
            )

            PlayerLifecycleState.Error -> to in listOf(
                PlayerLifecycleState.Cleaning,
                PlayerLifecycleState.Released
            )

            PlayerLifecycleState.Released -> to == PlayerLifecycleState.Idle
        }
    }
}