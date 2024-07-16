import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.KtorClient
import com.corner.catvodcore.util.Urls
import com.corner.catvodcore.util.Utils
import com.corner.server.KtorD
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.net.URI
import java.net.URL
import java.util.*
import kotlin.test.Test

class commonTest {
    private val url = "http://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json"

    //    private val url = "https://mirror.ghproxy.com/https://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json"
    private val fileUrl = "file://" + commonTest::class.java.getResource("/config.json").file

    //    private val fileUrl = "file://E:/Archives/compose-mutiplatform-workspace/0821.json"
    @Test
    fun exceptionTest(){
        val e = Exception("test")
        e.stackTraceToString()
    }

    @Test
    fun computeTest(){
        var a = 1
        a.plus(1)
        println(a)
    }

    @Test
    fun resolve(){
//        val convert = Urls.convert(
//            "F:/sync/compose-mutiplatform-workspace/My/CatVodSpider/json/config.json",
//            "alist.json"
//        )
//        val url1 = URI("F:/sync/compose-mutiplatform-workspace/My/CatVodSpider/json/config.json")
//        println(
//            Paths.get("F:/sync/compose-mutiplatform-workspace/My/CatVodSpider/json/config.json")
//                .resolve("./alist.json")
//        )
        println(URI("file:/F:/sync/compose-mutiplatform-workspace/My/CatVodSpider/json/config.json").resolve("./alist.json"))
//        val path = Paths.get(URI("F:/sync/compose-mutiplatform-workspace/My/CatVodSpider/json/config.json"))
//            .resolve("./alist.json").toString()
//        println(path)
    }

    @Test
    fun md5Test(){
        println(Utils.md5(File("F:\\sync\\compose-mutiplatform-workspace\\My\\CatVodSpider\\jar\\spider.jar")))
    }

    @Test
    fun splitTest() {
        val s = "$$$$$$"
        val split = s.split("/$/$/$".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        println(split)
    }

    @Test fun otherTest(){
        println("161fa5a0-a30b-4568-9e84-b3cd637cc8fe".uppercase(Locale.getDefault()))
    }

    @Test
    fun urlTest(){
        val s = "file://F:/sync/compose-mutiplatform-workspace/TV-Multiplatform/shared/src/desktopTest/resources/config.json"
        val s2 = "file://F:/sync/compose-mutiplatform-workspace/TV-Multiplatform/shared/src/desktopTest/resources/config.json";
        val url1 = URL(s2)
//        val uri = URI(s)
        println(url1.toURI().resolve("../sss.js"))
        println(fileUrl)

    }

    @Test
    fun ktorTest() {
        runBlocking { KtorD.init() }
        Thread.sleep(50000)
    }

    @Test
    fun trimTest(){
        var s = "\"https://gh-proxy.com/https://raw.githubusercontent.com/FongMi/CatVodSpider/main/json/alist.json\""
        println(s.apply { s.trim('"') })
    }


    /**
     * yrlclassLoader无法加载dex, 而且用于安卓的jar包有很多不兼容 只能重写
     */
    @Test
    fun classLoaderTest() {
//        val urlClassLoader =
//            URLClassLoader(
//                arrayOf(
//                    File("F:/sync/compose-mutiplatform-workspace/CatVodSpider/CatVodSpider/build/libs/CatVodSpider-1.0-SNAPSHOT.jar").toURI()
//                        .toURL()
//                ), commonTest::class.java.classLoader
//            )
//        println(commonTest::class.java.classLoader)
//        val loadClass: Spider = urlClassLoader.loadClass("com.github.catvod.spider.Wogg").getDeclaredConstructor().newInstance() as Spider
//
//        loadClass.init()

//        val exists =
//            File("F:/sync/compose-mutiplatform-workspace/TV-Multiplatform/shared/src/desktopTest/resources/7638addc233624a31deb7c569a6bcbc5.jar").exists()
//        val urlClassLoader1 = URLClassLoader(
//            arrayOf(
//                File("F:/sync/compose-mutiplatform-workspace/TV-Multiplatform/shared/src/desktopTest/resources/7638addc233624a31deb7c569a6bcbc5.jar").toURI()
//                    .toURL()
//            )
//        )
//        val loadClass1 = urlClassLoader1.loadClass("com.github.catvod.spider.WoGG")
    }

    @Test
    fun dataBaseTest() {
        val a = 10
        (0 until a step 3).map { i ->
            {
                println(i)
            }
        }
    }

    @Test
    fun playerTest(){
        val string = Jsons.decodeFromString<Result>(
            "{\n" +
                    "    \"header\": \"{/\"User-Agent/\":/\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36/\",/\"Referer/\":/\"https://www.aliyundrive.com//\"}\",\n" +
                    "    \"format\": \"application/octet-stream\",\n" +
                    "    \"url\": \"http://127.0.0.1:-1/proxy?do=ali&type=video&cate=open&shareId=oZ57YSztbiv&fileId=65818959490bb29ff891425781615706f974e445\",\n" +
                    "    \"subs\": [],\n" +
                    "    \"parse\": 0,\n" +
                    "    \"jx\": 0\n" +
                    "}"
        )
        println(string)
    }

    @Test
    fun headerTest(){
        var s = "{\"User-Agent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36\",\"Referer\":\"https://www.aliyundrive.com/\"}"
        val encodeToJsonElement = Jsons.encodeToJsonElement(s)
    }

    @Test
    fun flowTest(){
        runBlocking {
            (1..3).asFlow().collect{println(it)}
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun hotTest(){

        // {"data":[{"title":"与凤行","comment":"赵丽颖林更新绝美仙侠恋","upinfo":"更新至30集","doubanscore":"","id":0,"cat":2,"pv":"219,500","cover":"https://p7.qhimg.com/d/dy_dc82e931cba0d62a8f59246055e1fdda.jpg","url":"http://www.360kan.com/tv/QbJvaX7mSGPoMH.html","percent":"1","ent_id":"QbJvaX7mSGPoMH","moviecat":["古装","言情","奇幻","剧情"],"vip":true,"description":"该剧改编自晋江文学城九鹭非香小说《本王在此》，讲述了灵界碧苍王沈璃（赵丽颖 饰），因逃婚受伤坠落人间，机缘巧合下偶遇下凡体验生活的最后一位上古神行止（林更新 饰），两人纵横三界，携手展开了一段强强联合、充满烟火气和磅礴之力的新神话爱情故事。","pubdate":"2024-03-18"},
        runBlocking {
            val response = KtorClient.client.get("https://api.web.360kan.com/v1/rank?cat=1") {
                headers {
                    set(HttpHeaders.Referrer, "https://www.360kan.com/rank/general")
                }
            }
//            val hot = Jsons.decodeFromStream<Hot>(response.bodyAsChannel().toInputStream())
            println(response.bodyAsText())
        }
    }

    @Test fun urlResolve(){
        var convert: String = Urls.convert(
            "https://raw.githubusercontent.com/Greatwallcorner/CatVodSpider/master/json/config.json",
            "../jar/spider.jar"
        )
        println(convert)
    }

}

