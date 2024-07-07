package com.corner.catvodcore.util

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

class KtorClient {
    companion object{
        val client = HttpClient(OkHttp)
    }
}