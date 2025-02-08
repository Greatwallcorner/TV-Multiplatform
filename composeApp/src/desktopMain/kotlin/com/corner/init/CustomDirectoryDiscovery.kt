package com.corner.init

import cn.hutool.system.SystemUtil
import com.corner.bean.SettingStore
import com.corner.bean.SettingType
import com.corner.util.Constants
import com.corner.util.SysVerUtil
import com.corner.util.thisLogger
import com.corner.util.trimBlankChar
import org.apache.commons.lang3.StringUtils
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider
import java.io.File


class CustomDirectoryDiscovery:DiscoveryDirectoryProvider {
    private val log = thisLogger()
    override fun priority(): Int {
        return 99999
    }


    override fun directories(): Array<String> {
        val arrayOf = mutableListOf<String>()
        System.getProperty(Constants.resPathKey)?.run {
            log.debug("resPath: $this")
            arrayOf.add(this.trimBlankChar())
        }
        val debugPath = File(System.getProperty("user.dir")).resolve("src/desktopMain/resources/res/${getOsArchName()}/lib")
        if(StringUtils.isNotBlank(debugPath.toString())){
            arrayOf.add(debugPath.toString())
        }
        val playerPath = SettingStore.getSettingItem(SettingType.PLAYER.id).split("#")
        if(playerPath.size > 1 && StringUtils.isNotBlank(playerPath[1])){
            val path = playerPath[1].trimBlankChar()
            if(!File(path).exists()) return arrayOf.toTypedArray()
            arrayOf.add(File(path).parent)
        }
        log.info("自定义vlc播放器路径：$arrayOf")
        return arrayOf.toTypedArray()
    }

    private fun getOsArchName():String{
        val osName = SysVerUtil.getOsName()
        return if(SysVerUtil.getOsName() == SysVerUtil.System.WINDOWS) "${osName.name.lowercase()}-${getArchName()}"
        else "${osName.name.lowercase()}-${SystemUtil.getOsInfo().arch}"
    }

    private fun getArchName():String{
        val arch = SystemUtil.getOsInfo().arch
        return if(arch == "amd64") "x86"
        else arch
    }

    override fun supported(): Boolean {
        return true
    }
}