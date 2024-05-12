package com.corner.dlna

import org.jupnp.UpnpServiceImpl
import org.jupnp.model.message.header.STAllHeader
import org.jupnp.model.meta.LocalDevice
import org.jupnp.model.meta.RemoteDevice
import org.jupnp.registry.Registry
import org.jupnp.registry.RegistryListener


object Registry {
    val registry = object : RegistryListener {
        override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice?) {
            println(
                "Discovery started: " + device?.getDisplayString()
            );
        }

        override fun remoteDeviceDiscoveryFailed(registry: Registry?, device: RemoteDevice?, ex: Exception?) {
            println(
                "Discovery failed: " + device?.getDisplayString() + " => " + ex
            );
        }

        override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice?) {
            println(
                "Remote device available: " + device?.getDisplayString()
            );
        }

        override fun remoteDeviceUpdated(registry: Registry?, device: RemoteDevice?) {
            println(
                "Remote device updated: " + device?.getDisplayString()
            );
        }

        override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice?) {
            println(
                "Remote device removed: " + device?.getDisplayString()
            );
        }

        override fun localDeviceAdded(registry: Registry?, device: LocalDevice?) {
            println(
                "Local device added: " + device?.getDisplayString()
            );
        }

        override fun localDeviceRemoved(registry: Registry?, device: LocalDevice?) {
            println(
                "Local device removed: " + device?.getDisplayString()
            );
        }

        override fun beforeShutdown(registry: Registry?) {
            println(
                "Before shutdown, the registry has devices: "
                        + registry?.getDevices()?.size
            );
        }

        override fun afterShutdown() {
            println("Shutdown of registry complete!");
        }
    }

    fun init(){
        val upnpServiceImpl = UpnpServiceImpl()
        upnpServiceImpl.registry.addListener(registry)
        upnpServiceImpl.controlPoint.search(STAllHeader())


        // Let's wait 10 seconds for them to respond
        println("Waiting 10 seconds before shutting down...")
        Thread.sleep(10000)


        // Release all resources and advertise BYEBYE to other UPnP devices
        println("Stopping Cling...")
        upnpServiceImpl.shutdown()

//        upnpServiceImpl.startup()

    }
}