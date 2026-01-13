package com.corner.util.update

import com.corner.catvodcore.util.Paths
import com.corner.util.OperatingSystem
import com.corner.util.UserDataDirProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.core.use
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class UpdateLauncher {
    companion object {
        private val log = LoggerFactory.getLogger(UpdateLauncher::class.java)
        private const val UPDATER_VERSION_URL =
            "https://api.github.com/repos/xiaolong0302/LumenTV-Compose/releases/latest"

        suspend fun launchUpdater(zipFile: File, updaterUrl: String? = null): Boolean {
            return try {
                val userDataDir = Paths.userDataRoot()
                val updaterDir = userDataDir.resolve("updater")

                Files.createDirectories(updaterDir.toPath())

                val updaterFile = downloadUpdater(updaterDir.toPath(), updaterUrl)

                if (!updaterFile.exists()) {
                    log.error("Updater not found: $updaterFile")
                    return false
                }

                val currentDir = getCurrentDirectory()
                val tempDir = System.getProperty("java.io.tmpdir")
                val tempZipFile = File(tempDir, "LumenTV-update.zip")

                val processBuilder = ProcessBuilder()

                when (UserDataDirProvider.currentOs) {
                    OperatingSystem.Windows -> {
                        processBuilder.command(
                            "powershell",
                            "-Command",
                            "Start-Process",
                            "cmd.exe",
                            "-ArgumentList",
                            "\"/c\", \"\\\"${updaterFile.absolutePath}\\\"\", \"-path\", \"${currentDir}\", \"-file\", \"${tempZipFile.absolutePath}\"",
                            "-Verb",
                            "RunAs"
                        )
                    }

                    OperatingSystem.Linux -> {
                        processBuilder.command(
                            "gnome-terminal",
                            "--",
                            "sudo",
                            updaterFile.absolutePath,
                            "-path",
                            currentDir.toString(),
                            "-file",
                            tempZipFile.absolutePath
                        )
                    }

                    OperatingSystem.MacOS -> {
                        processBuilder.command(
                            "osascript",
                            "-e",
                            "tell application \"Terminal\" to do script \"sudo \\\"${updaterFile.absolutePath}\\\" -path \\\"${currentDir}\\\" -file \\\"${tempZipFile.absolutePath}\\\"\""
                        )
                    }

                    OperatingSystem.Unknown -> {
                        log.error("Unsupported operating system")
                        return false
                    }
                }

                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()

                log.info("Updater launched successfully")
                log.info("Updater: $updaterFile")
                log.info("Program path: $currentDir")
                log.info("Zip file: ${tempZipFile.absolutePath}")

                true
            } catch (e: Exception) {
                log.error("Failed to launch updater", e)
                false
            }
        }

        private suspend fun downloadUpdater(updaterDir: Path, updaterUrl: String?): File {
            val updaterName = when (UserDataDirProvider.currentOs) {
                OperatingSystem.Windows -> "updater.exe"
                OperatingSystem.Linux -> "updater"
                OperatingSystem.MacOS -> "updater"
                OperatingSystem.Unknown -> "updater"
            }

            val targetFile = updaterDir.resolve(updaterName).toFile()

            if (targetFile.exists()) {
                return targetFile
            }

            val url = updaterUrl ?: getUpdaterUrlFromGitHub()

            if (url != null) {
                try {
                    val client = HttpClient()
                    val response: HttpResponse = client.get(url)
                    val channel: ByteReadChannel = response.body()
                    val tempFile = File(updaterDir.toFile(), "${updaterName}.tmp")

                    // 使用正确的异步文件写入方式
                    withContext(Dispatchers.IO) {
                        val fileOutputStream = tempFile.outputStream()
                        try {
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (true) {
                                bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                                if (bytesRead <= 0) break
                                fileOutputStream.write(buffer, 0, bytesRead)
                            }
                        } finally {
                            fileOutputStream.close()
                        }
                    }

                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )

                    client.close()
                    log.info("Updater downloaded to: $targetFile")
                } catch (e: Exception) {
                    log.error("Failed to download updater", e)
                }
            }

            return targetFile
        }


        private suspend fun getUpdaterUrlFromGitHub(): String? {
            return withContext(Dispatchers.IO) {
                try {
                    val client = HttpClient()
                    val response: HttpResponse = client.get(UPDATER_VERSION_URL)
                    val versionInfo = kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                    }.decodeFromString<VersionInfo>(response.body())

                    val updaterUrl = versionInfo.updater_url
                    client.close()
                    updaterUrl
                } catch (e: Exception) {
                    log.error("Failed to get updater URL", e)
                    null
                }
            }
        }

        private fun getCurrentDirectory(): Path {
            val jarPath = File(UpdateLauncher::class.java.protectionDomain.codeSource.location.toURI()).toPath()
            return if (jarPath.fileName.toString().endsWith(".jar")) {
                jarPath.parent
            } else {
                jarPath.toAbsolutePath().parent
            }
        }

        fun exitApplication() {
            log.info("Exiting application for update...")
            System.exit(0)
        }
    }
}
