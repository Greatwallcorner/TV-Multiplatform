package com.corner.util.play

import MPC
import PotPlayer
import cn.hutool.core.util.ZipUtil
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.v
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FilenameFilter
import java.util.*

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
    if(SystemUtils.IS_OS_MAC){
        return if(checkPlayer(playerPath)){
         ProcessBuilder("open", "-a", playerPath, result.url?.v() ?: "")
        }else{
            ProcessBuilder("open", result.url?.v() ?: "")
        }
    }
    if(!checkPlayer(playerPath)) {
        log.info("未配置播放器 使用默认播放器")
        val path = getDefaultPlayerPath()
        log.info("默认播放器路径:{}", path)
        if(path.isBlank()){
            return null
        }
        return MPC.getProcessBuilder(result, title ?: "TV", path)
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

fun getDefaultPlayerPath():String {
    val resourcesDir = File(System.getProperty("compose.application.resources.dir"))
    // 已经解压
    var exeList = resourcesDir.resolve("mpc-hc").list(FilenameFilter { _, name -> name.lowercase().matches(Regex("mpc-hc\\X*.exe")) })
    if(exeList != null && exeList.isNotEmpty()) return resourcesDir.resolve("mpc-hc").resolve(exeList[0]).path

    val list = resourcesDir.list(FilenameFilter { _, name -> name.lowercase().matches(Regex("mpc-hc\\X*.zip")) })
    if(list == null || list.isEmpty()) {
        log.error("没有找到默认播放器压缩包")
        return ""
    }
    val destDir = resourcesDir.resolve("mpc-hc")
    log.info("解压默认播放器 MPC-HC")

    ZipUtil.unzip(resourcesDir.resolve(list[0]), destDir.path.toPath().toFile())
    exeList = destDir.list(FilenameFilter { _, name -> name.lowercase().matches(Regex("mpc-hc\\X*.exe")) })
    if(exeList == null || exeList.isEmpty()) {
        log.error("没有找到播放器exe")
        return ""
    }
    return destDir.resolve(exeList[0]).path
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