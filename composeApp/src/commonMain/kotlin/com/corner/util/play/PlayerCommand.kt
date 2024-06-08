package com.corner.util.play

import androidx.compose.ui.graphics.Path
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.v
import com.corner.catvodcore.util.Paths

interface PlayerCommand{
//    fun currentProcess(): String

    fun title(title:String):String

    fun start(time:String):String

//    fun headers(headers:Map<String, String>?)

    fun subtitle(s:String):String

//    fun add(s:String):String

    fun url(s:String):String{
        return "\"$s\""
    }

    fun getProcessBuilder(result: Result, title: String, playerPath: String):ProcessBuilder{
        return ProcessBuilder(playerPath, url(result.url.v())).redirectOutput(Paths.playerLog())
    }

    fun getProcessBuilder(url: String, title: String, playerPath: String):ProcessBuilder{
        return ProcessBuilder(playerPath, url(url)).redirectOutput(Paths.playerLog())
    }

    fun buildHeaderStr(headers:Map<String, String>?):String{
        if(headers.isNullOrEmpty()) return ""
        val header = ""
        for (entry in headers.entries) {
            if(entry.key.isNotEmpty() && entry.value.isNotEmpty()){
                header.plus("${entry.key}:${entry.value}\r\n")
            }
        }
        return header
    }
}

object Default: PlayerCommand {
    override fun title(title: String): String {
        TODO("Not yet implemented")
    }

    override fun start(time: String): String {
        TODO("Not yet implemented")
    }

    override fun subtitle(s: String): String {
        TODO("Not yet implemented")
    }
}