package com.financas.app.ui.i18n

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val locale: Locale
) {
    EN("en", "English", "🇬🇧", Locale.ENGLISH),
    PT("pt", "Português", "🇵🇹", Locale("pt", "PT"));

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}
