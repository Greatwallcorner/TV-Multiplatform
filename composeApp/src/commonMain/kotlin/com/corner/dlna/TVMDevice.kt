package upnp

import com.corner.dlna.TVConnectionManagerService
import com.corner.dlna.TvMAudioRenderingControlService
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder
import org.jupnp.model.DefaultServiceManager
import org.jupnp.model.meta.*
import org.jupnp.model.types.UDADeviceType
import org.jupnp.model.types.UDN
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl
import tv_multiplatform.composeapp.generated.resources.Res


class TVMDevice
    : LocalDevice(
    DeviceIdentity(
        UDN("d9fff39939cf4dde9c762ccd82629745")
    ),
    UDADeviceType("tvm"),
    DeviceDetails("TV Multiplatform"),
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
            72,
            68,
            24,
            "images/icon-s.png",
            runBlocking { Res.readBytes("drawable/TV-icon-s.png") }
        )
    )
    list.add(
        Icon(
            "image/png",
            269,
            248,
            24,
            "images/icon-x.png",
            runBlocking { Res.readBytes("drawable/TV-icon-x.png") }
        )
    )
    list.add(
        Icon(
            "image/png",
            17,
            40,
            24,
            "images/icon-xs.png",
            runBlocking { Res.readBytes("drawable/TV-icon-xs.png") }
        )
    )
    return list.toTypedArray()
}

@Suppress("UNCHECKED_CAST")
fun createService(): Array<LocalService<out Any>?>{
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
