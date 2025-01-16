package com.github.catvod.net

import org.apache.commons.lang3.StringUtils

class OkResult {
    val code: Int
    val body: String
        get() {
            return if (StringUtils.isEmpty(field)) "" else field
        }
    val resp: Map<String, List<String>>

    constructor() {
        this.code = 500
        this.body = ""
        this.resp = HashMap()
    }

    constructor(code: Int, body: String, resp: Map<String, List<String>>) {
        this.code = code
        this.body = body
        this.resp = resp
    }
}
