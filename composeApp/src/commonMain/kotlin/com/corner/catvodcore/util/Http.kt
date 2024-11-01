package com.corner.catvodcore.util

import com.github.catvod.bean.Doh
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.Credentials.basic
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.http.HttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class Http {
    companion object {
        private var doh: DnsOverHttps? = null
        private var client: OkHttpClient? = null
        private var selector: ProxySelect? = null
        private val defaultHeaders: Headers = Headers.Builder().build()

        //获取这个SSLSocketFactory
        fun getSSLSocketFactory(): SSLSocketFactory {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, getTrustManager(), SecureRandom())
                return sslContext.socketFactory
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        //获取TrustManager
        private fun getTrustManager(): Array<TrustManager> {
            return arrayOf(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
            )
        }

        //获取HostnameVerifier
        fun getHostnameVerifier(): HostnameVerifier {
            return HostnameVerifier { s: String?, sslSession: SSLSession? -> true }
        }

        fun getX509TrustManager(): X509TrustManager? {
            var trustManager: X509TrustManager? = null
            try {
                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(null as KeyStore?)
                val trustManagers = trustManagerFactory.trustManagers
                check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) { "Unexpected default trust managers:" + trustManagers.contentToString() }
                trustManager = trustManagers[0] as X509TrustManager
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return trustManager
        }
        private val builder: OkHttpClient.Builder
            get(){
                val dispatcher = Dispatcher()
                return OkHttpClient().newBuilder().connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .sslSocketFactory(getSSLSocketFactory(), getX509TrustManager()!!)
                    .hostnameVerifier((getHostnameVerifier()))
//                    .callTimeout(Duration.of(3, ChronoUnit.SECONDS))
                    .dispatcher(dispatcher)
                    .dns(dns())
            }


        fun dns(): Dns {
            return if (doh == null) Dns.SYSTEM else doh!!
        }

        fun setDoh(doh: Doh) {
            val dnsClient =
                OkHttpClient().newBuilder().cache(Cache(Paths.doh(), 8000))
                    .callTimeout(Duration.of(5, ChronoUnit.SECONDS))
                    .build()
            Companion.doh =
                DnsOverHttps.Builder().client(dnsClient).bootstrapDnsHosts(doh.hosts).url(doh.url.toHttpUrl()).build()
            client?.dispatcher?.executorService?.shutdownNow()
            client = builder.build()
        }

        fun setProxy(proxy: String) {
            ProxySelect.setDefault(getSelector(proxy))
        }

        fun setProxyHosts(hosts: List<String>?) {
            if (hosts != null) {
                selector?.setProxyHosts(hosts)
            }
        }

        private fun getSelector(proxy: String): ProxySelect {
            if (selector == null) {
                selector = ProxySelect()
            }
            selector!!.setProxy(proxy)
            return selector!!
        }

        fun client(connectTimeout: Int): HttpClient.Builder {
            return HttpClient.newBuilder().connectTimeout(Duration.of(connectTimeout.toLong(), ChronoUnit.MINUTES))
        }

        fun client(): OkHttpClient {
            return if (client == null) builder.build().also {
                client = it
            } else client!!
        }

        @JvmOverloads
        fun Get(url: String, params: Map<String, String?>? = null, headers: Headers? = null): Call {
            val builder: HttpUrl.Builder = url.toHttpUrlOrNull()!!.newBuilder()
            params?.forEach { (name: String, value: String?) ->
                builder.addQueryParameter(
                    name,
                    value
                )
            }
            val httpUrl: HttpUrl = builder.build()
            val request: Request.Builder = Request.Builder()
                .url(httpUrl.toUrl())
                .headers(headers ?: defaultHeaders)
            checkBaseAuth(httpUrl, request)
            return client().newCall(request.build())
        }

        fun Post(url: String, params: Map<String, String>?, headers: Headers?): Call {
            val builder: HttpUrl.Builder = url.toHttpUrlOrNull()!!.newBuilder()
            val httpUrl: HttpUrl = builder.build()
            val request: Request.Builder = Request.Builder()
                .url(httpUrl.toUrl())
                .post(Json.encodeToString(params).toByteArray().toRequestBody())
                .headers(headers ?: defaultHeaders)
            checkBaseAuth(httpUrl, request)
            return client!!.newCall(request.build())
        }

        private fun checkBaseAuth(url: HttpUrl, request: Request.Builder) {
            if (url.toUrl().userInfo != null) {
                request.header(org.apache.http.HttpHeaders.AUTHORIZATION, basic(url.username, url.password))
            }
        }

        private fun buildUrl(url: String, params:Map<String, String>): HttpUrl {
            val builder: HttpUrl.Builder = Objects.requireNonNull(url.toHttpUrlOrNull())!!.newBuilder()
            for ((key, value) in params.entries)
                builder.addQueryParameter(key, value)
            return builder.build()
        }

        fun newCall(url: String, headers: Headers, params: Map<String, String>): Call {
            return client().newCall(Request.Builder().url(buildUrl(url, params)).headers(headers).build())
        }
        fun newCall(url: String, headers: Headers, body: RequestBody): Call {
            return client().newCall(Request.Builder().url(url.toHttpUrl()).headers(headers).post(body).build())
        }

        fun newCall(api: String, headers: Headers): Call {
            return client().newCall(
                Request.Builder().url(api.toHttpUrl()).headers(headers)
                    .build()
            )
        }

        fun toBody(params: Map<String, String>): FormBody {
            val builder = FormBody.Builder()
            for (param in params) {
                builder.add(param.key, param.value)
            }
            return builder.build()
        }

    }

}
