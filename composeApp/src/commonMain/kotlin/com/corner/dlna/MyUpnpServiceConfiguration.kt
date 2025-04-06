package com.corner.dlna

//import org.jupnp.DefaultUpnpServiceConfiguration
//import org.jupnp.model.Namespace
//import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl
//import org.jupnp.transport.spi.NetworkAddressFactory
//import org.jupnp.transport.spi.StreamClient
//import org.jupnp.transport.spi.StreamServer
//import transport.impl.jetty.JettyTransportConfiguration
//import java.util.concurrent.Executors
//
//
//class MyUpnpServiceConfiguration : DefaultUpnpServiceConfiguration() {
//    override fun createNamespace(): Namespace {
//        return Namespace("/upnp") // This will be the servlet context path
//    }
//
//    override fun createStreamClient(): StreamClient<*> {
//        val newCachedThreadPool = Executors.newCachedThreadPool()
//        return JettyTransportConfiguration.INSTANCE.createStreamClient(newCachedThreadPool,  StreamClientConfigurationImpl(newCachedThreadPool))
//    }
//
//    override fun createStreamServer(networkAddressFactory: NetworkAddressFactory): StreamServer<*> {
//        return JettyTransportConfiguration.INSTANCE.createStreamServer(networkAddressFactory.streamListenPort)
//    }
//}