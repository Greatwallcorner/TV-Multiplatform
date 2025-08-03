package com.corner.ui.player.vlcj

import com.corner.bean.SettingStore
import com.corner.ui.getPlayerSetting
import com.corner.util.thisLogger
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery

class VlcJInit {
    companion object {
        private val log = thisLogger()
        private var controller: VlcjFrameController? = null
        @Volatile private var isReleased = false

        fun setController(controller: VlcjFrameController) {
            this.controller = controller
            isReleased = false
        }

        fun init(notify: Boolean = false) {
            val discover = NativeDiscovery().discover()
            if (!discover && SettingStore.getPlayerSetting()[0] as Boolean) {
                SnackBar.postMsg("未找到VLC播放器组件，请安装VLC或者配置vlc可执行文件位置", type = SnackBar.MessageType.ERROR)
            }
            if (notify) SnackBar.postMsg("VLC加载${if (discover) "成功" else "失败"}", type = SnackBar.MessageType.INFO)
        }

        /**
         * Vlc释放时要避免出现Invalid memory access问题
         * */

        fun release() {
            if (isReleased) {
                log.debug("VLC已全局释放，跳过")
                return
            }

            synchronized(this) {
                if (isReleased) return
                isReleased = true

                try {
                    controller?.let { ctrl ->
                        if (ctrl.hasPlayer()) {
                            ctrl.release()
                        }
                    }
                } catch (e: Throwable) {
                    log.error("VLC释放异常", e)
                } finally {
                    controller = null
                }
            }
        }
    }
}