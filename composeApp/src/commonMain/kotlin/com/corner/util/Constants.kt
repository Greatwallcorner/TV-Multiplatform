package com.corner.util

import androidx.compose.ui.graphics.Color

object Constants {
    val EpSize: Int = 15

    val lightBlue = Color(94, 181, 247)
    val darkBlue = Color(14, 22, 33)

    val resPathKey = "compose.application.resources.dir"

    val ChromeUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    val header: Map<String, String> = mapOf(
        "Accept" to "application/json, text/javascript, */*; q=0.01",
        "Accept-Encoding" to "gzip, deflate, br, zstd",
        "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6",
        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "DNT" to "1",
        "Origin" to "https://hhjx.hhplayer.com  ",
        "Priority" to "u=1, i",
        "Sec-Ch-Ua" to "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"",
        "Sec-Ch-Ua-Mobile" to "?0",
        "Sec-Ch-Ua-Platform" to "\"Windows\"",
        "Sec-Fetch-Dest" to "empty",
        "Sec-Fetch-Mode" to "cors",
        "Sec-Fetch-Site" to "same-origin",
        "Sec-Fetch-Storage-Access" to "active",
        "Sec-Gpc" to "1",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0",
        "X-Requested-With" to "XMLHttpRequest"
    )
}