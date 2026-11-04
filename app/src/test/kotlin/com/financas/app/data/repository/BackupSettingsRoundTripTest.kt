package com.financas.app.data.repository

import com.financas.app.domain.model.AppSettingsBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSettingsRoundTripTest {

    private val serializer = BackupSerializer()

    @Test
    fun settingsAndPhoto_surviveExportAndImport() {
        val settings = AppSettingsBackup(
            userName = "Paulo \"PR\" Ribeiro",
            defaultCurrency = "EUR (€)",
            monthlyCycleDay = 5,
            themeMode = "LIGHT",
            appLanguage = "PT",
            biometricsEnabled = true,
            notificationCaptureEnabled = false,
            customContextTags = listOf("GINASIO", "FERIAS 2026"),
            profilePhotoBase64 = "QUJDREVGRw=="
        )

        val json = serializer.exportToJson(emptyList(), emptyList(), settings = settings)
        val imported = serializer.importFromJson(json)

        assertEquals(BackupSerializer.CURRENT_SCHEMA_VERSION, imported.version)
        assertEquals(settings, imported.settings)
    }

    @Test
    fun backupWithoutSettings_importsWithNullSettings() {
        val json = serializer.exportToJson(emptyList(), emptyList())
        assertNull(serializer.importFromJson(json).settings)
    }

    @Test
    fun legacyBackupWithoutSettingsKey_stillImports() {
        val legacy = """{"version": 2, "categories": [], "transactions": [], "budgets": [], "goals": [], "investments": []}"""
        val imported = serializer.importFromJson(legacy)
        assertNull(imported.settings)
        assertTrue(imported.transactions.isEmpty())
    }

    @Test
    fun settingsWithoutPhoto_omitPhotoField() {
        val json = serializer.exportToJson(emptyList(), emptyList(), settings = AppSettingsBackup(userName = "A"))
        assertTrue(!json.contains("profilePhotoBase64"))
        assertNull(serializer.importFromJson(json).settings?.profilePhotoBase64)
    }
}
