package com.corner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spider_status")
data class SpiderStatus(
    @PrimaryKey val siteKey: String,
    val status: String,
    val lastTested: Long = System.currentTimeMillis()
)