package com.corner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Site(
    @PrimaryKey
    val key: String,
    val name: String?,
    val searchable: Long?,
    val changeable: Long?,
    val recordable: Long?,
    val configId: Long?,
)
