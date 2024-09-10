package com.corner.util

import cn.hutool.system.SystemUtil

object SysVerUtil {
    fun isWin10():Boolean{
        return SystemUtil.getOsInfo().name.equals("Windows 10")
    }
}