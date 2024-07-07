package com.corner.util

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import com.corner.catvod.enum.bean.Site
import io.ktor.util.*
import kotlinx.coroutines.Job
import java.util.concurrent.TimeUnit

fun Site.isEmpty():Boolean{
    return key.isEmpty() || name.isEmpty()
}

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


@Composable
fun LazyGridState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }

    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

fun Long.formatTimestamp(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours)
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}