package com.corner.dlna

import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.ui.player.PlayState
import com.corner.ui.player.vlcj.VlcJInit
import com.corner.ui.scene.SnackBar
import com.corner.util.thisLogger
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.support.avtransport.AbstractAVTransportService
import org.jupnp.support.model.*


class TvAvTransportService : AbstractAVTransportService() {
    private val log = thisLogger()
    override fun getCurrentInstanceIds(): Array<UnsignedIntegerFourBytes> {
        return arrayOf(UnsignedIntegerFourBytes(0x01))
    }

    override fun setAVTransportURI(
        instanceId: UnsignedIntegerFourBytes,
        currentURI: String,
        currentURIMetaData: String
    ) {

        runBlocking {
            GlobalAppState.DLNAUrl.emit("")
            GlobalAppState.DLNAUrl.emit(currentURI)
            SnackBar.postMsg("投屏：$currentURI", type = SnackBar.MessageType.INFO)
        }
        log.info("setAvTransportURI instance id: $instanceId currentURI: $currentURI currentURIMetaData: $currentURIMetaData")
    }

    override fun setNextAVTransportURI(p0: UnsignedIntegerFourBytes?, p1: String?, p2: String?) {
        TODO("Not yet implemented")
    }

    override fun getMediaInfo(p0: UnsignedIntegerFourBytes?): MediaInfo {
        log.debug("getMediaInfo instance id: {}", p0)

        val currentURI = runBlocking { GlobalAppState.DLNAUrl.value }

        return MediaInfo(
            currentURI,           // currentURI
            "",                   // currentURIMetaData (元数据，可以为空)
            UnsignedIntegerFourBytes(1), // numberOfTracks (轨道数)
            "00:00:00",          // mediaDuration (媒体时长，格式为HH:mm:ss)
            StorageMedium.NONE   // playMedium (播放介质)
        )
    }

    override fun getTransportInfo(p0: UnsignedIntegerFourBytes?): TransportInfo {
        log.debug("getTransportInfo instance id: {}", p0)

        val controller = VlcJInit.getController()
        if (controller != null && !controller.isReleased()) {
            val state = controller.state.value
            val transportState = when (state.state) {
                PlayState.PLAY -> TransportState.PLAYING
                PlayState.PAUSE -> TransportState.PAUSED_PLAYBACK
                PlayState.BUFFERING -> TransportState.TRANSITIONING
                PlayState.ERROR -> TransportState.NO_MEDIA_PRESENT
            }

            return TransportInfo(
                transportState,
                TransportStatus.OK,
                "1" // Current speed
            )
        }
        return TransportInfo(TransportState.NO_MEDIA_PRESENT, TransportStatus.OK, "1")
    }

    override fun getPositionInfo(p0: UnsignedIntegerFourBytes?): PositionInfo {
        // 获取播放器控制器
        val controller = VlcJInit.getController()

        if (controller != null && !controller.isReleased()) {
            val state = controller.state.value
            val currentTimeMs = state.timestamp
            val durationMs = state.duration

            // 转换为秒
            val currentTimeSec = currentTimeMs / 1000.0
            val durationSec = if (durationMs > 0) durationMs / 1000.0 else 0.0

            // 格式化时间为 HH:mm:ss 格式
            val currentTimeFormatted = formatTime(currentTimeSec)
            val durationFormatted = formatTime(durationSec)

            return PositionInfo(
                1,                      // track (轨道号)
                durationFormatted,      // trackDuration (轨道时长)
                "", // trackMetaData (轨道元数据)
                state.mediaInfo?.url ?: "", // trackURI (轨道URI)
                currentTimeFormatted,   // relTime (相对时间)
                currentTimeFormatted,   // absTime (绝对时间)
                0,                      // relCount (相对计数)
                0                       // absCount (绝对计数)
            )
        }

        // 默认返回空的PositionInfo
        return PositionInfo(
            0,                      // track
            "00:00:00",            // trackDuration
            "",                     // trackMetaData
            "",                     // trackURI
            "00:00:00",            // relTime
            "00:00:00",            // absTime
            0,                      // relCount
            0                       // absCount
        )
    }

    override fun getDeviceCapabilities(p0: UnsignedIntegerFourBytes?): DeviceCapabilities {
        TODO()
    }

    override fun getTransportSettings(p0: UnsignedIntegerFourBytes?): TransportSettings {
        log.debug("getTransportSettings instance id: {}", p0)
        return TransportSettings()
    }

    override fun stop(p0: UnsignedIntegerFourBytes?) {
        log.debug("stop instance id: {}", p0)

        val controller = VlcJInit.getController()
        if (controller != null && !controller.isReleased()) {
            controller.stop()
        }
    }

    override fun play(p0: UnsignedIntegerFourBytes?, p1: String?) {
        log.debug("play instance id: {}", p0)

        val controller = VlcJInit.getController()
        if (controller != null && !controller.isReleased()) {
            controller.play()
        }
    }

    override fun pause(p0: UnsignedIntegerFourBytes?) {
        log.debug("pause instance id: {}", p0)

        val controller = VlcJInit.getController()
        if (controller != null && !controller.isReleased()) {
            controller.pause()
        }
    }

    override fun record(p0: UnsignedIntegerFourBytes?) {
        log.debug("record instance id: {}", p0)
    }

    override fun seek(p0: UnsignedIntegerFourBytes?, p1: String?, p2: String?) {
        log.debug("seek instance id: {} unit: {} target: {}", p0, p1, p2)
        try {
            var targetTimeMs: Long = 0

            // 解析不同格式的时间
            when (p1) {
                "REL_TIME", "ABS_TIME" -> {
                    // 解析 HH:mm:ss 格式的时间
                    targetTimeMs = parseTimeStringToMillis(p2 ?: "00:00:00")
                }
                else -> {
                    val timeInSeconds = p2?.toDoubleOrNull()
                    if (timeInSeconds != null) {
                        targetTimeMs = (timeInSeconds * 1000).toLong()
                    }
                }
            }

            if (targetTimeMs > 0) {
                val controller = VlcJInit.getController()
                if (controller != null && !controller.isReleased()) {
                    log.debug("Seeking to: $targetTimeMs ms")
                    controller.seekTo(targetTimeMs)

                    controller.doWithPlayState { state ->
                        state.update { it.copy(timestamp = targetTimeMs) }
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Seek operation failed", e)
        }
    }

    override fun next(p0: UnsignedIntegerFourBytes?) {
        log.debug("next instance id: {}", p0)
    }

    override fun previous(p0: UnsignedIntegerFourBytes?) {
        log.debug("previous instance id: {}", p0)
    }

    override fun setPlayMode(p0: UnsignedIntegerFourBytes?, p1: String?) {
        log.debug("setPlayMode instance id: {}", p0)
    }

    override fun setRecordQualityMode(p0: UnsignedIntegerFourBytes?, p1: String?) {
        log.debug("setRecordQualityMode instance id: {}", p0)
    }

    override fun getCurrentTransportActions(p0: UnsignedIntegerFourBytes?): Array<TransportAction> {
        log.debug("getCurrentTransportActions instance id: {}", p0)
        return arrayOf()
    }

    private fun parseTimeStringToMillis(timeString: String): Long {
        try {
            val parts = timeString.split(":")
            return when (parts.size) {
                3 -> {
                    // HH:mm:ss 格式
                    val hours = parts[0].toLongOrNull() ?: 0
                    val minutes = parts[1].toLongOrNull() ?: 0
                    val seconds = parts[2].toLongOrNull() ?: 0
                    (hours * 3600 + minutes * 60 + seconds) * 1000
                }
                2 -> {
                    // mm:ss 格式
                    val minutes = parts[0].toLongOrNull() ?: 0
                    val seconds = parts[1].toLongOrNull() ?: 0
                    (minutes * 60 + seconds) * 1000
                }
                else -> {
                    // 只有秒数
                    ((timeString.toDoubleOrNull() ?: (0.0 * 1000))).toLong()
                }
            }
        } catch (e: Exception) {
            log.error("Failed to parse time string: $timeString", e)
            return 0
        }
    }

    private fun formatTime(seconds: Double): String {
        if (seconds <= 0) return "00:00:00"

        val totalSeconds = seconds.toLong()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}