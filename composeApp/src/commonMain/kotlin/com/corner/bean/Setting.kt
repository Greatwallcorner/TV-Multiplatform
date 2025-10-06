package com.corner.bean

import ch.qos.logback.classic.Level
import com.corner.bean.enums.PlayerType
import com.corner.catvodcore.util.Jsons
import com.corner.catvodcore.util.Paths
import com.corner.util.M3U8FilterConfig
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.io.path.exists

private val log = LoggerFactory.getLogger("Setting")

@Serializable
data class Setting(val id: String, val label: String, var value: String = "")

/**
 * 以井号分割的字符串
 * #
 */
fun Setting.parseValueToList():List<String>{
    return value?.split("#") ?: listOf()
}

@Serializable
sealed interface Cache{
    fun getName():String

    fun add(t:String)
}

@Serializable
class SearchHistoryCache:Cache{

    private val maxSize:Int = 30

    var searchHistoryList:LinkedHashSet<String> = linkedSetOf()
    override fun getName(): String {
        return "searchHistory"
    }

    override fun add(t:String) {
        if(searchHistoryList.size >= maxSize){
            val list:LinkedHashSet<String> = linkedSetOf()
            list.addAll(searchHistoryList.drop(1))
            searchHistoryList = list
        }
        searchHistoryList.remove(t)
        searchHistoryList.add(t)
    }

    fun getSearchList():List<String>{
        return searchHistoryList.toList().reversed()
    }

    fun remove(query: String) {
        searchHistoryList.remove(query)
    }

}

@Serializable
class PlayerStateCache:Cache{
    private val map:MutableMap<String, String> = mutableMapOf()
    override fun getName(): String {
        return "playerState"
    }

    override fun add(t: String) {

    }

    fun add(key:String, value: String){
        map.put(key,value)
    }

    fun get(key: String):String?{
        return map.get(key)
    }

}

@Serializable
data class SettingFile(val list: MutableList<Setting>, val cache: MutableMap<String, Cache>)

enum class SettingType(val id: String) {
    PLAYER("player"),
    VOD("vod"),
    LOG("log"),
    SEARCHHISTORY("searchHistory"),
    PROXY("proxy"),
    THEME("theme"),
    AD_FILTER("adFilter"),
    M3U8_FILTER_CONFIG("m3u8FilterConfig"),
    CRAWLER_SEARCH_TERMS("crawlerSearchTerms");
}

object SettingStore {
    private val defaultList = listOf(
        Setting("vod", "点播", ""),
        Setting("log", "日志级别", Level.DEBUG.levelStr),
        Setting("player", "播放器", "innie#"),
        Setting("proxy", "代理", "false#"),
        Setting("theme", "主题", "light"),
        Setting("adFilter", "广告过滤", "true"),
        Setting("m3u8FilterConfig", "M3U8 过滤配置", ""),
        Setting("crawlerSearchTerms", "爬虫搜索词", "")
    )

    private var settingFile = SettingFile(mutableListOf<Setting>(), mutableMapOf())

    init {
        getSettingList()
    }
    fun getSettingItem(s: String): String {
        return settingFile.list.find { it.id == s }?.value ?: ""
    }

    fun getSettingItem(type: SettingType):String{
        return getSettingItem(type.id)
    }

    fun getSettingList(): MutableList<Setting> {
        if (settingFile.list.isEmpty()) {
            initSetting()
        }
        return settingFile.list
    }

    fun reset(){
        settingFile = SettingFile(mutableListOf(), mutableMapOf())
        settingFile.list.addAll(defaultList)
        write()
    }

    fun write() {
        try {
            Files.write(Paths.setting(), Jsons.encodeToString(settingFile).toByteArray())
        } catch (e: Exception) {
            // 打印错误日志，方便排查问题
            e.printStackTrace()
        }
    }

    fun setValue(type: SettingType, s: String) {
        settingFile.list.find { i -> i.id == type.id }?.value = s
        write()
    }
    fun doWithCache(func:(MutableMap<String, Cache>) -> Unit){
        func(settingFile.cache)
        write()
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
            return (cache as SearchHistoryCache).getSearchList().toSet()
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

    fun isAdFilterEnabled(): Boolean {
        return getSettingItem(SettingType.AD_FILTER.id).toBoolean()
    }

    fun setAdFilterEnabled(enabled: Boolean) {
        setValue(SettingType.AD_FILTER, enabled.toString())
    }

    fun getM3U8FilterConfig(): M3U8FilterConfig {
        val configJson = getSettingItem(SettingType.M3U8_FILTER_CONFIG)
//        log.debug("获取 M3U8FilterConfig，原始JSON: \n{}", configJson)
        return if (configJson.isBlank()) {
            log.debug("M3U8FilterConfig JSON为空，返回默认配置")
            M3U8FilterConfig()
        } else {
            try {
                val config = Jsons.decodeFromString<M3U8FilterConfig>(configJson)
//                log.debug("成功反序列化 M3U8FilterConfig: {}", config)
                config
            } catch (e: Exception) {
//                log.error("反序列化 M3U8FilterConfig 失败，使用默认配置: ${e.message}")
                M3U8FilterConfig()
            }
        }
    }

    fun setM3U8FilterConfig(config: M3U8FilterConfig) {
        log.debug("保存 M3U8FilterConfig: {}", config)
        val configJson = Jsons.encodeToString(config)
        setValue(SettingType.M3U8_FILTER_CONFIG, configJson)
    }

    fun deleteSearchHistory(query: String) {
        getCache(SettingType.SEARCHHISTORY.id)?.let { cache ->
            (cache as? SearchHistoryCache)?.remove(query)
            write()
        }
    }
}

data class SettingEnable(
    val isEnabled: Boolean,
    val value: String
){
    companion object{
        fun Default():SettingEnable{
            return SettingEnable(false, "")
        }
    }
}

/**
 * boolean#字符串 转换为数组
 */
fun String.parseAsSettingEnable():SettingEnable{
    val split = this.split("#")
    return if(split.size == 1){
        SettingEnable(split.first().toBoolean(), "")
    }else{
        SettingEnable(split.first().toBoolean(), split.last())
    }
}

fun String.getPlayerSetting(sitePlayerType: String? = ""): List<String>{
    val internalPlayer = this.split("#")
    // first is site, second app setting
    val type = if (StringUtils.isNotBlank(
            sitePlayerType
        )
    ) PlayerType.getById(sitePlayerType ?: "").id else internalPlayer.first()
    return listOf(type, internalPlayer[1])
}