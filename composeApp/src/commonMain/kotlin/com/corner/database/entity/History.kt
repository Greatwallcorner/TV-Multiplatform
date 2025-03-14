package com.corner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class History(
    @PrimaryKey
    val key: String = "",
    val vodPic: String? = null,
    val vodName: String? = null,
    val vodFlag: String? = null,
    val vodRemarks: String? = null,
    val episodeUrl: String? = null,
    val revSort: Long? = null,
    val revPlay: Long? = null,
    val createTime: Long? = null,
    val opening: Long? = null,
    val ending: Long? = null,
    val position: Long? = null,
    val duration: Long? = null,
    val speed: Double? = null,
    val player: Long? = null,
    val scale: Long? = null,
    val cid: Long? = null,
)
