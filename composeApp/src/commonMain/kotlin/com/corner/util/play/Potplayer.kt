import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.v
import com.corner.catvodcore.util.Paths
import com.corner.util.play.PlayerCommand

object PotPlayer: PlayerCommand {
    private val TITLE = "/title=\"%S\""
    // time format is: hh:mm:ss.ms (OR specify seconds only e.g. /seek=1800 to start at 30th min)
    private val SEEK = "/seek=\"%S\""
    private val HEADERS = "/headers=\"%S\""
    // Loads the specified subtitle(s) from the specified paths or URLs.
    private val SUB = "/sub=\"%s\""
    // Appends the specified content(s) into playlist.
    private val ADD = "/add=\"%s\""
    fun currentProcess(): String {
        return "/current"
    }

    override fun title(title:String):String{
        return String.format(TITLE, title).replace(" ", "").replace("\t","")
    }

    override fun start(time:String):String{
        return String.format(SEEK, time)
    }

    override fun subtitle(s:String):String{
        return String.format(SUB, s)
    }

    fun add(s:String):String{
        return String.format(ADD, s)
    }

    fun headers(headers:Map<String, String>?):String{
        return String.format(HEADERS, buildHeaderStr(headers))
    }

    override fun getProcessBuilder(result: Result, title: String, playerPath: String):ProcessBuilder{
        val processBuilder = ProcessBuilder(
            playerPath,
            url(result.url.v()),
            currentProcess(),
            headers(result.header),
            title(title)
        )
        return processBuilder.redirectOutput(Paths.log())
    }

    override fun getProcessBuilder(url: String, title: String, playerPath: String): ProcessBuilder {
        val processBuilder = ProcessBuilder(
            playerPath,
            url(url),
            currentProcess(),
            title(title)
        )
        return processBuilder.redirectOutput(Paths.log())
    }
}