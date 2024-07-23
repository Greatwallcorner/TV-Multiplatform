package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Filter
import com.corner.catvodcore.bean.Result
import com.corner.catvodcore.bean.Type
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger

interface VideoComponent {
    var log:Logger

    var model:MutableValue<Model>

    fun homeLoad()

    fun loadMore()

    fun clickFolder(vod:Vod)

    fun chooseCate(cate: String)

    fun clear()

    data class Model(
        var homeVodResult: MutableSet<Vod> = mutableSetOf(),
        var homeLoaded: Boolean = false,
        var classList: MutableSet<Type> = mutableSetOf(),
        var filtersMap: MutableMap<String, List<Filter>> = mutableMapOf(),
        var currentClass: Type? = null,
        var currentFilters: List<Filter> = listOf(),
        var page: AtomicInteger = AtomicInteger(1),
        var isRunning: Boolean = false,
        val prompt:String = ""
    ){

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Model

            if (homeVodResult != other.homeVodResult) return false
            if (homeLoaded != other.homeLoaded) return false
            if (classList != other.classList) return false
            if (currentClass != other.currentClass) return false
            if (currentFilters != other.currentFilters) return false
            if (filtersMap != other.filtersMap) return false
            if (page != other.page) return false
            if (isRunning != other.isRunning) return false
            if (prompt != other.prompt) return false

            return true
        }

        override fun hashCode(): Int {
            var result = 0
            homeVodResult.forEach{result = 31 * result + it.hashCode()}
            result = 31 * result + homeLoaded.hashCode()
            result = 31 * result + classList.hashCode()
//            classList.forEach{ result = 31 * result + it.hashCode()}
            result = 31 * result + currentFilters.hashCode()
            result = 31 * result + filtersMap.hashCode()
            result = 31 * result + (currentClass?.hashCode() ?: 0)
            result = 31 * result + page.hashCode()
            result = 31 * result + isRunning.hashCode()
            result = 31 * result + prompt.hashCode()
            return result
        }


    }

    fun loadCate(cate: String): Result
}