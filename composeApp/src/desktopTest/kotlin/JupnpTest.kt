import org.junit.Test
import org.jupnp.DefaultUpnpServiceConfiguration
import org.jupnp.UpnpService
import org.jupnp.UpnpServiceImpl
import org.jupnp.model.message.header.STAllHeader
import org.jupnp.model.meta.LocalDevice
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.registry.Registry
import org.jupnp.registry.RegistryListener

class JupnpTest {
    @Test
    @Throws(InterruptedException::class)
    fun test() {
        println(System.getProperty("java.version"))
        // UPnP discovery is asynchronous, we need a callback

        val listener: RegistryListener = object : RegistryListener {
            override fun remoteDeviceDiscoveryStarted(registry: Registry, device: RemoteDevice) {
                println("Discovery started: " + device.displayString)
            }

            override fun remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice, e: Exception) {
                println("Discovery failed: " + device.displayString + " => " + e)
            }

            override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
                println("Remote device available: " + device.displayString)
            }

            override fun remoteDeviceUpdated(registry: Registry, device: RemoteDevice) {
                println("Remote device updated: " + device.displayString)
            }

            override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
                println("Remote device removed: " + device.displayString)
            }

            override fun localDeviceAdded(registry: Registry, device: LocalDevice) {
                println("Local device added: " + device.displayString)
            }

            override fun localDeviceRemoved(registry: Registry, device: LocalDevice) {
                println("Local device removed: " + device.displayString)
            }

            override fun beforeShutdown(registry: Registry) {
                println(
                    "Before shutdown, the registry has devices: "
                            + registry.devices.size
                )
            }

            override fun afterShutdown() {
                println("Shutdown of registry complete!")
            }
        }

        // This will create necessary network resources for UPnP right away
        println("Starting jUPnP...")
        val upnpService: UpnpService = UpnpServiceImpl(DefaultUpnpServiceConfiguration())
        upnpService.startup()
        upnpService.registry.addListener(listener)

        // Send a search message to all devices and services, they should respond soo
        println("Sending SEARCH message to all devices...")
        upnpService.controlPoint.search(STAllHeader())

        // Let's wait 10 seconds for them to respond
        println("Waiting 10 seconds before shutting down...")
        Thread.sleep(10000)

        // Release all resources and advertise BYEBYE to other UPnP devices
        println("Stopping jUPnP...")
        upnpService.shutdown()
    }
}
