package upnp

import org.jupnp.DefaultUpnpServiceConfiguration
import org.jupnp.UpnpServiceImpl
import org.jupnp.model.meta.LocalDevice
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.registry.Registry


class TVMUpnpService: UpnpServiceImpl(DefaultUpnpServiceConfiguration()) {
    private var localDevice: LocalDevice? = null;

    override fun createRegistry(protocolFactory: ProtocolFactory?): Registry {
        val registry = super.createRegistry(protocolFactory)
        localDevice = TVMDevice()
        registry.addDevice(localDevice)
        return registry
    }

    fun sendAlive() {
        getProtocolFactory().createSendingNotificationAlive(localDevice).run()
    }
}