package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.DarkBackground
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import com.financas.app.ui.util.writeTextToUri
import com.financas.app.ui.util.readTextLimited
import com.financas.app.ui.util.copyImageDownscaled
import kotlinx.coroutines.launch
import com.financas.app.ui.i18n.AppStrings
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings

enum class AccountSection(val icon: ImageVector) {
    PROFILE(Icons.Default.Person),
    SETTINGS(Icons.Default.Tune),
    VERSION(Icons.Default.Info);

    fun label(strings: AppStrings): String = when (this) {
        PROFILE -> strings.sectionProfile
        SETTINGS -> strings.sectionSettings
        VERSION -> strings.sectionVersion
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val requestBiometricToggle: (Boolean) -> Unit = { enabled ->
        val activity = context.findComponentActivity()
        val gate = (context.applicationContext as? com.financas.app.FinancasApplication)?.container?.biometricLockGate
        when {
            activity == null || gate == null -> Unit
            enabled && !gate.canEnableLock(context) -> Toast.makeText(
                context,
                tr("Configura primeiro um bloqueio de ecrã (PIN, padrão ou biometria) nas definições do telemóvel.", "First set up a screen lock (PIN, pattern or biometrics) in your phone settings."),
                Toast.LENGTH_LONG
            ).show()
            else -> gate.authenticate(
                activity = activity,
                subtitle = if (enabled) tr("Confirme para ativar o bloqueio", "Confirm to enable the lock") else tr("Confirme para desativar o bloqueio", "Confirm to disable the lock"),
                onSuccess = { viewModel.toggleBiometrics(enabled) },
                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
            )
        }
    }
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current

    var selectedAccountSection by remember { mutableStateOf(AccountSection.PROFILE) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    var showAddSubcategoryDialog by remember { mutableStateOf(false) }
    var targetParentForSubcategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var pendingDeleteSubcategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddContextDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
    var pendingEncryptedImport by remember { mutableStateOf<String?>(null) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var pendingDeleteBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var pendingDeleteGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var showReleaseNotesDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val localFile = context.filesDir.resolve("profile_photo.png")
                if (!copyImageDownscaled(context, selectedUri, localFile)) {
                    throw IllegalArgumentException(tr("imagem inválida ou demasiado grande", "invalid or too large image"))
                }
                viewModel.updateProfilePhoto("${localFile.absolutePath}?t=${System.currentTimeMillis()}")
                Toast.makeText(context, tr("Foto de perfil atualizada!", "Profile photo updated!"), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, tr("Erro ao carregar foto: ${e.message}", "Error loading photo: ${e.message}"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val profileBitmap = remember(uiState.profilePhotoUri) {
        val localFile = context.filesDir.resolve("profile_photo.png")
        when {
            localFile.exists() && localFile.length() > 0 -> {
                try {
                    android.graphics.BitmapFactory.decodeFile(localFile.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            uiState.profilePhotoUri != null -> {
                try {
                    val raw = uiState.profilePhotoUri!!.substringBefore('?')
                    if (raw.startsWith("/")) {
                        val file = java.io.File(raw)
                        if (file.exists() && file.length() > 0) {
                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                        } else null
                    } else {
                        val uri = Uri.parse(raw)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            localFile.outputStream().use { out -> stream.copyTo(out) }
                        }
                        if (localFile.exists() && localFile.length() > 0) {
                            android.graphics.BitmapFactory.decodeFile(localFile.absolutePath)?.asImageBitmap()
                        } else null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    val hasCustomPhoto = profileBitmap != null || (context.filesDir.resolve("profile_photo.png").let { it.exists() && it.length() > 0 })

    val initials = remember(uiState.userName) { com.financas.app.ui.components.profileInitials(uiState.userName) }

    val categoriesById = uiState.parentCategories.associateBy { it.id } +
        uiState.subcategoriesByParent.values.flatten().associateBy { it.id }
    val expenseCategories = (uiState.parentCategories + uiState.subcategoriesByParent.values.flatten())
        .filter { it.type == "EXPENSE" }
        .sortedWith(compareBy({ it.parentId ?: it.id }, { it.sortOrder }))
    val categoriesAvailableForBudget = expenseCategories.filter { it.id !in uiState.budgetedCategoryIds }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { target ->
            coroutineScope.launch {
                val ok = writeTextToUri(context, target, viewModel.exportJson())
                Toast.makeText(context, if (ok) "Backup guardado." else tr("Não foi possível guardar o backup.", "Could not save the backup."), Toast.LENGTH_LONG).show()
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { target ->
            coroutineScope.launch {
                val ok = writeTextToUri(context, target, viewModel.exportCsv())
                Toast.makeText(context, if (ok) "CSV guardado." else tr("Não foi possível guardar o CSV.", "Could not save the CSV."), Toast.LENGTH_LONG).show()
            }
        }
    }

    val encryptedExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val password = pendingExportPassword
        pendingExportPassword = null
        if (uri != null && password != null) {
            coroutineScope.launch {
                val ok = try {
                    val envelope = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        com.financas.app.data.repository.BackupCrypto.encrypt(viewModel.exportJson(), password.toCharArray())
                    }
                    writeTextToUri(context, uri, envelope)
                } catch (_: Exception) {
                    false
                }
                Toast.makeText(context, if (ok) "Backup cifrado guardado." else tr("Não foi possível guardar o backup.", "Could not save the backup."), Toast.LENGTH_LONG).show()
            }
        }
    }

    val routeImportContent: (String) -> Unit = { content ->
        if (com.financas.app.data.repository.BackupCrypto.isEncrypted(content)) {
            pendingEncryptedImport = content
        } else {
            coroutineScope.launch { viewModel.importJson(content) }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                val content = readTextLimited(context, selectedUri)
                routeImportContent(content)
            } catch (e: Exception) {
                Toast.makeText(context, tr("Erro ao ler ficheiro: ${e.message}", "Error reading file: ${e.message}"), Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Profile Header Card
        item {
            ProfileHeaderCard(
                uiState = uiState,
                profileBitmap = profileBitmap,
                hasCustomPhoto = hasCustomPhoto,
                initials = initials,
                onAvatarClick = {
                    if (hasCustomPhoto) showPhotoOptionsDialog = true else photoPickerLauncher.launch("image/*")
                },
                onEditProfile = { showEditProfileDialog = true }
            )
        }

        // Segmented Navigation Bar: Perfil | Configurações | Versão
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccountSection.entries.forEach { section ->
                    val isSelected = selectedAccountSection == section
                    val bgAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 0.18f else 0.0f,
                        animationSpec = tween(180),
                        label = "tabBgAlpha"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) AccentBlue else TextSecondary,
                        animationSpec = tween(180),
                        label = "tabTextColor"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "tabScale"
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentBlue.copy(alpha = bgAlpha))
                            .clickable { selectedAccountSection = section }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.label(strings),
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = section.label(strings),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Status / Feedback Banner
        if (uiState.message != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.message ?: "",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Fechar", "Close"),
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (selectedAccountSection == AccountSection.PROFILE) {
            item {
                ProfileTabContent(
                    uiState = uiState,
                    profileBitmap = profileBitmap,
                    initials = initials,
                    onEditProfile = { showEditProfileDialog = true },
                    onEditPhoto = {
                        if (hasCustomPhoto) {
                            showPhotoOptionsDialog = true
                        } else {
                            photoPickerLauncher.launch("image/*")
                        }
                    },
                    onToggleBiometrics = { requestBiometricToggle(it) },
                    onTogglePrivacyMode = { enabled ->
                        requestPrivacyModeChange(context, enabled) { viewModel.setPrivacyMode(it) }
                    },
                    onSelectThemeMode = { viewModel.updateThemeMode(it) },
                    onSelectLanguage = { viewModel.updateAppLanguage(it) }
                )
            }
        }

        if (selectedAccountSection == AccountSection.SETTINGS) {
            // ================= SECTION 1: CATEGORIAS & CONTEXTOS =================
        item {
            Text(
                text = tr("Categorias & Contextos", "Categories & Contexts"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr("Organização das 4 dimensões (No Quê e Onde)", "Organisation of the 4 dimensions (What and Where)"),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            CategoriesSettingsCard(
                uiState = uiState,
                onAddSubcategory = { parent ->
                    targetParentForSubcategory = parent
                    showAddSubcategoryDialog = true
                },
                onDeleteSubcategory = { pendingDeleteSubcategory = it },
                onAddContext = { showAddContextDialog = true }
            )
        }

        // ================= SECTION 2: ORÇAMENTOS & METAS =================
        item {
            Text(
                text = tr("Orçamentos & Metas", "Budgets & Goals"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr("Limites mensais por categoria e objetivos de poupança", "Monthly limits per category and savings goals"),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            BudgetsGoalsSettingsCard(
                viewModel = viewModel,
                uiState = uiState,
                categoriesById = categoriesById,
                onAddBudget = { showAddBudgetDialog = true },
                onEditBudget = { editingBudget = it },
                onDeleteBudget = { pendingDeleteBudget = it },
                onAddGoal = { showAddGoalDialog = true },
                onDeleteGoal = { pendingDeleteGoal = it }
            )
        }

        // ================= SECTION 3: CÓPIAS DE SEGURANÇA & DADOS =================
        item {
            Text(
                text = tr("Cópias de Segurança & Dados", "Backups & Data"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr("Portabilidade total: exportação atómica e sincronização offline", "Full portability: atomic export and offline sync"),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            BackupSettingsCard(
                onExportBackup = { showBackupPasswordDialog = true },
                onExportCsv = { csvExportLauncher.launch("movimentos_financas_${System.currentTimeMillis()}.csv") },
                onRestore = { showRestoreDialog = true }
            )
        }

        // ================= SECTION 4: SEGURANÇA & PRIVACIDADE =================
        item {
            Text(
                text = tr("Segurança & Privacidade", "Security & Privacy"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
            SecuritySettingsCard(viewModel, uiState, onToggleBiometrics = requestBiometricToggle)
        }

        // ================= SECTION: CAPTURA DE PAGAMENTOS (MB WAY & BANCOS) =================
        item {
            Text(
                text = tr("Captura Automática (MB WAY & Bancos)", "Automatic Capture (MB WAY & Banks)"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr("Processado no telemóvel · Nada é enviado · Deteta compras e envia para Pendentes", "Processed on the phone · Nothing is sent · Detects purchases and sends them to Pending"),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            NotificationCaptureCard(viewModel)
        }
    }

    if (selectedAccountSection == AccountSection.VERSION) {
        item {
            VersionTabContent(
                uiState = uiState,
                updateState = updateState,
                onCheckForUpdates = {
                    viewModel.checkForUpdates(com.financas.app.BuildConfig.VERSION_NAME)
                },
                onDownloadUpdate = { info ->
                    viewModel.downloadUpdate(info)
                },
                onInstallApk = { file ->
                    viewModel.installApk(file)
                },
                onResetUpdateState = {
                    viewModel.resetUpdateState()
                },
                onResetDemoData = {
                    viewModel.resetDemoData()
                },
                onOpenReleaseNotes = { showReleaseNotesDialog = true }
            )
        }
    }
}

    // Dialog: Nova Subcategoria
    if (showAddSubcategoryDialog) {
        AddSubcategoryDialog(viewModel, uiState, targetParentForSubcategory) { showAddSubcategoryDialog = false }
    }

    // Dialog: Apagar Subcategoria
    pendingDeleteSubcategory?.let { sub ->
        DeleteSubcategoryDialog(viewModel, sub) { pendingDeleteSubcategory = null }
    }

    // Dialog: Novo Contexto
    if (showAddContextDialog) {
        AddContextDialog(viewModel) { showAddContextDialog = false }
    }

    // Dialog: Restaurar Backup
    if (showBackupPasswordDialog) {
        BackupPasswordDialog(
            confirmMode = true,
            onDismiss = { showBackupPasswordDialog = false },
            onConfirm = { password ->
                showBackupPasswordDialog = false
                pendingExportPassword = password
                encryptedExportLauncher.launch("backup_financas_${System.currentTimeMillis()}.finflow")
            },
            onExportPlain = {
                showBackupPasswordDialog = false
                jsonExportLauncher.launch("backup_financas_${System.currentTimeMillis()}.json")
            }
        )
    }

    pendingEncryptedImport?.let { envelope ->
        BackupPasswordDialog(
            confirmMode = false,
            onDismiss = { pendingEncryptedImport = null },
            onConfirm = { password ->
                pendingEncryptedImport = null
                coroutineScope.launch {
                    try {
                        val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            com.financas.app.data.repository.BackupCrypto.decrypt(envelope, password.toCharArray())
                        }
                        viewModel.importJson(json)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: tr("Não foi possível abrir o backup.", "Could not open the backup."), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showRestoreDialog) {
        RestoreBackupDialog(
            onPickFile = { filePickerLauncher.launch("*/*") },
            onRestoreText = routeImportContent,
            onDismiss = { showRestoreDialog = false }
        )
    }

    // Dialog: Novo Orçamento
    if (showAddBudgetDialog) {
        AddBudgetDialog(viewModel, categoriesAvailableForBudget) { showAddBudgetDialog = false }
    }

    // Dialog: Editar limite do Orçamento
    editingBudget?.let { budget ->
        EditBudgetDialog(viewModel, budget, categoriesById) { editingBudget = null }
    }

    // Dialog: Apagar Orçamento
    pendingDeleteBudget?.let { budget ->
        DeleteBudgetDialog(viewModel, budget) { pendingDeleteBudget = null }
    }

    // Dialog: Nova Meta
    if (showAddGoalDialog) {
        AddGoalDialog(viewModel) { showAddGoalDialog = false }
    }

    // Dialog: Apagar Meta
    pendingDeleteGoal?.let { goal ->
        DeleteGoalDialog(viewModel, goal) { pendingDeleteGoal = null }
    }

    if (showReleaseNotesDialog) {
        ReleaseNotesDialog(
            onDismissRequest = { showReleaseNotesDialog = false }
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = uiState.userName,
            currentCurrency = uiState.defaultCurrency,
            currentCycleDay = uiState.monthlyCycleDay,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, currency, cycleDay ->
                viewModel.updateUserName(name)
                viewModel.updateDefaultCurrency(currency)
                viewModel.updateMonthlyCycleDay(cycleDay)
            }
        )
    }

    if (showPhotoOptionsDialog) {
        PhotoOptionsDialog(
            hasCustomPhoto = hasCustomPhoto,
            onDismiss = { showPhotoOptionsDialog = false },
            onPickFromGallery = { photoPickerLauncher.launch("image/*") },
            onRemovePhoto = {
                viewModel.updateProfilePhoto(null)
                try {
                    val local = context.filesDir.resolve("profile_photo.png")
                    if (local.exists()) local.delete()
                } catch (_: Exception) {}
            }
        )
    }

}

internal fun android.content.Context.findComponentActivity(): androidx.activity.ComponentActivity? {
    var current: android.content.Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is androidx.activity.ComponentActivity) return current
        current = current.baseContext
    }
    return null
}
