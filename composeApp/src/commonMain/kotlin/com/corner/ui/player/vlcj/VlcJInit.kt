package com.corner.ui.player.vlcj

import com.corner.ui.scene.SnackBar
import com.corner.util.Constants
import com.sun.jna.NativeLibrary
import org.apache.commons.lang3.StringUtils
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import java.nio.file.Paths
import kotlin.io.path.pathString

class VlcJInit {
    companion object{
        fun init(){
            val resourcePath = System.getProperty(Constants.resPathKey)
            if(StringUtils.isNotBlank(resourcePath)){
                NativeLibrary.addSearchPath(
                    RuntimeUtil.getLibVlcLibraryName(),
                    Paths.get(resourcePath, "lib", "libvlc.dll").pathString
                )
            }else{
                println("VlcJInit 未找到${Constants.resPathKey}环境变量")
            }
            val discover = NativeDiscovery().discover()
            if(!discover) SnackBar.postMsg("未找到VLC组件， 请安装VLC或者配置vlc可执行文件位置")
        }
    }
}