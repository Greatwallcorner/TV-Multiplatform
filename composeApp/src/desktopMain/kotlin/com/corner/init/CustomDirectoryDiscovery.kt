package com.corner.init

import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.util.trimBlankChar
import org.apache.commons.lang3.StringUtils
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider
import java.io.File


class CustomDirectoryDiscovery:DiscoveryDirectoryProvider {
//    private val resPath = System.getProperty(Constants.resPathKey) ?: ""
//
//    private var vlcPath:String? = null
//
//    init {
//        if(resPath.isNotBlank()){
//            vlcPath = File(resPath).resolve("lib").path
//        }
//    }
//
    override fun priority(): Int {
        return 99999
    }


    override fun directories(): Array<String> {
        val arrayOf = mutableListOf<String>()
//        if(StringUtils.isNotBlank(vlcPath)){
//            arrayOf.add(vlcPath!!)
//        }
        val playerPath = SettingStore.getSettingItem(SettingType.PLAYER.id).split("#")
        if(playerPath.size > 1 && StringUtils.isNotBlank(playerPath[1])){
            var path = playerPath[1].trimBlankChar()
            if(!File(path).exists()) return arrayOf.toTypedArray()
            arrayOf.add(File(path).parent)
        }
        println("自定义vlc播放器路径：$arrayOf")
        return arrayOf.toTypedArray()
    }

    override fun supported(): Boolean {
        return true
    }
}