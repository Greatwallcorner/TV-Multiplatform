package com.corner.util

object Updater {

}

data class LatestVersion(
    val channel: String,
    val releaseTime: String,
    val version: String,
    val changeLog: String,
    val description:String,
    val downloadUrl: String,
    val hash: String,
)