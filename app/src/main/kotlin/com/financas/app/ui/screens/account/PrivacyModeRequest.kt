package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity

fun requestPrivacyModeChange(
    context: Context,
    enabled: Boolean,
    apply: (Boolean) -> Unit
) {
    if (enabled) {
        apply(true)
        return
    }
    val gate = (context.applicationContext as? com.financas.app.FinancasApplication)?.container?.biometricLockGate
    if (gate == null || !gate.isBiometricsEnabled()) {
        apply(false)
        return
    }
    val activity = context.findActivityForPrompt() ?: return
    gate.authenticate(
        activity = activity,
        subtitle = tr("Confirme para mostrar os valores", "Confirm to show amounts"),
        onSuccess = { apply(false) },
        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )
}

fun requireAuthIfLockEnabled(context: Context, subtitle: String, action: () -> Unit) {
    val gate = (context.applicationContext as? com.financas.app.FinancasApplication)?.container?.biometricLockGate
    if (gate == null || !gate.isBiometricsEnabled()) {
        action()
        return
    }
    val activity = context.findActivityForPrompt() ?: return
    gate.authenticate(
        activity = activity,
        subtitle = subtitle,
        onSuccess = action,
        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )
}

fun expectExternalFlow(context: Context) {
    (context.applicationContext as? com.financas.app.FinancasApplication)
        ?.container?.sessionBackgroundClock?.armExternalFlow(android.os.SystemClock.elapsedRealtime())
}

private fun Context.findActivityForPrompt(): ComponentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}
