package com.corner.util


class UserDataDirProvider {
    companion object{
        fun getCurrentOperatingSystem(): OperatingSystem {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("linux") -> OperatingSystem.Linux
                osName.contains("mac") || osName.contains("darwin") -> OperatingSystem.MacOS
                osName.contains("windows") -> OperatingSystem.Windows
                else -> OperatingSystem.Unknown
            }
        }
        val currentOs = getCurrentOperatingSystem()
    }
}

enum class OperatingSystem {
    Linux,
    MacOS,
    Windows,
    Unknown
}
