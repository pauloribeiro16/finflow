package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

interface BiometricPreferenceStore {
    fun isBiometricsEnabled(): Boolean
    fun setBiometricsEnabled(enabled: Boolean)
}

class SharedPrefsBiometricPreferenceStore(
    context: Context
) : BiometricPreferenceStore {
    private val prefs = context.getSharedPreferences("financas_security_prefs", Context.MODE_PRIVATE)

    override fun isBiometricsEnabled(): Boolean {
        return prefs.getBoolean("biometrics_enabled", false)
    }

    override fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
    }
}

class FakeBiometricPreferenceStore(
    private var enabled: Boolean = false
) : BiometricPreferenceStore {
    override fun isBiometricsEnabled(): Boolean = enabled
    override fun setBiometricsEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}

class BiometricLockGate(
    private val preferenceStore: BiometricPreferenceStore
) {
    private var credentialRequestCounter = 0

    fun isBiometricsEnabled(): Boolean = preferenceStore.isBiometricsEnabled()

    fun setBiometricsEnabled(enabled: Boolean) {
        preferenceStore.setBiometricsEnabled(enabled)
    }

    fun isDeviceSecure(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceSecure == true

    fun canEnableLock(context: Context): Boolean = isDeviceSecure(context)

    fun promptBiometric(
        activity: ComponentActivity,
        title: String = "Finflow",
        subtitle: String = tr("Confirme a sua identidade para aceder", "Confirm your identity to continue"),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricsEnabled()) {
            onSuccess()
            return
        }
        authenticate(activity, title, subtitle, onSuccess, onError)
    }

    fun authenticate(
        activity: ComponentActivity,
        title: String = "Finflow",
        subtitle: String = "Confirme a sua identidade",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isDeviceSecure(activity)) {
            onSuccess()
            return
        }
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    val prompt = BiometricPrompt.Builder(activity)
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build()
                    prompt.authenticate(CancellationSignal(), activity.mainExecutor, callback(onSuccess, onError))
                }
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                    @Suppress("DEPRECATION")
                    val prompt = BiometricPrompt.Builder(activity)
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setDeviceCredentialAllowed(true)
                        .build()
                    prompt.authenticate(CancellationSignal(), activity.mainExecutor, callback(onSuccess, onError))
                }
                Build.VERSION.SDK_INT == Build.VERSION_CODES.P -> {
                    val prompt = BiometricPrompt.Builder(activity)
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setNegativeButton("Usar PIN", activity.mainExecutor) { _, _ ->
                            launchDeviceCredential(activity, title, subtitle, onSuccess, onError)
                        }
                        .build()
                    prompt.authenticate(
                        CancellationSignal(),
                        activity.mainExecutor,
                        callback(onSuccess, onError) {
                            launchDeviceCredential(activity, title, subtitle, onSuccess, onError)
                        }
                    )
                }
                else -> launchDeviceCredential(activity, title, subtitle, onSuccess, onError)
            }
        } catch (e: Exception) {
            launchDeviceCredential(activity, title, subtitle, onSuccess, onError)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun callback(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onBiometricsUnavailable: (() -> Unit)? = null
    ) = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED) return
            if (onBiometricsUnavailable != null && errorCode in BIOMETRICS_UNAVAILABLE_ERRORS) {
                onBiometricsUnavailable()
                return
            }
            onError(errString?.toString() ?: tr("Erro de autenticação", "Authentication error"))
        }
    }

    private companion object {
        val BIOMETRICS_UNAVAILABLE_ERRORS = setOf(
            BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS,
            BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT,
            BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT
        )
    }

    private fun launchDeviceCredential(
        activity: ComponentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        @Suppress("DEPRECATION")
        val intent = keyguard?.createConfirmDeviceCredentialIntent(title, subtitle)
        if (intent == null) {
            if (isDeviceSecure(activity)) onError(tr("Autenticação indisponível", "Authentication unavailable")) else onSuccess()
            return
        }
        var launcher: ActivityResultLauncher<Intent>? = null
        launcher = activity.activityResultRegistry.register(
            "finflow_device_credential_${credentialRequestCounter++}",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            launcher?.unregister()
            if (result.resultCode == Activity.RESULT_OK) onSuccess() else onError(tr("Autenticação cancelada", "Authentication cancelled"))
        }
        launcher.launch(intent)
    }
}
