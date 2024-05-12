package com.corner.catvodcore.util

import org.apache.commons.lang3.StringUtils
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

    fun base64(s: String): String {
        return base64(s.toByteArray())
    }

    fun base64(bytes: ByteArray?): String {
        return Base64.getEncoder().encodeToString(bytes)
    }



//    fun parse

}