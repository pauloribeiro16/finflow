package com.financas.app.domain.usecase

import com.financas.app.domain.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateChecker(
    private val releaseApiUrl: String = DEFAULT_GITHUB_API_URL
) {

    companion object {
        const val DEFAULT_GITHUB_API_URL =
            "https://api.github.com/repos/pauloribeiro16/finflow/releases/latest"

        /**
         * Compares two semantic version strings (e.g., "2.3.0" and "2.2.0").
         * Returns:
         *  > 0 if v1 > v2
         *  < 0 if v1 < v2
         *  == 0 if v1 == v2
         */
        fun compareVersions(v1: String, v2: String): Int {
            val s1 = v1.removePrefix("v").trim()
            val s2 = v2.removePrefix("v").trim()

            val parts1 = s1.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            val parts2 = s2.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) {
                    return p1.compareTo(p2)
                }
            }
            return 0
        }

        fun parseReleaseJson(jsonString: String): AppUpdateInfo? {
            val json = JSONObject(jsonString)
            val tagName = json.optString("tag_name", "").trim()
            if (tagName.isEmpty()) return null

            val versionName = tagName.removePrefix("v")
            val releaseNotes = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")
            val htmlUrl = json.optString("html_url", "")

            var apkUrl = ""
            var apkSize = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isEmpty()) return null

            return AppUpdateInfo(
                versionName = versionName,
                tagName = tagName,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                apkSize = apkSize,
                publishedAt = publishedAt,
                htmlUrl = htmlUrl
            )
        }
    }

    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(releaseApiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Finflow-App-Android")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@runCatching null
            }

            val responseText = connection.inputStream.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            connection.disconnect()

            val latestRelease = parseReleaseJson(responseText) ?: return@runCatching null

            if (compareVersions(latestRelease.versionName, currentVersion) > 0) {
                latestRelease
            } else {
                null
            }
        }
    }
}
