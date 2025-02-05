package com.corner.util

import cn.hutool.system.SystemUtil

object SysVerUtil {
    fun isWin10():Boolean{
        return SystemUtil.getOsInfo().name.equals("Windows 10")
    }

    fun getOsName():System{
        val osInfo = SystemUtil.getOsInfo()
        if(osInfo.isLinux) return System.LINUX
        if(osInfo.isMac) return System.MAC
        if(osInfo.isWindows) return System.WINDOWS
        return System.UNKNOWN
    }

    enum class System(name:String){
        WINDOWS("windows"),
        MAC("macos"),
        LINUX("linux"),
        UNKNOWN("unknown");
    }
}