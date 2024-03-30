package com.corner.dlna

import org.jupnp.binding.annotations.UpnpAction
import org.jupnp.binding.annotations.UpnpInputArgument
import org.jupnp.binding.annotations.UpnpService
import org.jupnp.binding.annotations.UpnpServiceId
import org.jupnp.binding.annotations.UpnpServiceType

@UpnpService(serviceId = UpnpServiceId("TV"), serviceType = UpnpServiceType("TV", version = 1))
class Service{

    @UpnpAction
    fun push(@UpnpInputArgument(name = "url") url:String){
        println("push:$url")
    }

}