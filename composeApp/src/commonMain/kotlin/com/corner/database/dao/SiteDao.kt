package com.corner.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.corner.database.entity.Site

@Dao
interface SiteDao{
    @Query("SELECT * FROM Site where `key` = :key")
    suspend fun find(key:String): Site?

    @Update
    suspend fun sync(site:Site)
}

//interface SiteRepository {
//    fun find(key:String): Site?
//    fun sync(cfg: Config, api: Api)
//}
//
//class SiteRepositoryImpl:SiteRepository, KoinComponent{
//    private val database: Database by inject()
//    private val siteQueries = database.siteQueries
//
//    override fun find(key: String):Site? {
//        return siteQueries.find(key).executeAsOneOrNull()
//    }
//
//    override fun sync(cfg: Config, api: Api) {
////        siteQueries.getAllById(cfg.id)
//    }
//
//}