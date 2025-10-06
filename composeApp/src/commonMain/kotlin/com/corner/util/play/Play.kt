package com.corner.util.play

import MPC
import PotPlayer
import cn.hutool.core.util.ZipUtil
import com.corner.bean.SettingStore
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.v
import com.corner.catvodcore.util.Paths
import com.corner.ui.getPlayerSetting
import com.corner.util.Constants
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

private val log = LoggerFactory.getLogger("Play")

class Play {
    companion object{
        fun start(result:Result?, title: String?){
            CoroutineScope(Dispatchers.IO).launch{
                getProcessBuilder(result, title)?.start()
            }
        }
        fun start(url: String, title: String?){
            CoroutineScope(Dispatchers.IO).launch{
                getProcessBuilder(url, title)?.start()
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
    val playerPath = SettingStore.getPlayerSetting()[1] as String

    if(SystemUtils.IS_OS_MAC){
        return if(checkPlayer(playerPath)){
         ProcessBuilder("open", "-a", playerPath, result.url.v()).redirectOutput(Paths.playerLog())
        }else{
            ProcessBuilder("open", result.url.v()).redirectOutput(Paths.playerLog())
        }
    }

    val compare = File(playerPath).name.lowercase(Locale.getDefault())
    if(compare.contains("potplayer") && checkPlayer(playerPath)){
        return PotPlayer.getProcessBuilder(result,title ?: "TV", playerPath)
    }else if(compare.contains("vlc") && checkPlayer(playerPath)){
        return VLC.getProcessBuilder(result, title ?: "TV", playerPath)
    }
    else if(compare.contains("mpc-be") && checkPlayer(playerPath)){
        return MPC.getProcessBuilder(result, title ?: "TV", playerPath)
    }
//    return Default.getProcessBuilder(result, title ?: "TV", playerPath)
    return null
}

fun getProcessBuilder(url:String, title: String?): ProcessBuilder? {
    if (StringUtils.isBlank(url)) return null
    val playerPath = SettingStore.getPlayerSetting()[1] as String

    if(SystemUtils.IS_OS_MAC){
        return if(checkPlayer(playerPath)){
            ProcessBuilder("open", "-a", playerPath, url)
        }else{
            ProcessBuilder("open", url)
        }
    }

    val compare = File(playerPath).name.lowercase(Locale.getDefault())
    if(compare.contains("potplayer") && checkPlayer(playerPath)){
        return PotPlayer.getProcessBuilder(url,title ?: "TV", playerPath)
    }else if(compare.contains("vlc") && checkPlayer(playerPath)){
        return VLC.getProcessBuilder(url, title ?: "TV", playerPath)
    }
    else if(compare.contains("mpc-be") && checkPlayer(playerPath)){
        return MPC.getProcessBuilder(url, title ?: "TV", playerPath)
    }
//    return Default.getProcessBuilder(url, title ?: "TV", playerPath)
    return null
}


//default player [mpc-hc]
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

/**
 * @param name dest dir name
 * @param exePattern 匹配exe可执行文件的regx "mpc-hc\\X*.exe"
 */
fun findAndExtract(dirName:String, exePattern:String): String? {
    val resourcesDir = File(System.getProperty(Constants.resPathKey))
    var exeList = resourcesDir.resolve(dirName).list(FilenameFilter { _, name -> name.lowercase().matches(Regex(exePattern)) })
    if(exeList != null && exeList.isNotEmpty()) return resourcesDir.resolve(dirName).resolve(exeList[0]).path

    val list = resourcesDir.list(FilenameFilter { _, name -> name.lowercase().matches(Regex(exePattern)) })
    if(list == null || list.isEmpty()) {
        log.error("没有找到压缩包")
        return ""
    }
    val destDir = resourcesDir.resolve(dirName)
    log.info("解压压缩包 $list")

    ZipUtil.unzip(resourcesDir.resolve(list[0]), destDir.path.toPath().toFile())
    exeList = destDir.list(FilenameFilter { _, name -> name.lowercase().matches(Regex(exePattern)) })
    if(exeList == null || exeList.isEmpty()) {
        log.error("没有找到播放器exe")
        return ""
    }
    return exeList.first()
 }

//check player file is exist and can execute
private fun checkPlayer(playerPath:String):Boolean{
    if(StringUtils.isBlank(playerPath)){
        SnackBar.postMsg("请配置播放器路径",type = SnackBar.MessageType.WARNING)
        log.warn("播放器路径为空")
        return false
    }
    val file = File(playerPath)
    if(!file.exists() || !file.canExecute()){
        SnackBar.postMsg("播放器文件不存在：$playerPath, 或不可执行",type = SnackBar.MessageType.ERROR)
        log.error("播放器文件不存在：$playerPath, 或不可执行")
        return false
    }
    return StringUtils.isNotBlank(playerPath) && (file.exists() || file.canExecute())
}