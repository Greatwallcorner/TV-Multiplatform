package com.corner.server

import com.corner.server.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("KtorD")
object KtorD {

    var port: Int = -1;

    var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    /**
     * https://ktor.io/docs/configuration-file.html#predefined-properties
     */
    suspend fun init() {
        log.info("KtorD init start")
        port = 9978
        do {
            try {
               server = embeddedServer(Netty, port = port, module = Application::module)
                    .start(wait = false)
                break
            } catch (e: Exception) {
                log.error("start server e:", e)
                ++port
                server?.stop()
            }
        }while(port < 9999)
        log.info("KtorD init end port:{}", server!!.application.engine.resolvedConnectors().first().port)
    }

    fun stop(){
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
    configureRouting()
}
