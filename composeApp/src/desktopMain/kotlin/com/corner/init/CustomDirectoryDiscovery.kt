package com.corner.init

import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.util.Constants
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider
import java.io.File


class CustomDirectoryDiscovery:DiscoveryDirectoryProvider {
    private val resPath = System.getProperty(Constants.resPathKey)

    private var vlcPath:String? = null

    init {
        vlcPath = File(resPath).resolve("vlc").path
    }

    override fun priority(): Int {
        return 50
    }

    override fun directories(): Array<String> {
        return arrayOf(vlcPath ?: "", SettingStore.getSettingItem(SettingType.PLAYER.id))
    }

    override fun supported(): Boolean {
        return true
    }
}