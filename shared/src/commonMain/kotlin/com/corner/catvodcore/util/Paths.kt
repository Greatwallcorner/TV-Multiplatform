package com.corner.catvodcore.util

import java.io.File
import java.nio.file.Path

object Paths {
    private val runPath = System.getProperty("user.dir")
    private val classPath = System.getProperty("java.class.path")

    private fun File.check():File{
        if (!exists()) {
            mkdirs()
        }
        return this
    }

    fun root():String{
        return runPath + "/data"
    }
    fun doh(): File {
        return File(cache( "doh")).check()
    }

    private fun cache(path: String): String {
        return root() +File.separator+ "cache" + File.separator + path
    }

    fun db():String{
        val path = File(root() + File.separator + "db").check().path.plus( File.separator + "db.db")
        return "jdbc:sqlite:${path}"
    }

    fun local(jar: String): File {
        val file = File(jar.replace("file:/", "").replace("file:\\", ""))
        return if(file.exists()) file else File(jar)
    }

    fun jar(): File {
        return File(cache("jar")).check()
    }

    fun jar(fileName:String):File{
        return File(jar(), Utils.md5(fileName) + ".jar")
    }

    fun write(path:File, bytes: ByteArray?):File{
        if(bytes == null || bytes.isEmpty()){
            return path
        }else{
            path.writeBytes(bytes)
        }

        return path
    }

    fun picCache(): File {
        return File(cache("pic")).check()
    }

    fun setting(): Path {
        val file = File(root()).check().resolve("setting.ini")
        return file.toPath()
    }

    fun log(): File {
        return File(root()).check().resolve("log.txt ")
    }

    fun logPath():String{
        return root().plus("${File.separator}log")
    }
}
