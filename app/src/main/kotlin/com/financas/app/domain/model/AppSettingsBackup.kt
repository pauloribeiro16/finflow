package com.financas.app.domain.model

data class AppSettingsBackup(
    val userName: String = "",
    val defaultCurrency: String = "EUR (€)",
    val monthlyCycleDay: Int = 1,
    val themeMode: String = "SYSTEM",
    val appLanguage: String = "EN",
    val biometricsEnabled: Boolean = false,
    val notificationCaptureEnabled: Boolean = true,
    val customContextTags: List<String> = emptyList(),
    val profilePhotoBase64: String? = null
)
