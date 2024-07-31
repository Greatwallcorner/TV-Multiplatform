package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

@Serializable
data class Url (
    val values: MutableList<Value> = mutableListOf(),
    val position:Int = 0
)

fun Url.v():String{
    return if (position >= (values.size)) "" else values[position].v ?: ""
}

fun Url.replace(url:String){
    values[position].v = url
}

fun Url.isEmpty():Boolean{
    return values.isEmpty() || values.filter { it.valueIsEmpty() }.size == values.size
}

fun Url.add(url:String):Url{
    values.add(Value("",url))
    return this
}

object UrlSerializable: JsonTransformingSerializer<Url>(serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if(element is JsonArray){
            return buildJsonObject {
                putJsonArray("values"){
                    for (i in 0 until element.jsonArray.size step 2) {
                        addJsonObject {
                            put("n", element.jsonArray[i].jsonPrimitive.content)
                            put("v", element.jsonArray[i+1].jsonPrimitive.content)
                        }
                    }
                }
            }
        }else{
            return buildJsonObject {
                putJsonArray("values"){
                    addJsonObject {
                        put("n", "")
                        put("v", element.jsonPrimitive.content)
                    }
                }
            }
        }
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        return super.transformSerialize(element)
    }
}