package com.corner.database


fun Config.create(){

}
private var config:Config? = null

fun Config.get():Config?  {
    return config
}

fun Config.find(url:String, type:Long):Config{
    var config = Db.Config.find(url, type)
    if (config == null){
        Db.Config.save(type = type, url = url)
        config = Db.Config.find(url, type)
    }
    return config!!
}