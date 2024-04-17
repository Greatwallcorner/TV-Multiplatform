package com.corner.bean

import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.KtorClient
import com.corner.catvodcore.viewmodel.GlobalModel
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class Hot(val data:List<HotData>) {
    companion object{
        @OptIn(ExperimentalSerializationApi::class)
        public fun getHotList() {
            SiteViewModel.viewModelScope.launch {
                val response = KtorClient.client.get("https://api.web.360kan.com/v1/rank?cat=1") {
                    headers {
                        set(HttpHeaders.Referrer, "https://www.360kan.com/rank/general")
                    }
                }

                if(response.status == HttpStatusCode.OK)
                    GlobalModel.hotList.value = Jsons.decodeFromStream<Hot>(response.bodyAsChannel().toInputStream()).data
//                    GlobalModel.hotList.update { it. = Jsons.decodeFromStream<Hot>(response.bodyAsChannel().toInputStream()).data}
//                    GlobalModel.hotList.value.addAll()
            }
        }
    }
}

@Serializable
data class HotData(val title:String, val comment:String, val upinfo:String, val description:String)