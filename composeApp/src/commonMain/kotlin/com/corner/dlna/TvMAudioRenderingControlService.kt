package com.corner.dlna

import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.model.types.UnsignedIntegerTwoBytes
import org.jupnp.support.model.Channel
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl
import org.slf4j.LoggerFactory


class TvMAudioRenderingControlService:AbstractAudioRenderingControl() {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun getCurrentInstanceIds(): Array<UnsignedIntegerFourBytes> {
        log.debug("getCurrentInstanceIds not yet implemented")
        return emptyArray()
    }

    override fun getMute(instanceId: UnsignedIntegerFourBytes?, channelName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun setMute(instanceId: UnsignedIntegerFourBytes?, channelName: String?, desiredMute: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getVolume(instanceId: UnsignedIntegerFourBytes?, channelName: String?): UnsignedIntegerTwoBytes {
        TODO("Not yet implemented")
    }

    override fun setVolume(
        instanceId: UnsignedIntegerFourBytes?,
        channelName: String?,
        desiredVolume: UnsignedIntegerTwoBytes?
    ) {
        log.info("setVolume instanceId:{} channelName:{} desiredVolume:{}", instanceId, channelName, desiredVolume)
    }

    override fun getCurrentChannels(): Array<Channel> {
        TODO("Not yet implemented")
    }
}