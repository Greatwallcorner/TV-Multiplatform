package com.corner.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.corner.catvodcore.util.Paths
import org.koin.dsl.module

actual val appModule = module {
    single<TvDatabase> {
        Room.databaseBuilder<TvDatabase>(Paths.db())
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver()).build()
    }
}



