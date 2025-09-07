package com.corner.dlna

import com.corner.catvodcore.viewmodel.GlobalAppState
import com.corner.util.thisLogger
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
        return MediaInfo()
    }

    override fun getTransportInfo(p0: UnsignedIntegerFourBytes?): TransportInfo {
        log.debug("getTransportInfo instance id: {}", p0)
        return TransportInfo()
    }

    override fun getPositionInfo(p0: UnsignedIntegerFourBytes?): PositionInfo {
        log.debug("getPositionInfo instance id: {}", p0)
        return PositionInfo()
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
    }

    override fun play(p0: UnsignedIntegerFourBytes?, p1: String?) {
        log.debug("play instance id: {}", p0)
    }

    override fun pause(p0: UnsignedIntegerFourBytes?) {
        log.debug("pause instance id: {}", p0)
    }

    override fun record(p0: UnsignedIntegerFourBytes?) {
        log.debug("record instance id: {}", p0)
    }

    override fun seek(p0: UnsignedIntegerFourBytes?, p1: String?, p2: String?) {
        log.debug("seek instance id: {}", p0)
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
}