package com.corner.ui.decompose

import com.arkivanov.decompose.value.MutableValue
import com.corner.catvod.enum.bean.Site
import com.corner.catvod.enum.bean.Vod
import com.corner.catvodcore.bean.Type
import com.corner.catvodcore.config.api
import org.slf4j.Logger

interface VideoComponent {
    var log:Logger

    var model:MutableValue<Model>

    fun homeLoad()

    data class Model(
        var homeVodResult: MutableSet<Vod>? = null,
        var homeLoaded: Boolean = false,
        var classList: MutableSet<Type> = mutableSetOf(),
        var currentClass: Type? = null,
        var page: Int = 1,
        var home: Site? = null,
        val isRunning: Boolean = false,
    ){
//        fun update(action:(Model)->Model){
//            action(this)
//        }

        fun getHomeSite():Site?{
            if(home == null){
                home = api?.home?.value
            }
            return home
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Model

            if (homeVodResult != other.homeVodResult) return false
            if (homeLoaded != other.homeLoaded) return false
            if (classList != other.classList) return false
            if (currentClass != other.currentClass) return false
            if (page != other.page) return false
            if (home != other.home) return false
            if (isRunning != other.isRunning) return false

            return true
        }

        override fun hashCode(): Int {
            var result = 0
            homeVodResult?.forEach{result = 31 * result + it.hashCode()}
            result = 31 * result + homeLoaded.hashCode()
            result = 31 * result + classList.hashCode()
            classList.forEach{ result = 31 * result + it.hashCode()}
            result = 31 * result + (currentClass?.hashCode() ?: 0)
            result = 31 * result + page
            result = 31 * result + (home?.hashCode() ?: 0)
            result = 31 * result + isRunning.hashCode()
            return result
        }


    }
}