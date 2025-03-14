package com.corner.catvodcore.util

import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Db
import com.corner.util.Constants
import com.google.common.net.HttpHeaders
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import java.util.*

object Utils {

    // 可下载
    private val prefix = listOf("magnet")

    fun isDownloadLink(str:String):Boolean{
        prefix.forEach { if(str.startsWith(it)) return true }
        return false
    }

    fun substring(text: String?): String? {
        return substring(text, 1)
    }

    fun substring(text: String?, num: Int): String? {
        return if (text != null && text.length > num) {
            text.substring(0, text.length - num)
        } else {
            text
        }
    }

    fun md5(str:String):String{
        try {
            if(StringUtils.isBlank(str)) return ""
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(str.toByteArray())
            val bigInteger = BigInteger(1, bytes)
            val stringBuilder = StringBuilder(bigInteger.toString(16))
            while(stringBuilder.length < 32) stringBuilder.insert(0, "0")
            return stringBuilder.toString().lowercase()
        } catch (e: Exception) {
            return ""
        }

    }

    var webHttpHeaderMap: HashMap<String, String> = HashMap()
    /**
     * @param referer
     * @param cookie 多个cookie name=value;name2=value2
     * @return
     */
    fun webHeaders(referer: String, cookie: String): HashMap<String, String> {
        val map = webHeaders(referer)
        map[HttpHeaders.COOKIE] = cookie
        return map
    }

    fun webHeaders(referer: String): HashMap<String, String> {
        if (webHttpHeaderMap.isEmpty()) {
                if (webHttpHeaderMap.isEmpty()) {
                    webHttpHeaderMap = HashMap<String, String>()
                    //                    webHttpHeaderMap.put(HttpHeaders.CONTENT_TYPE, ContentType.Application.INSTANCE.getJson().getContentType());
//                    webHttpHeaderMap.put(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
                    webHttpHeaderMap.put(
                        org.apache.http.HttpHeaders.CONNECTION,
                        "keep-alive"
                    )
                    webHttpHeaderMap.put(
                        org.apache.http.HttpHeaders.USER_AGENT,
                        Constants.ChromeUserAgent
                    )
                    webHttpHeaderMap.put(org.apache.http.HttpHeaders.ACCEPT, "*/*")
                }
        }
        if(StringUtils.isNotBlank(referer)) {
            val uri = URI.create(referer)
            val u = uri.scheme + "://" + uri.host
            webHttpHeaderMap[org.apache.http.HttpHeaders.REFERER] = u
            webHttpHeaderMap[io.ktor.http.HttpHeaders.Origin] = u
        }
        return webHttpHeaderMap
    }


    fun equals(name: String, md5: String): Boolean {
        return md5(Paths.jar(name)).equals(md5, ignoreCase = true)
    }

    fun md5(file: File): String {
        try {
            val digest = MessageDigest.getInstance("MD5")
            val fis = FileInputStream(file)
            val bytes = ByteArray(4096)
            var count: Int
            while ((fis.read(bytes).also { count = it }) != -1) digest.update(bytes, 0, count)
            fis.close()
            val sb = java.lang.StringBuilder()
            for (b in digest.digest()) sb.append(((b.toInt() and 0xff) + 0x100).toString(16).substring(1))
            return sb.toString()
        } catch (e: java.lang.Exception) {
            return ""
        }
    }

//    private fun md5()

    fun base64(s: String): String {
        return base64(s.toByteArray())
    }

    fun base64(bytes: ByteArray?): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun getDigit(text: String): Int {
        try {
            if (text.startsWith("上") || text.startsWith("下")) return -1
            return text.replace("(?i)(mp4|H264|H265|720p|1080p|2160p|4K)".toRegex(), "").replace("\\D+".toRegex(), "")
                .toInt()
        } catch (e: java.lang.Exception) {
            return -1
        }
    }

    fun getHistoryKey(key:String, id:String): String {
        return key + Db.SYMBOL + id + Db.SYMBOL + ApiConfig.api.cfg?.id!!
    }

    fun formatMilliseconds(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60)) % 24
        val days = milliseconds / (1000 * 60 * 60 * 24)

        return if (days > 0) {
            "%d天 %02d:%02d:%02d".format(days, hours, minutes, seconds)
        }else if(hours > 0){
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        }else{
            "%02d:%02d".format(minutes, seconds)
        }
    }


//    fun parse

}