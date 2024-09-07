//package com.corner.quickjs.method
//
//import org.mozilla.javascript.Context
//import org.mozilla.javascript.FunctionObject
//import org.mozilla.javascript.Scriptable
//import org.mozilla.javascript.ScriptableObject
//import java.util.concurrent.Callable
//
//class Function private constructor(
//    private val obj: Scriptable,
//    private val name: String,
//    private val args: Array<Any>
//) : Callable<Any?> {
//    private var result: Any? = null
//
//    @Throws(Exception::class)
//    override fun call(): Any? {
//        result = FunctionObject.callMethod(obj, name, args)
//        if (result is ScriptableObject) then(result!!)
//        return result
//    }
//
//    private fun then(result: Any) {
//        FunctionObject.callMethod(result as Scriptable, "then",
//            arrayOf(Context.javaToJS(Function::class.java.getMethod("callback"), obj)))
//    }
//
//    fun callback(args:Array<Any>){
//        args[0].also { result = it }
//    }
//
//    companion object {
//        fun call(obj: ScriptableObject, name: String, args: Array<Any>): Function {
//            return Function(obj, name, args)
//        }
//    }
//}
