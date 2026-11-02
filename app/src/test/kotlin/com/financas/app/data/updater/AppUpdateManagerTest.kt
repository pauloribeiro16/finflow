package com.financas.app.data.updater

import com.financas.app.domain.model.AppUpdateInfo
import com.financas.app.domain.usecase.AppUpdateChecker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testUpdateStateFlowTransitions() = runTest {
        val available = UpdateUiState.Available(
            AppUpdateInfo(
                versionName = "2.3.0",
                tagName = "v2.3.0",
                releaseNotes = "New features",
                apkDownloadUrl = "https://example.com/Finflow.apk",
                apkSize = 35000000L
            )
        )

        assertEquals("2.3.0", available.info.versionName)
        assertEquals("v2.3.0", available.info.tagName)
        assertEquals(35000000L, available.info.apkSize)

        val downloading = UpdateUiState.Downloading(45, 15000000L, 35000000L)
        assertEquals(45, downloading.progressPercent)
        assertEquals(15000000L, downloading.downloadedBytes)

        val ready = UpdateUiState.ReadyToInstall(java.io.File("/tmp/Finflow.apk"))
        assertEquals("/tmp/Finflow.apk", ready.apkFile.absolutePath)

        assertTrue(UpdateUiState.UpToDate is UpdateUiState)
        assertTrue(UpdateUiState.Checking is UpdateUiState)
        assertTrue(UpdateUiState.Idle is UpdateUiState)
    }

    @Test
    fun testSemVerParsingMultiDigits() {
        assertTrue(AppUpdateChecker.compareVersions("2.10.0", "2.9.9") > 0)
        assertTrue(AppUpdateChecker.compareVersions("10.0.0", "9.99.99") > 0)
        assertTrue(AppUpdateChecker.compareVersions("2.2.1-beta", "2.2.0") > 0)
    }
}
