package com.corner.quickjs.method

import org.mozilla.javascript.Context
import org.mozilla.javascript.FunctionObject
import org.mozilla.javascript.ScriptableObject
import java.util.concurrent.CompletableFuture

class Async private constructor() {
    private val future = CompletableFuture<Any?>()

    private fun call(name: String, args: Array<Any>): CompletableFuture<Any?> {
        val result = FunctionObject.callMethod(obj, name, args)
//        val function = obj.getJSFunction(name) ?: return empty()
//        val result = function.call(*args)
        if (result is ScriptableObject) then(result)
        else future.complete(result)
        return future
    }

    private fun empty(): CompletableFuture<Any?> {
        future.complete(null)
        return future
    }

    private fun then(result: Any) {
        val promise = result as ScriptableObject
        val then = FunctionObject.callMethod(promise, "then", arrayOf(Context.javaToJS(Async::class.java.getMethod("callback"), promise)))
//        val then = promise.getJSFunction("then")
//        then?.call(callback)
    }

    fun callback(args:Array<Any>){
        future.complete(args[0])
    }
//    private val callback = Function { args ->
//        future.complete(args[0])
//        null
//    }

    companion object {
        fun run(name: String, args: Array<Any>): CompletableFuture<Any?> {
            return Async().call(name, args)
        }
    }
}
