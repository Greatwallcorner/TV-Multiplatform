package com.corner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Site(
    val key: String = "",
    val name: String?,
    val searchable: Long?,
    val changeable: Long?,
    val recordable: Long?,
    val configId: Long?,
){
    @PrimaryKey(autoGenerate = true)
    var id:Int = 0
}
