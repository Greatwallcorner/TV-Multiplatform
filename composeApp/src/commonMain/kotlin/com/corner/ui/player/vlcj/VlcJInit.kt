package com.corner.ui.player.vlcj

import androidx.compose.ui.unit.Constraints
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
                NativeDiscovery().discover()
            }else{
                println("VlcJInit 未找到${Constants.resPathKey}环境变量")
            }
        }
    }
}