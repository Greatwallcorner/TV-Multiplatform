package com.corner.dlna

import org.jupnp.binding.annotations.AnnotationLocalServiceBinder
import org.jupnp.model.DefaultServiceManager
import org.jupnp.model.meta.*
import org.jupnp.model.types.DeviceType
import org.jupnp.model.types.UDN

fun createDevice(): LocalDevice{
    val deviceIdentity = DeviceIdentity(UDN.uniqueSystemIdentifier("TV-Multiplatform"))
    val deviceType = DeviceType("TV","1" )
    val deviceDetails = DeviceDetails("TV-Cast", ManufacturerDetails("heatdesert"),
        ModelDetails( "TV-Multiplatform", "TV dlna cast")
    )
    val serviceBound = AnnotationLocalServiceBinder().read(Service::class.java) as LocalService<Service>
    var s:Class<Service> = Service::class.java
    serviceBound.manager = object :DefaultServiceManager<Service>(serviceBound){
        override fun createServiceInstance(): Service {
            return Service()
        }
    }
    return LocalDevice(deviceIdentity, deviceType, deviceDetails, serviceBound)
}