package com.corner.catvodcore.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*

/**
@author heatdesert
@date 2023-12-17 17:12
@description
 */
class KtorClient {
    companion object{
        val client = HttpClient(CIO)
    }
}