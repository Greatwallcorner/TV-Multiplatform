package com.corner.quickjs.method

import com.whl.quickjs.wrapper.JSCallFunction
import com.whl.quickjs.wrapper.JSObject
import java.util.concurrent.CompletableFuture

class Async private constructor() {
    private val future = CompletableFuture<Any?>()

    private fun call(`object`: JSObject, name: String, args: Array<Any>): CompletableFuture<Any?> {
        val function = `object`.getJSFunction(name) ?: return empty()
        val result = function.call(*args)
        if (result is JSObject) then(result)
        else future.complete(result)
        return future
    }

    private fun empty(): CompletableFuture<Any?> {
        future.complete(null)
        return future
    }

    private fun then(result: Any) {
        val promise = result as JSObject
        val then = promise.getJSFunction("then")
        then?.call(callback)
    }

    private val callback = JSCallFunction { args ->
        future.complete(args[0])
        null
    }

    companion object {
        fun run(`object`: JSObject, name: String, args: Array<Any>): CompletableFuture<Any?> {
            return Async().call(`object`, name, args)
        }
    }
}
