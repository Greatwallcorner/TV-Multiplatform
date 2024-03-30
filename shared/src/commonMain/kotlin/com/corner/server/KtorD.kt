package com.corner.server

import com.corner.server.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

object KtorD {

    var port: Int = -1;

    var server:ApplicationEngine? = null

    suspend fun init() {
        port = 9978
        do {
            try {
                server = embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
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
//    install(ContentNegotiation){
//        json(Json {
//            isLenient = true
//            prettyPrint = true
//        })
//    }
    configureRouting()
}
