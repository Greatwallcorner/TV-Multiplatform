package com.corner.catvodcore.util

import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Db
import org.apache.commons.lang3.StringUtils
import java.math.BigInteger
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
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