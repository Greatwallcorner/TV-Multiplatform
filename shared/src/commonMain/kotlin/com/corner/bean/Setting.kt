package com.corner.bean

import ch.qos.logback.classic.Level
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Paths
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import kotlin.io.path.exists

@Serializable
data class Setting(val id: String, val label: String, var value: String?)

@Serializable
sealed interface Cache{
    fun getName():String

    fun add(t:String)
}

@Serializable
class SearchHistoryCache:Cache{

    private val maxSize:Int = 30

    private var searchHistoryList:MutableSet<String> = mutableSetOf()
    override fun getName(): String {
        return "searchHistory"
    }

    override fun add(t:String) {
        if(searchHistoryList.size >= maxSize){
            searchHistoryList = searchHistoryList.drop(1).toMutableSet()
        }
        searchHistoryList.add(t)
    }

    fun getSearchList():Set<String>{
        return searchHistoryList
    }

}

@Serializable
data class SettingFile(val list: MutableList<Setting>, val cache: MutableMap<String, Cache>)

enum class EditType {
    INPUT,
    CHOOSE
}

enum class SettingType(val id: String) {
    PLAYER("player"),
    VOD("vod"),
    LOG("log"),
    SEARCHHISTORY("searchHistory")
}

object SettingStore {
    private val defaultList = listOf(
        Setting("vod", "点播", ""),
        Setting("player", "外部播放器", ""),
        Setting("log", "日志级别", Level.DEBUG.levelStr)
    )

    private var settingFile = SettingFile(mutableListOf<Setting>(), mutableMapOf())

    init {
        getSettingList()
    }
    fun getSettingItem(s: String): String {
        return settingFile.list.find { it.id == s }?.value ?: ""
    }

    fun getSettingList(): MutableList<Setting> {
        if (settingFile.list.isEmpty()) {
            initSetting()
        }
        return settingFile.list
    }

    fun write() {
        Files.write(Paths.setting(), Jsons.encodeToString(settingFile).toByteArray())
    }

    fun setValue(type: SettingType, s: String) {
        settingFile.list.find { i -> i.id == type.id }?.value = s
        write()
    }
    
    fun setCache(name:String, value: String){
        settingFile.cache[name]?.add(value)
    }

    fun getCache(name:String): Cache? {
        return settingFile.cache[name]
    }

    private fun initSetting() {
        val file = Paths.setting()
        if (file.exists() && settingFile.list.size == 0) {
            settingFile = Jsons.decodeFromString<SettingFile>(Files.readString(file))
            if (settingFile.list.size != defaultList.size) {
                defaultList.forEach { setting ->
                    if (settingFile.list.find { setting.id == it.id } == null) {
                        settingFile.list.add(setting)
                    }
                }
            }
        }
        if (settingFile.list.size == 0) {
            settingFile.list.addAll(defaultList)
            Files.write(file, Jsons.encodeToString(settingFile).toByteArray())
        }
    }

    fun getHistoryList(): Set<String> {
        if (settingFile.list.isEmpty()) {
            initSetting()
        }
        val cache = getCache(SettingType.SEARCHHISTORY.id)
        if (cache != null) {
            return (cache as SearchHistoryCache).getSearchList()
        }
        return setOf()
    }

    fun addSearchHistory(s: String){
        val cache = getCache(SettingType.SEARCHHISTORY.id)
        if(cache == null) settingFile.cache[SettingType.SEARCHHISTORY.id] = SearchHistoryCache()
        if(s.trim().isNotBlank()){
            getCache(SettingType.SEARCHHISTORY.id)!!.add(s)
            write()
        }
    }
}
