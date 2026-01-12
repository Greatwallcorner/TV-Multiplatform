package com.corner.database

import com.corner.database.entity.Config
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking


fun create() {

}

private var config: Config? = null

fun get(): Config? {
    return config
}

fun find(url: String, type: Long): Config {
    val configFlow = Db.Config.find(url, type)
    var config: Config?
    runBlocking {
        config = configFlow.firstOrNull()
        if (config == null) {
            Db.Config.save(Config(type = type, url = url))
            config = Db.Config.find(url, type).firstOrNull()
        }
    }
    return config!!
}