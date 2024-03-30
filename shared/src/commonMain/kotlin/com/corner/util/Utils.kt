package com.corner.util

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
}