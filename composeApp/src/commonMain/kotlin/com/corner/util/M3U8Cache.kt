import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("M3U8Cache")
object M3U8Cache {
    private val cache = ConcurrentHashMap<String, String>()

    fun put(content: String): String {
//        logger.debug("缓存 M3U8 内容为 $content")
        val id = UUID.randomUUID().toString()
        cache[id] = content
        return id
    }

    fun get(id: String): String? = cache.remove(id) // 取完后自动清除
}