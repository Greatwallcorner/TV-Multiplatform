package upnp

import com.corner.dlna.TVConnectionManagerService
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder
import org.jupnp.model.DefaultServiceManager
import org.jupnp.model.meta.*
import org.jupnp.model.types.UDADeviceType
import org.jupnp.model.types.UDN


class TVMDevice
    : LocalDevice(
    DeviceIdentity(
        UDN("d9fff39939cf4dde9c762ccd82629745")
    ),
    UDADeviceType("tvm"),
    DeviceDetails("TV Multiplatform"),
    createICons(),
    createService(),
) {

}

fun createICons(): Array<Icon> {
    val list = arrayListOf<Icon>()
    list.add(
        Icon(
            "image/png",
            72,
            68,
            24,
            "images/icon-s.png",
            TVMDevice::class.java.getResourceAsStream("/pic/TV-icon-s.png")
        )
    )
    list.add(
        Icon(
            "image/png",
            269,
            248,
            24,
            "images/icon-x.png",
            TVMDevice::class.java.getResourceAsStream("/pic/TV-icon-x.png")
        )
    )
    list.add(
        Icon(
            "image/png",
            17,
            40,
            24,
            "images/icon-xs.png",
            TVMDevice::class.java.getResourceAsStream("/pic/TV-icon-xs.png")
        )
    )
    return list.toTypedArray()
}

fun createService(): Array<LocalService<out Any>?>{
    val services = arrayOfNulls<LocalService<out Any>>(2)
    val service = AnnotationLocalServiceBinder().read(TVConnectionManagerService::class.java) as LocalService<TVConnectionManagerService>
    service.manager = object: DefaultServiceManager<TVConnectionManagerService>(service, TVConnectionManagerService::class.java){
        override fun getLockTimeoutMillis(): Int {
            return 1000
        }

        override fun createServiceInstance(): TVConnectionManagerService {
            return TVConnectionManagerService()
        }
    }
    services[0] = service

    val transport =
        AnnotationLocalServiceBinder().read(TvAvTransportService::class.java) as LocalService<TvAvTransportService>
    transport.manager = object:DefaultServiceManager<TvAvTransportService>(transport, TvAvTransportService::class.java){
        override fun createServiceInstance(): TvAvTransportService {
            return TvAvTransportService()
        }
    }
    services[1] = transport
    return services
}
