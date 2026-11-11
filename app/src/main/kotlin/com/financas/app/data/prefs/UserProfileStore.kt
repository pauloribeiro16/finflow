package com.financas.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserProfileStore {
    fun getUserName(): String
    fun setUserName(name: String)
    fun getProfilePhotoUri(): String?
    fun setProfilePhotoUri(uri: String?)
    val profilePhotoFlow: StateFlow<String?>
    val userNameFlow: StateFlow<String>
    fun getDefaultCurrency(): String
    fun setDefaultCurrency(currency: String)
    fun getMonthlyCycleDay(): Int
    fun setMonthlyCycleDay(day: Int)
    fun getThemeMode(): String // "SYSTEM", "LIGHT", "DARK"
    fun setThemeMode(mode: String)
    val themeModeFlow: StateFlow<String>
    fun getAppLanguage(): String // "EN", "PT"
    fun setAppLanguage(lang: String)
    val appLanguageFlow: StateFlow<String>
    fun isPrivacyModeEnabled(): Boolean = false
    fun setPrivacyModeEnabled(enabled: Boolean) {}
    val privacyModeFlow: StateFlow<Boolean>
    fun isAutoUpdateCheckEnabled(): Boolean = true
    fun setAutoUpdateCheckEnabled(enabled: Boolean) {}
    fun isMarketQuotesAutoSyncEnabled(): Boolean = false
    fun setMarketQuotesAutoSyncEnabled(enabled: Boolean) {}
    fun isMarketQuotesDisclosureAccepted(): Boolean = false
    fun setMarketQuotesDisclosureAccepted(accepted: Boolean) {}
}

class SharedPrefsUserProfileStore(
    private val context: Context
) : UserProfileStore {
    private val prefs = context.getSharedPreferences("financas_user_profile", Context.MODE_PRIVATE)

    private val _userNameFlow = MutableStateFlow(getUserName())
    override val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _profilePhotoFlow = MutableStateFlow(getProfilePhotoUri())
    override val profilePhotoFlow: StateFlow<String?> = _profilePhotoFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    override val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _appLanguageFlow = MutableStateFlow(getAppLanguage())
    override val appLanguageFlow: StateFlow<String> = _appLanguageFlow.asStateFlow()

    private val _privacyModeFlow = MutableStateFlow(isPrivacyModeEnabled())
    override val privacyModeFlow: StateFlow<Boolean> = _privacyModeFlow.asStateFlow()

    init {
        com.financas.app.domain.model.MoneyCents.isPrivacyModeEnabled = isPrivacyModeEnabled()
        com.financas.app.core.i18n.AppLocale.isEnglish = getAppLanguage() == "EN"
    }

    override fun getUserName(): String =
        prefs.getString("user_name", "") ?: ""

    override fun setUserName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString("user_name", trimmed).apply()
        _userNameFlow.value = trimmed
    }

    override fun getProfilePhotoUri(): String? {
        val localFile = context.filesDir.resolve("profile_photo.png")
        if (localFile.exists() && localFile.length() > 0) {
            return localFile.absolutePath
        }
        val saved = prefs.getString("profile_photo_uri", null) ?: return null
        if (saved.startsWith("content://")) {
            prefs.edit().remove("profile_photo_uri").apply()
            return null
        }
        return saved
    }

    override fun setProfilePhotoUri(uri: String?) {
        if (uri == null) {
            prefs.edit().remove("profile_photo_uri").apply()
        } else {
            prefs.edit().putString("profile_photo_uri", uri).apply()
        }
        _profilePhotoFlow.value = uri
    }

    override fun getDefaultCurrency(): String =
        prefs.getString("default_currency", "EUR (€)") ?: "EUR (€)"

    override fun setDefaultCurrency(currency: String) {
        prefs.edit().putString("default_currency", currency).apply()
    }

    override fun getMonthlyCycleDay(): Int =
        prefs.getInt("monthly_cycle_day", 1)

    override fun setMonthlyCycleDay(day: Int) {
        prefs.edit().putInt("monthly_cycle_day", day).apply()
    }

    override fun getThemeMode(): String {
        val saved = prefs.getString("theme_mode", null)
        if (saved == null || saved == "SYSTEM") {
            prefs.edit().putString("theme_mode", "DARK").apply()
            return "DARK"
        }
        return saved
    }

    override fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeModeFlow.value = mode
    }

    override fun getAppLanguage(): String {
        return prefs.getString("app_language", "EN") ?: "EN"
    }

    override fun setAppLanguage(lang: String) {
        val uppercaseLang = lang.uppercase()
        prefs.edit().putString("app_language", uppercaseLang).apply()
        com.financas.app.core.i18n.AppLocale.isEnglish = uppercaseLang == "EN"
        _appLanguageFlow.value = uppercaseLang
    }

    override fun isPrivacyModeEnabled(): Boolean = prefs.getBoolean("privacy_mode_enabled", false)

    override fun setPrivacyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_mode_enabled", enabled).apply()
        com.financas.app.domain.model.MoneyCents.isPrivacyModeEnabled = enabled
        _privacyModeFlow.value = enabled
    }

    override fun isAutoUpdateCheckEnabled(): Boolean = prefs.getBoolean("auto_update_check", true)

    override fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_update_check", enabled).apply()
    }

    override fun isMarketQuotesAutoSyncEnabled(): Boolean = prefs.getBoolean("market_quotes_auto_sync", false)

    override fun setMarketQuotesAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("market_quotes_auto_sync", enabled).apply()
        if (enabled) setMarketQuotesDisclosureAccepted(true)
    }

    override fun isMarketQuotesDisclosureAccepted(): Boolean = prefs.getBoolean("market_quotes_disclosure", false)

    override fun setMarketQuotesDisclosureAccepted(accepted: Boolean) {
        prefs.edit().putBoolean("market_quotes_disclosure", accepted).apply()
    }
}
