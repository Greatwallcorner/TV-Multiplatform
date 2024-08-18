package com.corner.quickjs.method

import com.whl.quickjs.wrapper.*
import java.util.concurrent.Callable

class Function private constructor(
    private val `object`: JSObject,
    private val name: String,
    private val args: Array<Any>
) : Callable<Any?> {
    private var result: Any? = null

    @Throws(Exception::class)
    override fun call(): Any? {
        val function = `object`.getJSFunction(name) ?: return null
        result = function.call(*args)
        if (result is JSObject) then(result!!)
        return result
    }

    private fun then(result: Any) {
        val promise = result as JSObject
        val then = promise.getJSFunction("then")
        then?.call(callback)
    }

    private val callback = JSCallFunction { args -> args[0].also { result = it } }

    companion object {
        fun call(`object`: JSObject, name: String, args: Array<Any>): Function {
            return Function(`object`, name, args)
        }
    }
}
