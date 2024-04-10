package com.corner.util.play

import MPC
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.catvodcore.bean.Result
import com.corner.ui.scene.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

/**
@author heatdesert
@date 2024-01-07 21:50
@description
 */

private val log = LoggerFactory.getLogger("Player")

class Play {
    companion object{
        fun start(result:Result?, title: String?){
            CoroutineScope(Dispatchers.IO).launch{
                getProcessBuilder(result, title)?.start()
            }
        }
    }
}

/**
 * potplauer
 * vlc
 * mpc-be
 */
fun getProcessBuilder(result: Result?, title: String?): ProcessBuilder? {
    if (result == null) return null
    val playerPath = SettingStore.getSettingItem(SettingType.PLAYER.id)
    if(!checkPlayer(playerPath)) {
        log.info("未配置播放器 使用默认播放器")
        val resourcesDir = File(System.getProperty("compose.application.resources.dir")).resolve("MPC-BE\\mpc-be64.exe").path
        if(resourcesDir.isNullOrBlank()){
            return null
        }
        return MPC.getProcessBuilder(result, title ?: "TV", resourcesDir)
    }
    val compare = File(playerPath).name.lowercase(Locale.getDefault())
    if(compare.contains("potplayer")){
        return PotPlayer.getProcessBuilder(result,title ?: "TV", playerPath)
    }else if(compare.contains("vlc")){
        return VLC.getProcessBuilder(result, title ?: "TV", playerPath)
    }
    else if(compare.contains("mpc-be")){
        return MPC.getProcessBuilder(result, title ?: "TV", playerPath)
    }
    return Default.getProcessBuilder(result, title ?: "TV", playerPath)
}

private fun checkPlayer(playerPath:String):Boolean{
//    if(StringUtils.isBlank(playerPath)){
//        SnackBar.postMsg("请配置播放器路径")
//        return false
//    }
    val file = File(playerPath)
//    if(!file.exists() || !file.canExecute()){
//        SnackBar.postMsg("播放器文件不存在：$playerPath, 或不可执行")
//        return false
//    }
    return StringUtils.isNotBlank(playerPath) && (file.exists() || file.canExecute())
}