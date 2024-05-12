package com.corner.database

import app.cash.sqldelight.db.SqlDriver
import com.corner.database.repository.ConfigRepository
import com.corner.database.repository.HistoryRepository
import com.corner.database.repository.SiteRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.Module

expect class DriverFactory {
    fun createDriver(): SqlDriver
}
expect fun appModule():Module



object Db:KoinComponent{
    const val SYMBOL = "@@@"
    val Config:ConfigRepository by inject()
    val Site:SiteRepository by inject()
    val History:HistoryRepository by inject()

}