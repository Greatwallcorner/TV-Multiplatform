package com.corner.server

import cn.hutool.core.codec.Base64
import com.corner.server.plugins.configureRouting
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.netty.handler.codec.http.HttpServerCodec
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("KtorD")

object KtorD {

    var ports: Int = -1;

    var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    /**
     * https://ktor.io/docs/configuration-file.html#predefined-properties
     */
    suspend fun init() {
        log.info("KtorD Init")
        ports = 9978
        do {
            try {
                server = embeddedServer(Netty, configure = {
                    this.connectors.add(EngineConnectorBuilder().apply {
                        port = ports
                    }
                    )
                    httpServerCodec = {
                        HttpServerCodec(
                            maxInitialLineLength * 10,
                            maxHeaderSize,
                            maxChunkSize
                        )
                    }
                }, module = Application::module)
                    .start(wait = false)
                break
            } catch (e: Exception) {
                log.error("start server e:", e)
                ++ports
                server?.stop()
            }
        } while (ports < 9999)
        log.info("KtorD init end port:{}", server!!.application.engine.resolvedConnectors().first().port)
    }

    fun stop() {
        log.info("KtorD stop")
        server?.stop()
    }
}

private fun Application.module() {
//    install(CallLogging){
//        level = Level.DEBUG
//    }
//    install(ContentNegotiation){
//        json(Json {
//            isLenient = true
//            prettyPrint = true
//        })
//    }
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("MyCustomHeader")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    configureRouting()
}
