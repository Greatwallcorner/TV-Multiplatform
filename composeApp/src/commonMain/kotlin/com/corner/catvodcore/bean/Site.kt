package com.corner.catvod.enum.bean

import com.corner.catvodcore.util.ToStringSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Site(
    var key: String,
    var name: String,
    var type: Int,
    var api: String,
    var searchable: Int? = null,
    var changeable: Int? = null,
    var playUrl: String? = null,
    var quickSearch: Int? = null,
    var categories: Set<String> = mutableSetOf(),
    @Serializable(ToStringSerializer::class)
    var ext: String = "",
    var style: Style? = null,
    var timeout: Int? = null,
    var jar: String? = null,
    var header: Map<String, String> = mutableMapOf()
) {
    companion object {
        fun get(key: String, name: String): Site {
            return Site(key, name, -1, "")
        }
    }

    fun toDbSite(configId: Long): com.corner.database.entity.Site {
        return com.corner.database.entity.Site(
            key,
            name,
            searchable?.toLong(),
            changeable?.toLong(),
            recordable = 0,
            configId
        )
    }

    fun isSearchable(): Boolean = searchable == 1

    fun isChangeable(): Boolean = changeable == 1

    fun toggleSearchable() = run {
        searchable = if (searchable == 1) 0
        else 1
    }
}
