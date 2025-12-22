package com.corner.database.dao

import androidx.room.*
import com.corner.database.entity.SpiderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SpiderStatusDao {
    @Query("SELECT * FROM spider_status")
    fun getAll(): Flow<List<SpiderStatus>>

    @Query("SELECT * FROM spider_status WHERE siteKey = :siteKey")
    suspend fun getBySiteKey(siteKey: String): SpiderStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: SpiderStatus)

    @Update
    suspend fun update(status: SpiderStatus)

    @Delete
    suspend fun delete(status: SpiderStatus)

    @Query("DELETE FROM spider_status")
    suspend fun clearAll()
}