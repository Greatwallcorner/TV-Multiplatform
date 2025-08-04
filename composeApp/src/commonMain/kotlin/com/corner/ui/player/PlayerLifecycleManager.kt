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
                log.debug("播放器状态转换: {} -> {}", _lifecycleState.value, newState)

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
                    PlayerLifecycleState.Paused -> stopInternal()
                    PlayerLifecycleState.Initializing_Sync -> initializeSyncInternal()
                    PlayerLifecycleState.Ready -> readyInternal()
                    PlayerLifecycleState.Loading -> loadingInternal()
                    PlayerLifecycleState.Playing -> playingInternal()
                    PlayerLifecycleState.Ended -> endedInternal()
                    else -> Result.success(Unit)
                }
            } catch (e: Exception) {
                log.error("状态转换失败", e)
                _lifecycleState.value = PlayerLifecycleState.Error
                Result.failure(e)
            }
        }
    }

    // 状态可转换检查函数
    public fun canTransitionTo(target: PlayerLifecycleState): Boolean {
        // 相同状态转换允许（幂等操作）
        if (lifecycleState.value == target) return true

        return isValidTransition(lifecycleState.value, target)
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
     * 同步初始化
     */
    suspend fun initialize_sync(): Result<Unit> = transitionTo(PlayerLifecycleState.Initializing_Sync)

    private suspend fun initializeSyncInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                controller.vlcjFrameInit()
                _lifecycleState.value = PlayerLifecycleState.Initialized
                Result.success(Unit)
            } catch (e: Exception) {
                log.error("同步初始化失败", e)
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
                log.debug("LifecycleManager - 清理完成")
                Result.success(Unit)
            } finally {
                _isCleaning.value = false
            }
        }
    }

    /**
     * 停止播放
     */
    suspend fun stop(): Result<Unit> = transitionTo(PlayerLifecycleState.Paused)

    private suspend fun stopInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                controller.pause()
                Result.success(Unit)
            } catch (e: Exception) {
                log.error("停止播放失败", e)
                Result.failure(e)
            }
        }
    }

    suspend fun ended(): Result<Unit> = transitionTo(PlayerLifecycleState.Ended)
    private suspend fun endedInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                controller.stop()
                Result.success(Unit)
            } catch (e: Exception) {
                log.error("播放结束失败", e)
                Result.failure(e)
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

    suspend fun ready(): Result<Unit> = transitionTo(PlayerLifecycleState.Ready)
    private suspend fun readyInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                controller.playerReady.let { player ->
                    if (player) {
                        return@withContext Result.success(Unit)
                    }
                }
                return@withContext Result.failure(IllegalStateException("Player not initialized"))
            } catch (e: Exception) {
                log.error("就绪失败", e)
                Result.failure(e)
            }
        }
    }

    suspend fun loading(): Result<Unit> = transitionTo(PlayerLifecycleState.Loading)
    private suspend fun loadingInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                controller.playerLoading.let { player ->
                    if (player) {
                        return@withContext Result.success(Unit)
                    }
                }
                return@withContext Result.failure(IllegalStateException("Player not initialized"))
            } catch (e: Exception) {
                log.error("加载失败", e)
                Result.failure(e)
            }
        }
    }

    suspend fun start() = transitionTo(PlayerLifecycleState.Playing)

    private suspend fun playingInternal(): Result<Unit> {
        return withContext(lifecycleDispatcher) {
            try {
                controller.playerPlayering.let { player ->
                    if (player) {
                        return@withContext Result.success(Unit)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                log.error("播放失败", e)
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
                PlayerLifecycleState.Released,
                PlayerLifecycleState.Initializing_Sync,
            )

            PlayerLifecycleState.Initializing -> to in listOf(
                PlayerLifecycleState.Initialized,
                PlayerLifecycleState.Error,
                PlayerLifecycleState.Cleaning,
                PlayerLifecycleState.Paused
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

            PlayerLifecycleState.Initializing_Sync -> to in listOf(
                PlayerLifecycleState.Error,
                PlayerLifecycleState.Cleaning,
                PlayerLifecycleState.Loading,
                PlayerLifecycleState.Paused,
                PlayerLifecycleState.Initialized
            )

            PlayerLifecycleState.Released -> to == PlayerLifecycleState.Idle
        }
    }
}