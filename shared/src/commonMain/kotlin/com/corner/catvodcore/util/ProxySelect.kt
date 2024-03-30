package com.corner.catvodcore.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.net.*

class ProxySelect : ProxySelector() {

    private var proxy: Proxy? = null

    private var proxyHosts: List<String>? = null;

    companion object {
        fun setDefault(proxySelector: ProxySelector) {
            ProxySelector.setDefault(proxySelector)
        }
    }

    override fun select(uri: URI?): MutableList<Proxy> {
        val httpUrl = uri?.toHttpUrlOrNull()
        if (httpUrl == null || proxy == null || proxyHosts.isNullOrEmpty() || InetAddress.getByName(httpUrl.host).isLoopbackAddress) {
            return mutableListOf(Proxy.NO_PROXY)
        }
        proxyHosts?.forEach { host -> if (host.contains(host)) return mutableListOf(proxy!!) }
        return mutableListOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
    }

    fun setProxyHosts(hosts: List<String>) {
        proxyHosts = hosts;
    }

    fun setProxy(proxy: String) {
        val httpUrl = proxy.toHttpUrlOrNull()
        if(httpUrl?.username != null){
            Authenticator.setDefault(object :Authenticator(){
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(httpUrl.username, httpUrl.password.toCharArray())
                }
            })
        }
    }
}