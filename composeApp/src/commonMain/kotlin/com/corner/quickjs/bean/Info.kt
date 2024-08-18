package com.corner.quickjs.bean

class Info(var rule: String) {
    var index: Int = 0
    var excludes: MutableList<String>? = null

//    fun setRule(rule: String) {
//        this.rule = rule
//    }

    fun setInfo(pos: String) {
        var pos = pos
        if (rule.contains("--")) {
            val rules = rule.split("--".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            setExcludes(rules)
            rule = rules[0]
//            setRule(rules[0])
        } else if (pos.contains("--")) {
            val rules = pos.split("--".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            setExcludes(rules)
            pos = rules[0]
        }
        index = try {
            pos.replace("eq(", "").replace(")", "").toInt()
        } catch (ignored: Exception) {
            0
        }
    }

    fun setExcludes(rules: Array<String>) {
        excludes = mutableListOf<String>().apply { addAll(rules) }
        excludes!!.removeAt(0)
    }
}
