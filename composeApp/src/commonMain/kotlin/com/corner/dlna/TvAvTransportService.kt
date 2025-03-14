package upnp

import com.corner.util.play.Play
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.support.avtransport.AbstractAVTransportService
import org.jupnp.support.model.*
import org.slf4j.LoggerFactory


class TvAvTransportService: AbstractAVTransportService() {
    private val log = LoggerFactory.getLogger(this::class.java)
    override fun getCurrentInstanceIds(): Array<UnsignedIntegerFourBytes> {
        return arrayOf(UnsignedIntegerFourBytes(0x01))
    }

    override fun setAVTransportURI(instanceId: UnsignedIntegerFourBytes, currentURI: String, currentURIMetaData: String) {
        Play.start(currentURI, "test")
        log.info("setAvTransportURI instance id: $instanceId currentURI: $currentURI currentURIMetaData: $currentURIMetaData")
    }

    override fun setNextAVTransportURI(p0: UnsignedIntegerFourBytes?, p1: String?, p2: String?) {
        TODO("Not yet implemented")
    }

    override fun getMediaInfo(p0: UnsignedIntegerFourBytes?): MediaInfo {
        TODO("Not yet implemented")
    }

    override fun getTransportInfo(p0: UnsignedIntegerFourBytes?): TransportInfo {
        TODO("Not yet implemented")
    }

    override fun getPositionInfo(p0: UnsignedIntegerFourBytes?): PositionInfo {
        TODO("Not yet implemented")
    }

    override fun getDeviceCapabilities(p0: UnsignedIntegerFourBytes?): DeviceCapabilities {
        TODO("Not yet implemented")
    }

    override fun getTransportSettings(p0: UnsignedIntegerFourBytes?): TransportSettings {
        TODO("Not yet implemented")
    }

    override fun stop(p0: UnsignedIntegerFourBytes?) {
        TODO("Not yet implemented")
    }

    override fun play(p0: UnsignedIntegerFourBytes?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun pause(p0: UnsignedIntegerFourBytes?) {
        TODO("Not yet implemented")
    }

    override fun record(p0: UnsignedIntegerFourBytes?) {
        TODO("Not yet implemented")
    }

    override fun seek(p0: UnsignedIntegerFourBytes?, p1: String?, p2: String?) {
        TODO("Not yet implemented")
    }

    override fun next(p0: UnsignedIntegerFourBytes?) {
        TODO("Not yet implemented")
    }

    override fun previous(p0: UnsignedIntegerFourBytes?) {
        TODO("Not yet implemented")
    }

    override fun setPlayMode(p0: UnsignedIntegerFourBytes?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun setRecordQualityMode(p0: UnsignedIntegerFourBytes?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun getCurrentTransportActions(p0: UnsignedIntegerFourBytes?): Array<TransportAction> {
        TODO("Not yet implemented")
    }
}