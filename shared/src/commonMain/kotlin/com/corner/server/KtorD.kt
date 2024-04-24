package com.corner.server

import com.corner.server.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.tomcat.*
import org.slf4j.event.Level

object KtorD {

    var port: Int = -1;

    var server:ApplicationEngine? = null

    /**
     * https://ktor.io/docs/configuration-file.html#predefined-properties
     */
    suspend fun init() {
        port = 9978
        do {
            try {
                server = embeddedServer(Tomcat, port = port, host = "0.0.0.0", module = Application::module, configure = {
                    connectionGroupSize = 20
                    workerGroupSize = 8
                    callGroupSize = 15
                    shutdownGracePeriod = 2000
                    shutdownTimeout = 3000
//                    responseWriteTimeoutSeconds = -1
//                    requestReadTimeoutSeconds = 0
                })
                    .start(wait = false)
                break
            } catch (e: Exception) {
                ++port
                server?.stop()
            }
        }while(port < 9999)
        println(server!!.resolvedConnectors().first().port)
    }

}

private fun Application.module() {
    install(CallLogging){
        level = Level.DEBUG
    }
//    install(ContentNegotiation){
//        json(Json {
//            isLenient = true
//            prettyPrint = true
//        })
//    }
    configureRouting()
}
