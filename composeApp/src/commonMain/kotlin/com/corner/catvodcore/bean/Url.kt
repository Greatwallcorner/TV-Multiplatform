package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

@Serializable
class Url {
    val values: MutableList<Value> = mutableListOf()
    val position = 0
}

fun Url.v():String{
    return if (position >= (values.size)) "" else values.get(position).v ?: ""
}

fun Url.replace(url:String){
    values.get(position).v = url
}

fun Url.add(url:String):Url{
//    if(StringUtils.isBlank(url)) return
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

//object UrlSerialize: KSerializer<Url>{
//    override val descriptor: SerialDescriptor
//        get() = buildClassSerialDescriptor("Url"){
//            element<Value>("values")
//            element<Int>("position")
//        }
//
//    override fun deserialize(decoder: Decoder): Url {
//        decoder.decodeStructure(descriptor){
//            val values = mutableListOf<Value>()
//            val position = 0
//            while(true){
//                when(val index = decodeElementIndex(descriptor)){
//                    0 -> decoder.decode
//                }
//            }
//        }
//    }
//
//    override fun serialize(encoder: Encoder, value: Url) {
//        TODO("Not yet implemented")
//    }
//
//}