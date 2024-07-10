package com.corner.ui.player.vlcj

import com.corner.ui.scene.SnackBar
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery

class VlcJInit {
    companion object{
        fun init(){
//            val resourcePath = System.getProperty(Constants.resPathKey)
//            if(StringUtils.isNotBlank(resourcePath)){
//                NativeLibrary.addSearchPath(
//                    RuntimeUtil.getLibVlcLibraryName(),
//                    Paths.get(resourcePath, "lib").pathString
//                )
//            }else{
//                println("VlcJInit 未找到${Constants.resPathKey}环境变量")
//            }
            val discover = NativeDiscovery().discover()
            if(!discover) SnackBar.postMsg("未找到VLC组件， 请安装VLC或者配置vlc可执行文件位置")
        }
    }
}