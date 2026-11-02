package com.financas.app.domain.model

data class AppUpdateInfo(
    val versionName: String,
    val tagName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSize: Long = 0L,
    val publishedAt: String = "",
    val htmlUrl: String = ""
)
