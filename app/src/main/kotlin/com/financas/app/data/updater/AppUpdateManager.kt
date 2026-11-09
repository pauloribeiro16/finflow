package com.financas.app.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.SigningInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.financas.app.domain.model.AppUpdateInfo
import com.financas.app.domain.usecase.AppUpdateChecker
import com.financas.app.domain.usecase.UpdateSignatureCheck
import com.financas.app.domain.usecase.UpdateUrlPolicy
import com.financas.app.ui.util.AppNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val info: AppUpdateInfo) : UpdateUiState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateUiState
    data class ReadyToInstall(val apkFile: File) : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class AppUpdateManager(
    private val context: Context,
    private val checker: AppUpdateChecker = AppUpdateChecker(isDemo = com.financas.app.BuildConfig.IS_DEMO),
    private val notificationHelper: AppNotificationHelper = AppNotificationHelper(context),
    private val backupProvider: (suspend () -> String)? = null
) {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    suspend fun checkForUpdates(
        currentVersion: String,
        notifyIfAvailable: Boolean = true
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        _state.value = UpdateUiState.Checking
        val result = checker.checkForUpdate(currentVersion)
        val updateInfo = result.getOrNull()

        if (updateInfo != null) {
            _state.value = UpdateUiState.Available(updateInfo)
            if (notifyIfAvailable) {
                notificationHelper.postUpdateAvailableNotification(updateInfo)
            }
            updateInfo
        } else if (result.isSuccess) {
            _state.value = UpdateUiState.UpToDate
            null
        } else {
            val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Falha ao contactar servidor de atualizações"
            _state.value = UpdateUiState.Error(errorMsg)
            null
        }
    }

    suspend fun downloadUpdate(updateInfo: AppUpdateInfo) = withContext(Dispatchers.IO) {
        try {
            _state.value = UpdateUiState.Downloading(0, 0L, updateInfo.apkSize)

            if (!UpdateUrlPolicy.isAllowedReleaseUrl(updateInfo.apkDownloadUrl)) {
                _state.value = UpdateUiState.Error("Endereço de download não autorizado.")
                return@withContext
            }

            backupProvider?.let { provider ->
                runCatching { savePrivateBackup(context, provider(), updateInfo.tagName) }
            }

            cleanupDownloads(context)
            val updatesDir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
            val safeTag = updateInfo.tagName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val apkFile = File(updatesDir, "Finflow-$safeTag.apk")

            val connection = openFollowingAllowedRedirects(updateInfo.apkDownloadUrl)
            try {
                val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else updateInfo.apkSize
                if (totalBytes > MAX_APK_BYTES) throw IllegalStateException("APK demasiado grande.")
                var downloadedBytes = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            downloadedBytes += bytesRead
                            if (downloadedBytes > MAX_APK_BYTES) throw IllegalStateException("APK demasiado grande.")
                            output.write(buffer, 0, bytesRead)
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            _state.value = UpdateUiState.Downloading(percent, downloadedBytes, totalBytes)
                        }
                        output.flush()
                    }
                }
            } finally {
                connection.disconnect()
            }

            _state.value = UpdateUiState.ReadyToInstall(apkFile)
            installApk(apkFile)
        } catch (e: Exception) {
            _state.value = UpdateUiState.Error(e.localizedMessage ?: "Erro durante o download do APK")
        }
    }

    private fun openFollowingAllowedRedirects(startUrl: String): HttpURLConnection {
        var current = startUrl
        repeat(MAX_REDIRECTS + 1) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "Finflow-App-Android")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                val next = location?.let { URL(URL(current), it).toString() }
                    ?: throw IllegalStateException("Redirecionamento inválido.")
                if (!UpdateUrlPolicy.isAllowedRedirect(next)) {
                    throw IllegalStateException("Redirecionamento para servidor não autorizado.")
                }
                current = next
                return@repeat
            }
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IllegalStateException("Servidor devolveu HTTP $code.")
            }
            return connection
        }
        throw IllegalStateException("Demasiados redirecionamentos.")
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) {
            _state.value = UpdateUiState.Error("Ficheiro APK não encontrado.")
            return
        }

        when (verifyCandidate(apkFile)) {
            CandidateCheck.OK -> Unit
            CandidateCheck.NOT_UPGRADE -> {
                apkFile.delete()
                _state.value = UpdateUiState.Error(UpdateSignatureCheck.INVALID_PACKAGE_MESSAGE)
                return
            }
            CandidateCheck.UNTRUSTED -> {
                apkFile.delete()
                _state.value = UpdateUiState.Error(UpdateSignatureCheck.UNTRUSTED_MESSAGE)
                return
            }
            CandidateCheck.MISMATCH -> {
                apkFile.delete()
                _state.value = UpdateUiState.Error(UpdateSignatureCheck.MISMATCH_MESSAGE)
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }

    private enum class CandidateCheck { OK, NOT_UPGRADE, UNTRUSTED, MISMATCH }

    private fun verifyCandidate(apkFile: File): CandidateCheck {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val installed = pm.getPackageInfo(context.packageName, flags)
            val candidate = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                ?: return CandidateCheck.UNTRUSTED

            if (!UpdateSignatureCheck.isSamePackageUpgrade(
                    installedPackage = context.packageName,
                    installedVersionCode = versionCodeOf(installed),
                    candidatePackage = candidate.packageName,
                    candidateVersionCode = versionCodeOf(candidate)
                )
            ) {
                return CandidateCheck.NOT_UPGRADE
            }

            val candidateDigests = signerDigests(candidate)
            if (!UpdateSignatureCheck.isOfficialRelease(candidateDigests)) return CandidateCheck.UNTRUSTED
            if (!UpdateSignatureCheck.isCompatible(signerDigests(installed), candidateDigests)) return CandidateCheck.MISMATCH
            CandidateCheck.OK
        } catch (e: Exception) {
            CandidateCheck.UNTRUSTED
        }
    }

    private fun versionCodeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingCertificates(info.signingInfo)
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.toList().orEmpty()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun signingCertificates(info: SigningInfo?): List<android.content.pm.Signature> {
        if (info == null) return emptyList()
        val signers = if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        return signers?.toList().orEmpty()
    }

    fun resetState() {
        _state.value = UpdateUiState.Idle
    }

    companion object {
        const val UPDATES_DIR = "updates"
        const val BACKUPS_DIR = "backups"
        const val MAX_PRIVATE_BACKUPS = 3
        const val MAX_REDIRECTS = 5
        const val MAX_APK_BYTES = 200L * 1024 * 1024

        fun cleanupDownloads(context: Context) {
            File(context.cacheDir, UPDATES_DIR).listFiles()?.forEach { it.delete() }
        }

        fun cleanupStaleDownloads(context: Context, maxAgeMs: Long = 24L * 60 * 60 * 1000) {
            val cutoff = System.currentTimeMillis() - maxAgeMs
            File(context.cacheDir, UPDATES_DIR).listFiles()
                ?.filter { it.lastModified() < cutoff }
                ?.forEach { it.delete() }
        }

        fun savePrivateBackup(context: Context, json: String, tagName: String): File {
            val dir = File(context.filesDir, BACKUPS_DIR).apply { mkdirs() }
            val safeTag = tagName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "antes_de_atualizar_${safeTag}_${System.currentTimeMillis()}.json")
            file.writeText(json)
            dir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_PRIVATE_BACKUPS)
                ?.forEach { it.delete() }
            return file
        }
    }
}
