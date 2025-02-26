package com.corner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Config(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: Long,
    val time: Long = System.currentTimeMillis(),
    val url: String? = null,
    val json: String? = null,
    val name: String? = null,
    val home: String? = null,
    val parse: String? = null,
)
