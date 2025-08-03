package com.corner.util
import kotlinx.serialization.Serializable
@Serializable
data class M3U8FilterConfig(
    var tsNameLenExtend: Int = 1,
    var theExtinfBenchmarkN: Int = 5,
    var violentFilterModeFlag: Boolean = false
)