package com.financas.app.data.repository

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.financas.app.data.prefs.ContextTagsStore
import com.financas.app.data.prefs.UserProfileStore
import com.financas.app.data.service.FinancialNotificationListenerService
import com.financas.app.domain.model.AppSettingsBackup
import com.financas.app.ui.screens.account.BiometricPreferenceStore
import java.io.ByteArrayOutputStream
import java.io.File

class SettingsBackupManager(
    private val context: Context,
    private val userProfileStore: UserProfileStore,
    private val biometricPreferenceStore: BiometricPreferenceStore,
    private val contextTagsStore: ContextTagsStore
) {

    private val photoFile: File get() = File(context.filesDir, PHOTO_FILE_NAME)

    fun snapshot(): AppSettingsBackup = AppSettingsBackup(
        userName = userProfileStore.getUserName(),
        defaultCurrency = userProfileStore.getDefaultCurrency(),
        monthlyCycleDay = userProfileStore.getMonthlyCycleDay(),
        themeMode = userProfileStore.getThemeMode(),
        appLanguage = userProfileStore.getAppLanguage(),
        biometricsEnabled = biometricPreferenceStore.isBiometricsEnabled(),
        notificationCaptureEnabled = FinancialNotificationListenerService.isCaptureEnabled(context),
        customContextTags = contextTagsStore.getCustomTags().sorted(),
        profilePhotoBase64 = encodePhoto()
    )

    fun apply(settings: AppSettingsBackup) {
        userProfileStore.setUserName(settings.userName)
        userProfileStore.setDefaultCurrency(settings.defaultCurrency)
        userProfileStore.setMonthlyCycleDay(settings.monthlyCycleDay)
        userProfileStore.setThemeMode(settings.themeMode)
        userProfileStore.setAppLanguage(settings.appLanguage)
        FinancialNotificationListenerService.setCaptureEnabled(context, settings.notificationCaptureEnabled)
        settings.customContextTags.forEach { contextTagsStore.addTag(it) }

        val canUseBiometrics = (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true
        biometricPreferenceStore.setBiometricsEnabled(settings.biometricsEnabled && canUseBiometrics)

        settings.profilePhotoBase64?.let { decodePhoto(it) }
    }

    private fun encodePhoto(): String? {
        val file = photoFile
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / sample > MAX_PHOTO_SIDE * 2 || bounds.outHeight / sample > MAX_PHOTO_SIDE * 2) sample *= 2
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
            val scale = MAX_PHOTO_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodePhoto(base64: String) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            if (bytes.isEmpty()) return
            photoFile.outputStream().use { it.write(bytes) }
            userProfileStore.setProfilePhotoUri("${photoFile.absolutePath}?t=${System.currentTimeMillis()}")
        } catch (_: Exception) {
        }
    }

    companion object {
        const val PHOTO_FILE_NAME = "profile_photo.png"
        const val MAX_PHOTO_SIDE = 768
    }
}
