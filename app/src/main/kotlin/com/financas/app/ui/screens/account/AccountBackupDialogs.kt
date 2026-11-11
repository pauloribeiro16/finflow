package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary

@Composable
internal fun BackupPasswordDialog(
    confirmMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onExportPlain: (() -> Unit)? = null
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val minLength = com.financas.app.data.repository.BackupCrypto.MIN_PASSWORD_LENGTH
    val valid = if (confirmMode) password.length >= minLength && password == repeat else password.isNotEmpty()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentBlue,
        unfocusedBorderColor = DarkBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                if (confirmMode) "Proteger backup" else "Backup cifrado",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (confirmMode) {
                        tr("O ficheiro fica cifrado (AES-256). Sem esta palavra-passe não é possível restaurá-lo — guarde-a num local seguro. Mínimo $minLength caracteres.", "The file is encrypted (AES-256). Without this password it cannot be restored — keep it somewhere safe. Minimum $minLength characters.")
                    } else {
                        "Introduza a palavra-passe usada ao exportar este backup."
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(tr("Palavra-passe", "Password"), color = TextSecondary) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmMode) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = { repeat = it },
                        label = { Text(tr("Repetir palavra-passe", "Repeat password"), color = TextSecondary) },
                        singleLine = true,
                        isError = repeat.isNotEmpty() && repeat != password,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (onExportPlain != null) {
                    TextButton(onClick = onExportPlain) {
                        Text(tr("Exportar sem cifra (JSON legível por qualquer pessoa)", "Export without encryption (JSON readable by anyone)"), color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = password
                    password = ""
                    repeat = ""
                    onConfirm(value)
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(if (confirmMode) tr("Exportar", "Export") else "Abrir", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}
