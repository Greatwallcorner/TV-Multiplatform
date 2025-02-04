package com.corner.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.corner.catvodcore.util.Paths
import com.corner.database.repository.*
import org.koin.dsl.module

actual class DriverFactory {
//    var database: Database = Database(createDriver())
    actual fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(Paths.db()).also {
            Database.Schema.create(it)
//            Database.Schema.migrate(it)
        }

    }
}

actual fun appModule() = module {
    single<Database> {
        Database(DriverFactory().createDriver())
    }
    single<ConfigRepository> {
        ConfigRepositoryImpl()
    }

    single<SiteRepository> {
        SiteRepositoryImpl()
    }
    single<HistoryRepository> {
        HistoryRepositoryImpl()
    }
}
