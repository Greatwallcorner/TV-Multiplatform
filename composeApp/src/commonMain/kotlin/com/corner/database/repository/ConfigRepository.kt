package com.corner.database.repository

import com.corner.database.Config
import com.corner.database.Database
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ConfigRepository {
    fun save(
        type: Long,
        url: String? = null,
        json: String? = null,
        name: String? = null,
        home: String? = null,
        parse: String? = null
    )

    fun find(url: String, type: Long): Config?

    fun findOneByType(type: Long): Config?

    fun update(config: Config)

    fun updateSome( id:Int, json: String? = null, home: String? = null, parse: String? = null)
    fun updateUrl(id: Long, textFieldValue: String)
    fun setHome(url: String?, type: Int, key: String)
    fun getAll(): List<Config>
    abstract fun deleteById(id: Long?)
}

class ConfigRepositoryImpl : ConfigRepository, KoinComponent {
    private val database: Database by inject()
    private val configQueries = database.configQueries
    override fun save(
        type: Long,
        url: String?,
        json: String?,
        name: String?,
        home: String?,
        parse: String?
    ) {
        configQueries.save(type, url, json, name, home, parse)
    }

    override fun find(url: String, type: Long): Config? {
        return configQueries.find(type, url).executeAsOneOrNull()
    }

    override fun findOneByType(type: Long): Config? {
        return configQueries.selectOneByType(type).executeAsOneOrNull()
    }

    override fun update(config: Config) {
        return configQueries.update(
            config.type,
            config.url,
            config.json,
            config.name,
            config.home,
            config.parse,
            config.id
        )
    }

    override fun updateSome(id: Int, json: String?, home: String?, parse: String?) {
        configQueries.updateOne(json, home, parse, id.toLong())
    }

    override fun updateUrl(id: Long, textFieldValue: String) {
        configQueries.updateUrl(textFieldValue, id)
    }

    override fun setHome(url: String?, type: Int, key: String) {
        configQueries.setHome(key, type.toLong(), url)
    }

    override fun getAll(): List<Config> {
        return configQueries.getAll().executeAsList()
    }

    override fun deleteById(id: Long?) {
        id ?: return
        configQueries.deleteById(id)
    }

}
