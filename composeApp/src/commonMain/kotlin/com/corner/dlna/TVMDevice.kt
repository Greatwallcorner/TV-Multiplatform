package com.corner.dlna

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder
import org.jupnp.model.DefaultServiceManager
import org.jupnp.model.meta.*
import org.jupnp.model.types.UDADeviceType
import org.jupnp.model.types.UDN
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl
import lumentv_compose.composeapp.generated.resources.Res
import java.util.UUID


class TVMDevice
    : LocalDevice(
    DeviceIdentity(
        UDN(UUID.randomUUID().toString())
    ),
    UDADeviceType("tvm"),
    DeviceDetails("LumenTV-Compose"),
    createIcons(),
    createService(),
) {

}

@OptIn(ExperimentalResourceApi::class)
fun createIcons(): Array<Icon> {
    val list = arrayListOf<Icon>()
    list.add(
        Icon(
            "image/png",
            48,
            48,
            24,
            "images/icon-xs.png",
            runBlocking { Res.readBytes("drawable/LumenTV-icon-48.png") }
        )
    )
    list.add(
        Icon(
            "image/png",
            128,
            128,
            24,
            "images/icon-s.png",
            runBlocking { Res.readBytes("drawable/LumenTV-icon-128.png") }
        )
    )
    list.add(
        Icon(
            "image/png",
            256,
            256,
            24,
            "images/icon-x.png",
            runBlocking { Res.readBytes("drawable/LumenTV-icon-256.png") }
        )
    )
    return list.toTypedArray()
}

@Suppress("UNCHECKED_CAST")
fun createService(): Array<LocalService<*>>{
    val service = AnnotationLocalServiceBinder().read(TVConnectionManagerService::class.java) as LocalService<TVConnectionManagerService>
    service.manager = object : DefaultServiceManager<TVConnectionManagerService>(service, null){
        override fun getLockTimeoutMillis(): Int {
            return LOCK_TIMEOUT
        }

        override fun createServiceInstance(): TVConnectionManagerService {
            return TVConnectionManagerService()
        }
    }
//    services[0] = service

    val transport =
        AnnotationLocalServiceBinder().read(TvAvTransportService::class.java) as LocalService<TvAvTransportService>

    transport.manager = object : DefaultServiceManager<TvAvTransportService>(transport, null){

        override fun getLockTimeoutMillis(): Int {
            return LOCK_TIMEOUT
        }

        override fun createServiceInstance(): TvAvTransportService {
            return TvAvTransportService()
        }
    }

    return arrayOf(service, transport, createRenderingControl())
}

private const val LOCK_TIMEOUT = 5000

@Suppress("UNCHECKED_CAST")
private fun createRenderingControl(): LocalService<AbstractAudioRenderingControl> {
    val renderingControlService: LocalService<AbstractAudioRenderingControl> = AnnotationLocalServiceBinder().read(AbstractAudioRenderingControl::class.java) as LocalService<AbstractAudioRenderingControl>
    renderingControlService.setManager(object :
        DefaultServiceManager<AbstractAudioRenderingControl>(renderingControlService, null) {
        override fun getLockTimeoutMillis(): Int {
            return LOCK_TIMEOUT
        }

        override fun createServiceInstance(): AbstractAudioRenderingControl {
            return TvMAudioRenderingControlService()
        }
    })
    return renderingControlService
}