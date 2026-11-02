package com.financas.app.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.financas.app.domain.model.AppUpdateInfo
import com.financas.app.domain.usecase.AppUpdateChecker
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
    private val checker: AppUpdateChecker = AppUpdateChecker(),
    private val notificationHelper: AppNotificationHelper = AppNotificationHelper(context)
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

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "Finflow-${updateInfo.tagName}.apk")

            val url = URL(updateInfo.apkDownloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Finflow-App-Android")
            }

            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else updateInfo.apkSize
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
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

            connection.disconnect()
            _state.value = UpdateUiState.ReadyToInstall(apkFile)
            installApk(apkFile)
        } catch (e: Exception) {
            _state.value = UpdateUiState.Error(e.localizedMessage ?: "Erro durante o download do APK")
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) {
            _state.value = UpdateUiState.Error("Ficheiro APK não encontrado.")
            return
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

    fun resetState() {
        _state.value = UpdateUiState.Idle
    }
}
