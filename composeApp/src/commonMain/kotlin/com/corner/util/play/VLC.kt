package com.corner.util.play

import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.v

object VLC: PlayerCommand {
    override fun title(title: String): String {
        return String.format("--video-title=%s", title)
    }

    override fun start(time: String): String {
        return String.format("--start-time=%s", time)
    }

    override fun subtitle(s: String): String {
        return String.format("--sub-file=%s", s)
    }

    override fun getProcessBuilder(result: Result, title: String, playerPath: String): ProcessBuilder {
        return ProcessBuilder(playerPath, title(title), /*"--playlist-tree",*/ url(result.url.v()))
    }

    override fun getProcessBuilder(url: String, title: String, playerPath: String): ProcessBuilder {
        return ProcessBuilder(playerPath, title(title), /*"--playlist-tree",*/ url)
    }
}