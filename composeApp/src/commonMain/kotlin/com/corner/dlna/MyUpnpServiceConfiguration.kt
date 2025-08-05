package com.corner.dlna
//
//import org.jupnp.DefaultUpnpServiceConfiguration
//import org.jupnp.model.Namespace
//import org.jupnp.transport.impl.jetty.JettyTransportConfiguration
//import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl
//import org.jupnp.transport.spi.NetworkAddressFactory
//import org.jupnp.transport.spi.StreamClient
//import org.jupnp.transport.spi.StreamServer
//import java.util.concurrent.Executors
//import java.util.concurrent.ScheduledExecutorService
//import java.util.concurrent.ThreadPoolExecutor
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.LinkedBlockingQueue
//
//class MyUpnpServiceConfiguration : DefaultUpnpServiceConfiguration() {
//    override fun createNamespace(): Namespace {
//        return Namespace("/upnp")
//    }
//
//    // 使用固定大小的线程池替代cachedThreadPool
//    override fun createDefaultExecutorService(): ScheduledExecutorService {
//        return Executors.newScheduledThreadPool(4) // 限制为4个核心线程
//    }
//
//    override fun createStreamClient(): StreamClient<*> {
//        // 使用固定大小的线程池
//        val executor = ThreadPoolExecutor(
//            2, // core pool size
//            4, // max pool size
//            60L, TimeUnit.SECONDS,
//            LinkedBlockingQueue(100)
//        )
//        return JettyTransportConfiguration.INSTANCE.createStreamClient(
//            executor,
//            StreamClientConfigurationImpl(executor)
//        )
//    }
//
//    override fun createStreamServer(networkAddressFactory: NetworkAddressFactory): StreamServer<*> {
//        return JettyTransportConfiguration.INSTANCE.createStreamServer(networkAddressFactory.streamListenPort)
//    }
//}
