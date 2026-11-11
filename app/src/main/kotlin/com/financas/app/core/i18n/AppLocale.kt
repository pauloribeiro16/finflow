package com.financas.app.core.i18n

object AppLocale {
    @Volatile
    var isEnglish: Boolean = false
}

fun tr(pt: String, en: String): String = if (AppLocale.isEnglish) en else pt
