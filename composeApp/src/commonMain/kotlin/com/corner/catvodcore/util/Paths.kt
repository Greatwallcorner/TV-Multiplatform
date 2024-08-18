package com.corner.catvodcore.util

import com.corner.ui.scene.SnackBar
import com.corner.util.OperatingSystem
import com.corner.util.UserDataDirProvider
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object Paths {
    //    private val runPath = System.getProperty("user.dir")
    private val classPath = System.getProperty("java.class.path")
    private val ApplicationName = "TV-Multiplatform"
    private val log = LoggerFactory.getLogger("Paths")

    private val userDataDir = getUserDataDir()

    private fun getUserDataDir() = run {
        when (UserDataDirProvider.currentOs) {
            OperatingSystem.Windows -> File(System.getenv("AppData"), "$ApplicationName/cache")
            OperatingSystem.Linux -> File(System.getProperty("user.home"), ".cache/$ApplicationName")
            OperatingSystem.MacOS -> File(System.getProperty("user.home"), "Library/Caches/$ApplicationName")
            OperatingSystem.Unknown -> throw RuntimeException("未知操作系统")
        }
    }

    private fun File.check(): File {
        if (!exists()) {
            mkdirs()
        }
        return this
    }

    fun root(): File {
        return userDataDir.resolve("data")
    }

    fun userDataRoot(): File {
        return userDataDir
    }

    fun doh(): File {
        return cache("doh").check()
    }

    private fun cache(path: String): File {
        return root().resolve("cache").resolve(path)
    }

    fun db(): String {
        val path = userDataRoot().resolve("db").check().resolve("db.db")
        return "jdbc:sqlite:${path}"
    }

    fun local(jar: String): File {
        val file = File(jar.replace("file:/", "").replace("file:\\", ""))
        return if (file.exists()) file else {
            log.info("jar文件不存在 $jar")
            SnackBar.postMsg("本地Jar文件不存在")
            File(jar)
        }
    }

    fun jar(): File {
        return cache("jar").check()
    }

    fun jar(fileName: String): File {
        return File(jar(), Utils.md5(fileName) + ".jar")
    }

    @JvmStatic
    fun js(): File {
        return cache("js").check()
    }

    @JvmStatic
    fun js(path: String): File {
        return cache("js").resolve(path)
    }

    @JvmStatic
    fun write(path: File, bytes: ByteArray?): File {
        if (bytes == null || bytes.isEmpty()) {
            return path
        } else {
            path.writeBytes(bytes)
        }

        return path
    }

    @JvmStatic
    fun read(file: File): String {
        return try {
            read(FileInputStream(file))
        } catch (e: Exception) {
            ""
        }
    }

    fun read(path: String): String {
        return try {
            read(FileInputStream(local1(path)))
        } catch (e: Exception) {
            ""
        }
    }

    fun read(`is`: InputStream): String {
        try {
            val data = ByteArray(`is`.available())
            `is`.read(data)
            `is`.close()
            return String(data, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    fun local1(path: String): File {
        val file1 = File(path.replace("file:/", ""))
        val file2 = File(path.replace("file:/", userDataDir.path))
        return if (file2.exists()) file2 else if (file1.exists()) file1 else File(path)
    }

    fun picCache(): File {
        return cache("pic").check()
    }

    fun setting(): Path {
        val file = userDataRoot().check().resolve("setting.ini")
        return file.toPath()
    }

    fun playerLog(): File {
        return root().check().resolve("playerLog.txt")
    }

    fun logPath(): File {
        return root().resolve("log")
    }

}
