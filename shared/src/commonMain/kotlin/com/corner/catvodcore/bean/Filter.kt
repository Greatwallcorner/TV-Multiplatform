package com.corner.catvodcore.bean

import kotlinx.serialization.Serializable

@Serializable
class Filter {
    private val key: String? = null

    private val name: String? = null

    private val init: String? = null

    private val value: List<Value>? = null
}

// MutableMap<String, List<Filter>>
//object FiltersSerialize: JsonTransformingSerializer<MutableMap<String, List<Filter>>>(serializer()) {
//    override fun transformDeserialize(element: JsonElement): JsonElement {
//        return buildJsonObject {
//            if(element is JsonObject) {
//                val jsonObject = element.jsonObject
//                for (key in jsonObject.keys) {
//                    val jsonElement = jsonObject[key]
//                    put(key, Jsons.)
//                    if()
//                }
//            }
//        }
//    }
//}