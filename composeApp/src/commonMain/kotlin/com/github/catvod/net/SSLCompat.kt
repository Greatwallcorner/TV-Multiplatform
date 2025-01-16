package com.github.catvod.net

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*


class SSLCompat : SSLSocketFactory() {
    private var factory: SSLSocketFactory? = null
    private var cipherSuites: Array<String>? = null
    private var protocols: Array<String>? = null

    override fun getDefaultCipherSuites(): Array<String> {
        return cipherSuites!!
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return cipherSuites!!
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val ssl = factory!!.createSocket(s, host, port, autoClose)
        if (ssl is SSLSocket) upgradeTLS(ssl)
        return ssl
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket {
        val ssl = factory!!.createSocket(host, port)
        if (ssl is SSLSocket) upgradeTLS(ssl)
        return ssl
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        val ssl = factory!!.createSocket(host, port, localHost, localPort)
        if (ssl is SSLSocket) upgradeTLS(ssl)
        return ssl
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        val ssl = factory!!.createSocket(host, port)
        if (ssl is SSLSocket) upgradeTLS(ssl)
        return ssl
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        val ssl = factory!!.createSocket(address, port, localAddress, localPort)
        if (ssl is SSLSocket) upgradeTLS(ssl)
        return ssl
    }

    private fun upgradeTLS(ssl: SSLSocket) {
        if (protocols != null) ssl.enabledProtocols = protocols
        if (cipherSuites != null) ssl.enabledCipherSuites = cipherSuites
    }

    init {
        try {
            val list: MutableList<String> = LinkedList()
            val socket = getDefault().createSocket() as SSLSocket
            for (protocol in socket.supportedProtocols) if (!protocol.uppercase(Locale.getDefault())
                    .contains("SSL")
            ) list.add(protocol)
            protocols = list.toTypedArray<String>()
            val allowedCiphers: List<String> = mutableListOf(
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECHDE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
            )
            val availableCiphers = Arrays.asList(*socket.supportedCipherSuites)
            val preferredCiphers = HashSet(allowedCiphers)
            preferredCiphers.retainAll(availableCiphers)
            preferredCiphers.addAll(HashSet(Arrays.asList(*socket.enabledCipherSuites)))
            cipherSuites = preferredCiphers.toTypedArray<String>()
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf(TM), null)
            HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory.also { factory = it })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmField
        val TM: X509TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }
}
