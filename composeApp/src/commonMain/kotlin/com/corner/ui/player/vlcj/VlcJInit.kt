package com.corner.ui.player.vlcj

import com.corner.bean.SettingStore
import com.corner.ui.getPlayerSetting
import com.corner.ui.scene.SnackBar
import com.corner.util.thisLogger
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery

class VlcJInit {
    companion object{
        private val log = thisLogger()
        private var controller: VlcjFrameController? = null

        fun setController(controller: VlcjFrameController) {
            this.controller = controller
        }

        fun init(notify:Boolean = false){
//            val resourcePath = System.getProperty(Constants.resPathKey)
//            log.info("resourcePath: $resourcePath")
//            if(StringUtils.isNotBlank(resourcePath)){
//                NativeLibrary.addSearchPath(
//                    RuntimeUtil.getLibVlcLibraryName(),
//                    Paths.get(resourcePath, "lib").pathString
//                )
//            }else{
//                log.warn("VlcJInit 未找到${Constants.resPathKey}环境变量")
//            }
            val discover = NativeDiscovery().discover()
            if(!discover && SettingStore.getPlayerSetting()[0] as Boolean) SnackBar.postMsg("未找到VLC播放器组件， 请安装VLC或者配置vlc可执行文件位置")
            if(notify) SnackBar.postMsg("VLC加载${if(discover) "成功" else "失败"}")
        }

        fun release(){
            controller?.release()
        }
    }
}