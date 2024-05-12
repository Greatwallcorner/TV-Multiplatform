package com.corner.database.repository

import com.corner.database.Database
import com.corner.database.Site
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface SiteRepository {
    fun find(key:String): Site?
}

class SiteRepositoryImpl:SiteRepository, KoinComponent{
    private val database: Database by inject()
    private val siteQueries = database.siteQueries
    override fun find(key: String):Site? {
        return siteQueries.find(key).executeAsOneOrNull()
    }

}