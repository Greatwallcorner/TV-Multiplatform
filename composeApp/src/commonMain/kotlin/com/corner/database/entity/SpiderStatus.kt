package com.corner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spider_status")
data class SpiderStatus(
    @PrimaryKey val siteKey: String,
    val status: String, // 可用状态: AVAILABLE, UNAVAILABLE, UNKNOWN
    val lastTested: Long = System.currentTimeMillis() // 上次测试时间戳
)