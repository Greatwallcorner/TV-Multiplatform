package com.corner.database

import com.corner.database.entity.Config
import kotlinx.coroutines.runBlocking


fun Config.create(){

}
private var config:Config? = null

fun Config.get():Config?  {
    return config
}

fun Config.find(url:String, type:Long):Config{
    val configFlow = Db.Config.find(url, type)
    var config:Config? = null
    runBlocking {
        configFlow.collect { it ->
            if (it == null) {
                Db.Config.save(Config(type = type, url = url))
                Db.Config.find(url, type).collect { cfg ->
                    config = cfg
                }
            }
        }
    }

//    if (config == null){
//        Db.Config.save(Config(type = type, url = url))
//        config = Db.Config.find(url, type)
//    }
    return config!!
}