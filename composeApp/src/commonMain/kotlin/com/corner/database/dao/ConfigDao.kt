package com.corner.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.corner.database.entity.Config
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Insert
    suspend fun save(config: Config)

    @Query("SELECT * FROM Config where url = :url and type = :type")
    fun find(url: String, type: Long): Flow<Config?>

    @Query("SELECT * FROM Config WHERE `type` = :type ORDER BY time DESC LIMIT 1")
    suspend fun findOneByType(type: Long): Config?

    @Update
    suspend fun update(config: Config)

    @Query("UPDATE Config SET json = :json, home = :home, parse = :parse where id = :id ")
    suspend fun updateSome( id:Int, json: String? = null, home: String? = null, parse: String? = null)

    @Query("UPDATE Config SET url = :textFieldValue where id = :id ")
    suspend fun updateUrl(id: Long, textFieldValue: String)

    @Query("UPDATE Config SET home = :key where type = :type and url = :url ")
    suspend fun setHome(url: String?, type: Int, key: String)

    @Query("SELECT * from Config")
    fun getAll(): Flow<List<Config>>

    @Query("DELETE FROM Config where id = :id")
    suspend fun deleteById(id: Long?):Int
}

//class ConfigRepositoryImpl : ConfigDao, KoinComponent {
//    private val database: Database by inject()
//    private val configQueries = database.configQueries
//    override fun save(
//        type: Long,
//        url: String?,
//        json: String?,
//        name: String?,
//        home: String?,
//        parse: String?
//    ) {
//        configQueries.save(type, url, json, name, home, parse)
//    }
//
//    override fun find(url: String, type: Long): Config? {
//        return configQueries.find(type, url).executeAsOneOrNull()
//    }
//
//    override fun findOneByType(type: Long): Config? {
//        return configQueries.selectOneByType(type).executeAsOneOrNull()
//    }
//
//    override fun update(config: Config) {
//        return configQueries.update(
//            config.type,
//            config.url,
//            config.json,
//            config.name,
//            config.home,
//            config.parse,
//            config.id
//        )
//    }
//
//    override fun updateSome(id: Int, json: String?, home: String?, parse: String?) {
//        configQueries.updateOne(json, home, parse, id.toLong())
//    }
//
//    override fun updateUrl(id: Long, textFieldValue: String) {
//        configQueries.updateUrl(textFieldValue, id)
//    }
//
//    override fun setHome(url: String?, type: Int, key: String) {
//        configQueries.setHome(key, type.toLong(), url)
//    }
//
//    override fun getAll(): List<Config> {
//        return configQueries.getAll().executeAsList()
//    }
//
//    override fun deleteById(id: Long?) {
//        id ?: return
//        configQueries.deleteById(id)
//    }
//
//}
