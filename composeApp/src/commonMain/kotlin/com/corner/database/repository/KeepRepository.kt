package com.corner.database.repository

import com.corner.catvodcore.config.ApiConfig
import com.corner.database.Database
import com.corner.database.Keep
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class KeepRepository:KoinComponent{
    private val database: Database by inject()
    private val keepQueries = database.keepQueries

    fun getAll():List<Keep>{
        ApiConfig.api.cfg.value?.id ?: return listOf<Keep>()
        return keepQueries.getAll(ApiConfig.api.cfg.value?.id!!.toLong()).executeAsList()
    }
}