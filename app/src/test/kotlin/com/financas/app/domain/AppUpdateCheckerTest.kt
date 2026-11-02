package com.financas.app.domain

import com.financas.app.domain.usecase.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testCompareVersions() {
        assertTrue(AppUpdateChecker.compareVersions("2.3.0", "2.2.0") > 0)
        assertTrue(AppUpdateChecker.compareVersions("v2.3.0", "2.2.0") > 0)
        assertTrue(AppUpdateChecker.compareVersions("2.2.1", "2.2.0") > 0)
        assertEquals(0, AppUpdateChecker.compareVersions("2.2.0", "2.2.0"))
        assertEquals(0, AppUpdateChecker.compareVersions("v2.2.0", "2.2.0"))
        assertTrue(AppUpdateChecker.compareVersions("2.1.9", "2.2.0") < 0)
        assertTrue(AppUpdateChecker.compareVersions("2.10.0", "2.9.0") > 0)
        assertTrue(AppUpdateChecker.compareVersions("3.0.0", "2.99.99") > 0)
    }

    @Test
    fun testParseReleaseJsonWithApk() {
        val sampleJson = """
        {
          "tag_name": "v2.3.0",
          "name": "Finflow v2.3.0",
          "body": "Fixed chart scaling and added new categories.",
          "html_url": "https://github.com/pauloribeiro16/finflow/releases/tag/v2.3.0",
          "published_at": "2026-11-02T12:00:00Z",
          "assets": [
            {
              "name": "Finflow-v2.3.0.aab",
              "size": 22000000,
              "browser_download_url": "https://github.com/pauloribeiro16/finflow/releases/download/v2.3.0/Finflow-v2.3.0.aab"
            },
            {
              "name": "Finflow-v2.3.0.apk",
              "size": 34000000,
              "browser_download_url": "https://github.com/pauloribeiro16/finflow/releases/download/v2.3.0/Finflow-v2.3.0.apk"
            }
          ]
        }
        """.trimIndent()

        val parsed = AppUpdateChecker.parseReleaseJson(sampleJson)
        assertNotNull(parsed)
        assertEquals("2.3.0", parsed!!.versionName)
        assertEquals("v2.3.0", parsed.tagName)
        assertEquals("Fixed chart scaling and added new categories.", parsed.releaseNotes)
        assertEquals(
            "https://github.com/pauloribeiro16/finflow/releases/download/v2.3.0/Finflow-v2.3.0.apk",
            parsed.apkDownloadUrl
        )
        assertEquals(34000000L, parsed.apkSize)
    }

    @Test
    fun testParseReleaseJsonWithoutApkReturnsNull() {
        val sampleJson = """
        {
          "tag_name": "v2.3.0",
          "body": "Release without apk",
          "assets": [
            {
              "name": "source.zip",
              "browser_download_url": "https://example.com/source.zip"
            }
          ]
        }
        """.trimIndent()

        val parsed = AppUpdateChecker.parseReleaseJson(sampleJson)
        assertNull(parsed)
    }
}
