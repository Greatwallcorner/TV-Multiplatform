package com.corner.util

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import cn.hutool.core.util.CharUtil
import com.corner.catvod.enum.bean.Site
import io.ktor.util.*
import kotlinx.coroutines.Job
import org.apache.commons.lang3.StringUtils
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

fun String.deleteInvisibleChar():String{
    val str = this
    if (StringUtils.isEmpty(str)) {
        return str
    }
    val sz: Int = str.length
    val chs = CharArray(sz)
    var count = 0
    for (i in 0 until sz) {
        if (!CharUtil.isBlankChar(str[i])) {
            chs[count++] = str[i]
        }
    }
    if (count == sz) {
        return str
    }
    if (count == 0) {
        return StringUtils.EMPTY
    }
    return String(chs, 0, count)
}

fun String.trimBlankChar():String{
    return this.trim{CharUtil.isBlankChar(it)}
}

var Color.Companion.FirefoxGray:Color
    get() = Color(59, 59, 60)
    set(value) {}