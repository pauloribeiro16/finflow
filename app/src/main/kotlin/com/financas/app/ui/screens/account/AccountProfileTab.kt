package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import android.provider.Settings
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings

@Composable
internal fun ProfileTabContent(
    uiState: AccountUiState,
    profileBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    initials: String,
    onEditProfile: () -> Unit,
    onEditPhoto: () -> Unit,
    onToggleBiometrics: (Boolean) -> Unit,
    onTogglePrivacyMode: (Boolean) -> Unit = {},
    onSelectThemeMode: (String) -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card 1: Preferências Pessoais & Moeda
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.sectionProfile,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    TextButton(onClick = onEditProfile) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.edit, color = AccentBlue, fontSize = 12.sp)
                    }
                }

                val language = LocalAppLanguage.current
                ProfileInfoRow(
                    label = if (language == AppLanguage.EN) "Name" else "Nome",
                    value = uiState.userName,
                    subtext = tr("Nome exibido no topo e nas mensagens da app", "Name shown at the top and in app messages")
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                ProfileInfoRow(
                    label = if (language == AppLanguage.EN) "Photo" else "Foto",
                    value = if (profileBitmap != null) "Foto personalizada ativa" else tr("Foto padrão ($initials)", "Default photo ($initials)"),
                    subtext = tr("Toca para alterar ou carregar foto da galeria", "Tap to change or upload a photo from the gallery"),
                    onClick = onEditPhoto
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                ProfileInfoRow(
                    label = strings.currency,
                    value = uiState.defaultCurrency,
                    subtext = tr("Formato numérico com precisão em cêntimos", "Number format with cent precision")
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                ProfileInfoRow(
                    label = if (language == AppLanguage.EN) "Cycle" else "Ciclo",
                    value = tr("Dia ${uiState.monthlyCycleDay} de cada mês", "Day ${uiState.monthlyCycleDay} of each month"),
                    subtext = tr("Data de renovação de metas e orçamentos", "Renewal date for goals and budgets")
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                val themeLabel = when (uiState.themeMode) {
                    "DARK" -> strings.themeDark
                    "LIGHT" -> strings.themeLight
                    else -> strings.themeSystem
                }

                ProfileInfoRow(
                    label = strings.themeMode,
                    value = themeLabel,
                    subtext = tr("Alternar entre modo diurno, noturno ou seguir o sistema", "Switch between light, dark or system mode"),
                    onClick = { showThemeDialog = true }
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                val languageLabel = if (uiState.appLanguage.equals("PT", ignoreCase = true)) "🇵🇹 Português" else "🇬🇧 English"
                ProfileInfoRow(
                    label = strings.languageSetting,
                    value = languageLabel,
                    subtext = strings.languageSelect,
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentMode = uiState.themeMode,
                onSelectMode = { mode ->
                    onSelectThemeMode(mode)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = uiState.appLanguage,
                onSelectLanguage = { lang ->
                    onSelectLanguage(lang)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // Card 2: Segurança do Perfil
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("Segurança do Perfil", "Profile Security"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.biometricLock,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.biometricLockSubtitle,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.isBiometricsEnabled,
                        onCheckedChange = onToggleBiometrics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue
                        )
                    )
                }

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.privacyModeTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.privacyModeSubtitle,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.isPrivacyMode,
                        onCheckedChange = onTogglePrivacyMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue
                        )
                    )
                }
            }
        }

        // Card 3: Garantia de Privacidade Local
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tr("Privacidade: Dados Locais", "Privacy: Local Data"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = tr("Todos os seus dados financeiros, nome e fotografia estão salvaguardados exclusivamente na memória segura deste telemóvel. O Finflow não possui servidores, telemetria nem rastreadores externos. Liga-se apenas ao GitHub, para procurar atualizações, e ao Yahoo Finance, para cotações dos tickers que guardar. As consultas automáticas a ambos podem ser desativadas em Versão.", "All your financial data, name and photo are kept only in this phone's secure storage. Finflow has no servers, telemetry or external trackers. It only connects to GitHub, to check for updates, and to Yahoo Finance, for quotes of the tickers you save. Automatic checks for both can be turned off under Version."),
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
internal fun ProfileInfoRow(
    label: String,
    value: String,
    subtext: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Text(
            text = subtext,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
internal fun EditProfileDialog(
    currentName: String,
    currentCurrency: String,
    currentCycleDay: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, currency: String, cycleDay: Int) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var currency by remember { mutableStateOf(currentCurrency) }
    var cycleDay by remember { mutableIntStateOf(currentCycleDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = tr("Configurações do Perfil", "Profile Settings"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nome
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Nome de Utilizador", "User Name"), color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Moeda
                Column {
                    Text(
                        text = tr("Moeda Principal", "Main Currency"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EUR (€)", "USD ($)", "GBP (£)").forEach { cur ->
                            val isSel = currency == cur
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                    .border(1.dp, if (isSel) AccentBlue else DarkBorder, RoundedCornerShape(10.dp))
                                    .clickable { currency = cur }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cur,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Início do Ciclo Mensal
                Column {
                    Text(
                        text = tr("Início do Ciclo Mensal: Dia $cycleDay", "Monthly Cycle Start: Day $cycleDay"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr("Dia em que os orçamentos e relatórios mensais renovam.", "Day on which monthly budgets and reports reset."),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                            val isSel = cycleDay == day
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) AccentBlue.copy(alpha = 0.25f) else DarkSurfaceVariant)
                                    .border(1.dp, if (isSel) AccentBlue else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { cycleDay = day }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tr("Dia $day", "Day $day"),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, currency, cycleDay)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Guardar", "Save"), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun PhotoOptionsDialog(
    hasCustomPhoto: Boolean,
    onDismiss: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = tr("Foto de Perfil", "Profile Photo"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("Personalize o avatar da sua conta no Finflow.", "Customise your Finflow account avatar."),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Button(
                    onClick = {
                        onDismiss()
                        onPickFromGallery()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Escolher Foto da Galeria", "Choose Photo from Gallery"), color = Color.White)
                }
                if (hasCustomPhoto) {
                    Button(
                        onClick = {
                            onRemovePhoto()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCoral.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = AccentCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("Remover Foto", "Remove Photo"), color = AccentCoral)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"), color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun ThemeSelectionDialog(
    currentMode: String,
    onSelectMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple("LIGHT", "Modo Diurno (Claro)", tr("Cores claras otimizadas para ambientes iluminados", "Light colours optimised for bright environments")),
        Triple("DARK", "Modo Noturno (Escuro)", tr("Cores escuras de alto contraste para menor fadiga ocular", "High-contrast dark colours for less eye strain")),
        Triple("SYSTEM", "Seguir o Sistema", tr("Adapta-se automaticamente à configuração do Android", "Adapts automatically to your Android setting"))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr("Modo de Visualização", "Display Mode"),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (mode, label, description) ->
                    val isSelected = currentMode == mode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceHighlight)
                            .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AccentBlue else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 15.sp
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Fechar", "Close"), color = AccentBlue)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
internal fun LanguageSelectionDialog(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val options = listOf(
        Triple("EN", "English (Global)", "Default English version for global international use 🇬🇧"),
        Triple("PT", "Português", tr("Versão em Português com terminologia adaptada a Portugal 🇵🇹", "Portuguese version with terminology adapted to Portugal 🇵🇹"))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.languageSelect,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (code, label, description) ->
                    val isSelected = currentLanguage.equals(code, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceHighlight)
                            .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { onSelectLanguage(code) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AccentBlue else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 15.sp
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close, color = AccentBlue)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
