package com.corner.util.play

import com.corner.catvodcore.bean.Result
import com.corner.ui.getSettingItem
import com.corner.ui.scene.SnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.util.*

/**
@author heatdesert
@date 2024-01-07 21:50
@description
 */

class Play {
    companion object{
        fun start(result:Result?, title: String?){
            CoroutineScope( Dispatchers.IO).launch{
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
    val playerPath = getSettingItem("player")
    if(!checkPlayer(playerPath)) return null
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
    if(StringUtils.isBlank(playerPath)){
        SnackBar.postMsg("请配置播放器路径")
        return false
    }
    val file = File(playerPath)
    if(!file.exists() || !file.canExecute()){
        SnackBar.postMsg("播放器文件不存在：$playerPath, 或不可执行")
        return false
    }
    return true
}