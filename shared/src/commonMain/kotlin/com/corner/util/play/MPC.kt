import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.v
import com.corner.util.play.PlayerCommand

object MPC: PlayerCommand {
    override fun title(title: String): String {
        TODO("Not yet implemented")
    }

    // 毫秒
    override fun start(time: String): String {
        return String.format("/start %s", time)
    }

    override fun subtitle(s: String): String {
        return String.format("/sub \"%s\"", s)
    }

    override fun getProcessBuilder(result: Result, title: String, playerPath: String): ProcessBuilder {
        return ProcessBuilder(playerPath, result.url?.v(), "/play")
    }
}