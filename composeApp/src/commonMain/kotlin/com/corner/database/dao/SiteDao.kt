package com.corner.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.corner.catvodcore.bean.Api
import com.corner.database.entity.Config
import com.corner.database.entity.Site
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
interface SiteDao{
    @Query("SELECT * FROM Site where `key` = :key")
    suspend fun find(key:String): Site?

    @Query("SELECT * FROM Site where configId = :configId")
    fun findByConfigId(configId: Long): Flow<List<Site>>

    @Update
    suspend fun update(site:Site)

    @Insert
    suspend fun save(sites: List<Site>)

    suspend fun update(cfg:Config, api: Api): MutableSet<com.corner.catvod.enum.bean.Site> {
        val sites = api.sites
        val siteList = findByConfigId(cfg.id).firstOrNull()
        if(siteList.isNullOrEmpty() && api.sites.isNotEmpty()){
            save(api.sites.map { it.toDbSite(configId = cfg.id) })
        }else{
            for (site in sites) {
                val filter = siteList!!.firstOrNull { it.key == site.key }
                filter?.apply {
                    site.searchable = searchable?.toInt()
                    site.changeable = changeable?.toInt()
                }
            }
        }
        return sites
    }
}