package com.corner.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.corner.database.entity.Keep
import kotlinx.coroutines.flow.Flow

@Dao
interface KeepDao{
    @Query("SELECT * FROM Keep")
    fun getAll(): Flow<List<Keep>>
}
//class KeepDao:KoinComponent{
//    private val database: Database by inject()
//    private val keepQueries = database.keepQueries
//
//    fun getAll():List<Keep>{
//        ApiConfig.api.cfg.value?.id ?: return listOf<Keep>()
//        return keepQueries.getAll(ApiConfig.api.cfg.value?.id!!.toLong()).executeAsList()
//    }
//}