package com.corner.ui.player

/**
 * 播放器生命周期状态枚举
 * 统一追踪播放器从创建到释放的完整生命周期
 */
enum class PlayerLifecycleState {
    /** 初始状态，播放器未初始化 */
    Idle,

    /** 正在初始化播放器 */
    Initializing,

    /** 播放器初始化完成，可以加载媒体 */
    Initialized,

    /** 正在加载媒体 */
    Loading,

    /** 媒体加载完成，准备播放 */
    Ready,

    /** 正在播放 */
    Playing,

    /** 已暂停 */
    Paused,

    /** 播放结束 */
    Ended,

    /** 正在清理资源 */
    Cleaning,

    /** 已释放 */
    Released,

    /** 发生错误 */
    Error,

    /** 异步初始化 */
    Initializing_Sync,

    /** 异步停止播放 */
    Ended_Async
}
