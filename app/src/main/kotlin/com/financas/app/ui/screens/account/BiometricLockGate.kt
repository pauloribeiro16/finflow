package com.financas.app.ui.screens.account

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity

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
    fun isBiometricsEnabled(): Boolean = preferenceStore.isBiometricsEnabled()

    fun setBiometricsEnabled(enabled: Boolean) {
        preferenceStore.setBiometricsEnabled(enabled)
    }

    fun promptBiometric(
        activity: ComponentActivity,
        title: String = "Finflow",
        subtitle: String = "Confirme a sua biometria para aceder",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricsEnabled()) {
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButton("Cancelar", activity.mainExecutor) { _, _ ->
                        onError("Autenticação cancelada")
                    }
                    .build()

                prompt.authenticate(
                    CancellationSignal(),
                    activity.mainExecutor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            onError(errString?.toString() ?: "Erro de autenticação")
                        }
                    }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Biometria indisponível")
            }
        } else {
            onError("Autenticação indisponível nesta versão do Android")
        }
    }
}
