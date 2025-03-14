package com.corner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Keep(
    @PrimaryKey
    val key: String,
    val siteName: String?,
    val vodName: String?,
    val vodPic: String?,
    val createTime: Long,
    val type: Long,
    val cid: Long,
)
