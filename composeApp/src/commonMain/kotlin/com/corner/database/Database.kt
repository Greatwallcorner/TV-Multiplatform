@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.corner.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.corner.database.dao.ConfigDao
import com.corner.database.dao.HistoryDao
import com.corner.database.dao.KeepDao
import com.corner.database.dao.SiteDao
import com.corner.database.entity.Config
import com.corner.database.entity.History
import com.corner.database.entity.Keep
import com.corner.database.entity.Site
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.Module

//expect class DriverFactory {
//    fun createDriver(): SqlDriver
//}
//expect fun appModule():Module



object Db:KoinComponent{
    const val SYMBOL = "@@@"
    val database by inject<TvDatabase>()
    val Config:ConfigDao by lazy{ database.getConfigDao() }
    val Site:SiteDao by lazy { database.getSiteDao() }
    val History:HistoryDao by lazy { database.getHistoryDao() }
}

@Database(entities = [Config::class, History::class, Keep::class, Site::class], version = 1)
@ConstructedBy(TvDatabaseConstructor::class)
abstract class TvDatabase: RoomDatabase(){
    abstract fun getConfigDao(): ConfigDao
    abstract fun getHistoryDao(): HistoryDao
    abstract fun getSiteDao(): SiteDao
    abstract fun getKeepDap(): KeepDao

}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TvDatabaseConstructor: RoomDatabaseConstructor<TvDatabase>{
    override fun initialize(): TvDatabase
}

expect val appModule : Module
