package com.corner.catvodcore.util

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

val Jsons = Json {
    ignoreUnknownKeys = true
    isLenient = true
}


object ToStringSerializer: JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val s = element.toString().trim({c -> c == '"' })
        return JsonPrimitive(s)
    }
}

object JsonStrToMapSerializer: JsonTransformingSerializer<Map<String, String>>(MapSerializer(String.serializer(),String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        println(element.jsonPrimitive.content)
        return Jsons.parseToJsonElement(element.jsonPrimitive.content)
    }

}

