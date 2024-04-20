package com.corner.catvodcore.util

import com.corner.util.OperatingSystem
import com.corner.util.UserDataDirProvider
import java.io.File
import java.nio.file.Path

object Paths {
    private val runPath = System.getProperty("user.dir")
    private val classPath = System.getProperty("java.class.path")
    private val ApplicationName = "TV-Multiplatform"

    private val userDataDir = getUserDataDir()

    private fun getUserDataDir() = run {
        when (UserDataDirProvider.currentOs) {
            OperatingSystem.Windows -> File(System.getenv("AppData"), "$ApplicationName/cache")
            OperatingSystem.Linux -> File(System.getProperty("user.home"), ".cache/$ApplicationName")
            OperatingSystem.MacOS -> File(System.getProperty("user.home"), "Library/Caches/$ApplicationName")
            OperatingSystem.Unknown -> throw RuntimeException("未知操作系统")
        }
    }

    private fun File.check():File{
        if (!exists()) {
            mkdirs()
        }
        return this
    }

    fun root():File{
        return File(runPath).resolve("data")
    }

    fun userDataRoot():File{
        return userDataDir
    }

    fun doh(): File {
        return cache( "doh").check()
    }

    private fun cache(path: String): File {
        return root().resolve("cache").resolve(path)
    }

    fun db():String{
        val path = userDataRoot().resolve("db").check().resolve("db.db")
        return "jdbc:sqlite:${path}"
    }

    fun local(jar: String): File {
        val file = File(jar.replace("file:/", "").replace("file:\\", ""))
        return if(file.exists()) file else File(jar)
    }

    fun jar(): File {
        return cache("jar").check()
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
        return cache("pic").check()
    }

    fun setting(): Path {
        val file = userDataRoot().check().resolve("setting.ini")
        return file.toPath()
    }

    fun log(): File {
        return root().check().resolve("log.txt")
    }

    fun logPath():File{
        return root().resolve("log")
    }
}
