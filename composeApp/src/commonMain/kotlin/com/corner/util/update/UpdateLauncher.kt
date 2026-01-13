package com.corner.util.update

import com.corner.util.OperatingSystem
import com.corner.util.UserDataDirProvider
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

class UpdateLauncher {
    companion object {
        private val log = LoggerFactory.getLogger(UpdateLauncher::class.java)

        fun launchUpdater(zipFile: File): Boolean {
            return try {
                val currentDir = getCurrentDirectory()
                val updaterPath = getUpdaterPath(currentDir)

                if (!updaterPath.toFile().exists()) {
                    log.error("Updater not found at: $updaterPath")
                    return false
                }

                val processBuilder = ProcessBuilder()
                processBuilder.directory(currentDir.toFile())

                when (UserDataDirProvider.currentOs) {
                    OperatingSystem.Windows -> {
                        processBuilder.command(updaterPath.toString(), "-path", currentDir.toString(), "-file", zipFile.absolutePath)
                    }
                    OperatingSystem.Linux, OperatingSystem.MacOS -> {
                        processBuilder.command(updaterPath.toString(), "-path", currentDir.toString(), "-file", zipFile.absolutePath)
                    }
                    OperatingSystem.Unknown -> {
                        log.error("Unsupported operating system")
                        return false
                    }
                }

                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()

                log.info("Updater launched successfully")
                log.info("Updater path: $updaterPath")
                log.info("Program path: $currentDir")
                log.info("Zip file: ${zipFile.absolutePath}")

                true
            } catch (e: Exception) {
                log.error("Failed to launch updater", e)
                false
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

        private fun getUpdaterPath(currentDir: Path): Path {
            val updaterName = when (UserDataDirProvider.currentOs) {
                OperatingSystem.Windows -> "updater.exe"
                OperatingSystem.Linux -> "updater"
                OperatingSystem.MacOS -> "updater"
                OperatingSystem.Unknown -> "updater"
            }

            val updaterPath = currentDir.resolve(updaterName)

            if (updaterPath.toFile().exists()) {
                return updaterPath
            }

            val appDir = currentDir.resolve("LumenTV")
            if (appDir.toFile().exists()) {
                val appUpdaterPath = appDir.resolve(updaterName)
                if (appUpdaterPath.toFile().exists()) {
                    return appUpdaterPath
                }
            }

            return updaterPath
        }

        fun exitApplication() {
            log.info("Exiting application for update...")
            System.exit(0)
        }
    }
}
