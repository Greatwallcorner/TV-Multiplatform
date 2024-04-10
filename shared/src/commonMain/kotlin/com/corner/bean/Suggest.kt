package com.corner.bean

import com.corner.catvodcore.util.Jsons
import kotlinx.serialization.Serializable

/**
@author heatdesert
@date 2024-04-06 11:56
@description
 */

@Serializable
class Suggest {
    var data: List<Data>? = null

    fun isEmpty(): Boolean {
        return data.isNullOrEmpty()
    }

    fun clear(){
        data = null
    }

    @Serializable
    class Data {
        val name: String = ""
    }

    companion object {
        fun objectFrom(str: String): Suggest {
            return Jsons.decodeFromString<Suggest>(str)
        }

        fun get(str: String): List<String> {
            try {
                val items = mutableListOf<String>()
                for (item in objectFrom(str).data!!) items.add(item.name)
                return items
            } catch (e: Exception) {
                return mutableListOf()
            }
        }
    }
}