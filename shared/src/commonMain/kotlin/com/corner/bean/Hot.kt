package com.corner.bean

import SiteViewModel
import com.corner.catvodcore.util.Http
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.viewmodel.GlobalModel
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Headers.Companion.toHeaders

@Serializable
data class Hot(val data:List<HotData>) {
    companion object{
        @OptIn(ExperimentalSerializationApi::class)
        public fun getHotList() {
            SiteViewModel.viewModelScope.launch {
                val response = Http.Get("https://api.web.360kan.com/v1/rank?cat=1", headers = mapOf(HttpHeaders.Referrer to "https://www.360kan.com/rank/general").toHeaders()).execute()
//                val response = KtorClient.client.get("https://api.web.360kan.com/v1/rank?cat=1") {
//                    headers {
//                        set(HttpHeaders.Referrer, "https://www.360kan.com/rank/general")
//                    }
//                }

                if(response.isSuccessful)
                    GlobalModel.hotList.value = Jsons.decodeFromStream<Hot>(response.body.byteStream()).data
//                    GlobalModel.hotList.update { it. = Jsons.decodeFromStream<Hot>(response.bodyAsChannel().toInputStream()).data}
//                    GlobalModel.hotList.value.addAll()
            }
        }
    }
}

@Serializable
data class HotData(val title:String, val comment:String, val upinfo:String, val description:String)