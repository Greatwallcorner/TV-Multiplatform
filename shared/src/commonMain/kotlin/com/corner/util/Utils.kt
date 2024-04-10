package com.corner.util

import io.ktor.util.*
import kotlinx.coroutines.Job

object Utils {
    fun <E> MutableList<E>.addIfAbsent(index:Int, element: E){
        if(!this.contains(element)){
            this.add(index, element)
        }
    }

    fun MutableList<Job>.cancelAll():MutableList<Job>{
        this.forEach{it.cancel()}
        return this
    }

    fun StringValues.toSingleValueMap(): Map<String, String> =
        entries().associateByTo(LinkedHashMap(), { it.key }, { it.value.toList()[0] })
}