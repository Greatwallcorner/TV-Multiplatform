import com.corner.server.logic.multiThreadDownload
import kotlin.test.Test

class MultiThreadDownloadTest {
    var downloadUrl = "https://dldir1.qq.com/qqfile/qq/PCQQ9.7.20/QQ9.7.20.29269.exe"

    @Test
    fun test(){
        multiThreadDownload(downloadUrl, 5, null)
    }

}