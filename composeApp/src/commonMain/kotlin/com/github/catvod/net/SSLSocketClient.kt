package com.github.catvod.net

import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object SSLSocketClient {
    @JvmStatic
    val sSLSocketFactory: SSLSocketFactory
        //获取这个SSLSocketFactory
        get() {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustManager, SecureRandom())
                return sslContext.socketFactory
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

    private val trustManager: Array<TrustManager>
        //获取TrustManager
        get() = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
            }

            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
        )

    @JvmStatic
    val hostnameVerifier: HostnameVerifier
        //获取HostnameVerifier
        get() = HostnameVerifier { _: String?, _: SSLSession? -> true }

    @JvmStatic
    val x509TrustManager: X509TrustManager
        get() {
            var trustManager: X509TrustManager? = null
            try {
                val trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(null as KeyStore?)
                val trustManagers = trustManagerFactory.trustManagers
                check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) { "Unexpected default trust managers:" + trustManagers.contentToString() }
                trustManager = trustManagers[0] as X509TrustManager
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return trustManager!!
        }
}
