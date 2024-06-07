package com.corner.server

import com.corner.server.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("KtorD")
object KtorD {

    var port: Int = -1;

    var server:ApplicationEngine? = null

    /**
     * https://ktor.io/docs/configuration-file.html#predefined-properties
     */
    suspend fun init() {
        log.info("KtorD init start")
        port = 9978
        do {
            try {
                server = embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module, configure = {
                    connectionGroupSize = 20
                    workerGroupSize = 8
                    callGroupSize = 15
                    shutdownGracePeriod = 2000
                    shutdownTimeout = 3000
                    responseWriteTimeoutSeconds = -1
                    requestReadTimeoutSeconds = 0
                })
                    .start(wait = false)
                break
            } catch (e: Exception) {
                log.error("start server e:", e)
                ++port
                server?.stop()
            }
        }while(port < 9999)
        log.info("KtorD init end port:{}", server!!.resolvedConnectors().first().port)
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
