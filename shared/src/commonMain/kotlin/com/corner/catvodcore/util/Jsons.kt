package com.corner.catvodcore.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

val Jsons = Json { ignoreUnknownKeys = true }

object ToStringSerializer: JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonPrimitive(element.toString().apply { this.trim('"') })
    }
}

object JsonStrToMapSerializer: JsonTransformingSerializer<Map<String, String>>(MapSerializer(String.serializer(),String.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        println(element.jsonPrimitive.content)
        return Jsons.parseToJsonElement(element.jsonPrimitive.content)
    }

}

