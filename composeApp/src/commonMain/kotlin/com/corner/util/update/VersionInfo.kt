package com.corner.util.update

import kotlinx.serialization.Serializable

@Serializable
data class VersionInfo(
    val version: String,
    val windows: PlatformInfo? = null,
    val linux: PlatformInfo? = null,
    val mac: PlatformInfo? = null
)

@Serializable
data class PlatformInfo(
    val download_url: String,
    val msi_url: String? = null,
    val deb_url: String? = null,
    val dmg_url: String? = null
)
