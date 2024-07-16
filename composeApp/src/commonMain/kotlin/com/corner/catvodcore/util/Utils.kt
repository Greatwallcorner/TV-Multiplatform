package com.corner.catvodcore.util

import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Db
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

object Utils {
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

    fun equals(name: String, md5: String): Boolean {
        return md5(Paths.jar(name)).equals(md5, ignoreCase = true)
    }

    fun md5(file: File?): String {
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
        return key + Db.SYMBOL + id + Db.SYMBOL + ApiConfig.api.cfg.value?.id!!
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