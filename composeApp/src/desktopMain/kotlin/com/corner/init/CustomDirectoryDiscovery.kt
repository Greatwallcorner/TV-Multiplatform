package com.corner.init

import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.util.Constants
import org.apache.commons.lang3.StringUtils
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider
import java.io.File


class CustomDirectoryDiscovery:DiscoveryDirectoryProvider {
    private val resPath = System.getProperty(Constants.resPathKey) ?: ""

    private var vlcPath:String? = null

    init {
        if(resPath.isNotBlank()){
            vlcPath = File(resPath).resolve("vlc").path
        }
    }

    override fun priority(): Int {
        return 50
    }

    override fun directories(): Array<String> {
        val arrayOf = mutableListOf<String>(vlcPath ?: "")
        val playerPath = SettingStore.getSettingItem(SettingType.PLAYER.id)
        if(StringUtils.isNotBlank(playerPath)){
            arrayOf.add(File(playerPath).parent)
        }
        return arrayOf.toTypedArray()
    }

    override fun supported(): Boolean {
        return true
    }
}