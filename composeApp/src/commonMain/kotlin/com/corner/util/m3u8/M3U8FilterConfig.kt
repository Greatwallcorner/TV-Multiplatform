package com.corner.util.m3u8
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class M3U8FilterConfig(
    @EncodeDefault
    var tsNameLenExtend: Int = 1,
    @EncodeDefault
    var theExtinfBenchmarkN: Int = 5,
    @EncodeDefault
    var violentFilterModeFlag: Boolean = false
)