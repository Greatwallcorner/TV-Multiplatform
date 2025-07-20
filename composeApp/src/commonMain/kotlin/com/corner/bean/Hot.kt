package com.corner.bean

import SiteViewModel
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.viewmodel.GlobalAppState
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Response
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Hot")

@Serializable
data class Hot(val data: List<HotData>) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun getHotList() {
            SiteViewModel.viewModelScope.launch {
                try {
                    Http.Get(
                        "https://api.web.360kan.com/v1/rank?cat=1",
                        headers = mapOf(HttpHeaders.Referrer to "https://www.360kan.com/rank/general").toHeaders()
                    ).execute().use { response ->
                        if (response.isSuccessful) {
                            GlobalAppState.hotList.value = Jsons.decodeFromStream<Hot>(
                                response.body!!.byteStream()
                            ).data
                        }
                    }
                } catch (e: Exception) {
                    log.error("请求热搜失败", e)
                }
            }
        }
    }
}

@Serializable
data class HotData(val title: String, val comment: String, val upinfo: String, val description: String)