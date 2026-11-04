package com.financas.app.ui.screens.account

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import com.financas.app.core.version.AppReleaseManager
import com.financas.app.core.version.ReleaseType
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.R
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.service.FinancialNotificationListenerService
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.theme.AccentAmber
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.AccentPurple
import com.financas.app.ui.theme.DarkBackground
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import com.financas.app.ui.util.exportTextFile
import com.financas.app.ui.util.shareText
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.AppStrings
import com.financas.app.ui.i18n.CategoryTranslator
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
    var jsonRestoreInput by remember { mutableStateOf("") }
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
                try {
                    context.contentResolver.takePersistableUriPermission(
                        selectedUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val localFile = context.filesDir.resolve("profile_photo.png")
                context.contentResolver.openInputStream(selectedUri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.updateProfilePhoto("${localFile.absolutePath}?t=${System.currentTimeMillis()}")
                Toast.makeText(context, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar foto: ${e.message}", Toast.LENGTH_SHORT).show()
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                context.contentResolver.openInputStream(selectedUri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    coroutineScope.launch {
                        viewModel.importJson(content)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao ler ficheiro: ${e.message}", Toast.LENGTH_LONG).show()
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar Box with photo / initials & Camera Badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (hasCustomPhoto) {
                                    showPhotoOptionsDialog = true
                                } else {
                                    photoPickerLauncher.launch("image/*")
                                }
                            }
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap,
                                contentDescription = "Foto de Perfil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(AccentBlue, Color(0xFF6366F1))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Camera Badge
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(AccentBlue)
                                .border(2.dp, DarkSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Alterar Foto",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // User Name & Subtitle
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = uiState.userName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar Nome",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Text(
                            text = "Conta Pessoal · Finflow Pro",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald)
                            )
                            Text(
                                text = "100% Offline & Seguro",
                                fontSize = 11.sp,
                                color = AccentEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
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
                                contentDescription = "Fechar",
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
                    onToggleBiometrics = { viewModel.toggleBiometrics(it) },
                    onSelectThemeMode = { viewModel.updateThemeMode(it) },
                    onSelectLanguage = { viewModel.updateAppLanguage(it) }
                )
            }
        }

        if (selectedAccountSection == AccountSection.SETTINGS) {
            // ================= SECTION 1: CATEGORIAS & CONTEXTOS =================
        item {
            Text(
                text = "Categorias & Contextos",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Organização das 4 dimensões (No Quê e Onde)",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categorias Pai & Subcategorias",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = {
                                targetParentForSubcategory = uiState.parentCategories.firstOrNull()
                                showAddSubcategoryDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova Subcategoria", color = AccentBlue, fontSize = 12.sp)
                        }
                    }

                    uiState.parentCategories.forEach { parent ->
                        ParentCategoryGroupItem(
                            parent = parent,
                            subcategories = uiState.subcategoriesByParent[parent.id] ?: emptyList(),
                            onAddSubcategory = {
                                targetParentForSubcategory = parent
                                showAddSubcategoryDialog = true
                            },
                            onDeleteSubcategory = { sub ->
                                pendingDeleteSubcategory = sub
                            }
                        )
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                    // Contexts "Onde"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contextos «Onde»",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        TextButton(onClick = { showAddContextDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Novo Contexto", color = AccentBlue, fontSize = 12.sp)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.contextTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================= SECTION 2: ORÇAMENTOS & METAS =================
        item {
            Text(
                text = "Orçamentos & Metas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Limites mensais por categoria e objetivos de poupança",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Orçamentos Mensais",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        TextButton(onClick = { showAddBudgetDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Novo Orçamento", color = AccentBlue, fontSize = 12.sp)
                        }
                    }

                    if (uiState.budgets.isEmpty()) {
                        Text(
                            text = "Sem orçamentos definidos. Cria um limite mensal por categoria.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    } else {
                        uiState.budgets.forEach { budget ->
                            BudgetRowItem(
                                budget = budget,
                                categoryName = categoriesById[budget.categoryId]?.name ?: budget.categoryId,
                                colorHex = categoriesById[budget.categoryId]?.colorHex ?: 0xFF888888,
                                onEdit = { editingBudget = budget },
                                onDelete = { pendingDeleteBudget = budget }
                            )
                        }
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metas de Poupança",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        TextButton(onClick = { showAddGoalDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova Meta", color = AccentBlue, fontSize = 12.sp)
                        }
                    }

                    if (uiState.goalItems.isEmpty()) {
                        Text(
                            text = "Sem metas criadas. Define um alvo de poupança com prazo opcional.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    } else {
                        uiState.goalItems.forEach { item ->
                            GoalRowItem(
                                item = item,
                                onArchive = { viewModel.archiveGoal(item.goal) },
                                onDelete = { pendingDeleteGoal = item.goal }
                            )
                        }
                    }
                }
            }
        }

        // ================= SECTION 3: CÓPIAS DE SEGURANÇA & DADOS =================
        item {
            Text(
                text = "Cópias de Segurança & Dados",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Portabilidade total: exportação atómica e sincronização offline",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
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
                    // Export JSON Button
                    ActionButtonItem(
                        icon = Icons.Default.FileUpload,
                        iconTint = AccentBlue,
                        title = "Exportar Backup Completo (JSON)",
                        subtitle = "Cópia completa e atómica de categorias, movimentos, orçamentos e metas",
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.exportJson()
                                exportTextFile(context, json, "backup_financas_${System.currentTimeMillis()}.json", "application/json")
                            }
                        }
                    )

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                    // Export CSV Button
                    ActionButtonItem(
                        icon = Icons.Default.TableChart,
                        iconTint = AccentEmerald,
                        title = "Exportar para Folha de Cálculo (CSV)",
                        subtitle = "Compatível com Excel em PT-PT com as 4 dimensões completas",
                        onClick = {
                            coroutineScope.launch {
                                val csv = viewModel.exportCsv()
                                exportTextFile(context, csv, "movimentos_financas_${System.currentTimeMillis()}.csv", "text/csv")
                            }
                        }
                    )

                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                    // Restore JSON Button
                    ActionButtonItem(
                        icon = Icons.Default.FileDownload,
                        iconTint = Color(0xFFF59E0B),
                        title = "Restaurar Backup (JSON)",
                        subtitle = "Importação atómica e idempotente de categorias, movimentos, orçamentos e metas",
                        onClick = {
                            showRestoreDialog = true
                        }
                    )
                }
            }
        }

        // ================= SECTION 4: SEGURANÇA & PRIVACIDADE =================
        item {
            Text(
                text = "Segurança & Privacidade",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bloqueio Biométrico",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (uiState.isBiometricsEnabled) "Ativo ao iniciar a aplicação" else "Desativado (arranque em 0s)",
                                    fontSize = 12.sp,
                                    color = if (uiState.isBiometricsEnabled) AccentEmerald else TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isBiometricsEnabled,
                            onCheckedChange = { viewModel.toggleBiometrics(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    Text(
                        text = "Por defeito, a aplicação abre instantaneamente em 0 segundos. Ative esta opção se pretender exigir autenticação biométrica (impressão digital ou reconhecimento facial) sempre que abrir a aplicação.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // ================= SECTION: CAPTURA DE PAGAMENTOS (MB WAY & BANCOS) =================
        item {
            Text(
                text = "Captura Automática (MB WAY & Bancos)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "100% Offline · Zero internet · Deteta compras e envia para Pendentes",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            var isAutoCaptureEnabled by remember {
                mutableStateOf(FinancialNotificationListenerService.isCaptureEnabled(context))
            }
            var isPermissionGranted by remember {
                mutableStateOf(FinancialNotificationListenerService.isNotificationAccessGranted(context))
            }
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        isPermissionGranted = FinancialNotificationListenerService.isNotificationAccessGranted(context)
                        FinancialNotificationListenerService.requestRebindIfGranted(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Captura Local de Notificações",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isAutoCaptureEnabled) "Ativo (analisa MB WAY, Moey, PayPal, Revolut e Bancos)" else "Desativado",
                                    fontSize = 12.sp,
                                    color = if (isAutoCaptureEnabled) AccentEmerald else TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isAutoCaptureEnabled,
                            onCheckedChange = {
                                isAutoCaptureEnabled = it
                                FinancialNotificationListenerService.setCaptureEnabled(context, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentEmerald,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    if (!isPermissionGranted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentAmber.copy(alpha = 0.15f))
                                .border(1.dp, AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Permissão do Android Necessária",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                                Text(
                                    text = "Para detetar pagamentos quando recebes notificação do MB WAY ou bancos, ativa o Acesso a Notificações no sistema Android.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("1. Ativar Acesso a Notificações", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "Se a opção estiver cinzenta (Definições Restritas no Android 13+): vai a Info da Aplicação, toca nos 3 pontos no canto superior direito e seleciona 'Permitir definições restritas'.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 15.sp
                                )

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("2. Abrir Info da App (Desbloquear Restritas)", color = AccentAmber, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentEmerald.copy(alpha = 0.15f))
                                .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Serviço Ativo e Autorizado no Android",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentEmerald
                                )
                            }
                        }
                    }

                    Text(
                        text = "Todas as compras detetadas vão para a 'Fila de Pendentes' no Início, onde podes rever, alterar a categoria/contexto ou descartar antes de guardar.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = DarkBorder)

                    Button(
                        onClick = {
                            viewModel.simulatePendingNotification(
                                amountCents = 1250L,
                                merchant = "Continente Bom Dia",
                                sourceApp = "MB WAY"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simular Notificação de Teste (12,50 € MB WAY)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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
        var subName by remember { mutableStateOf("") }
        var isFixed by remember { mutableStateOf(false) }
        var selectedParentId by remember {
            mutableStateOf(targetParentForSubcategory?.id ?: uiState.parentCategories.firstOrNull()?.id ?: "")
        }

        AlertDialog(
            onDismissRequest = { showAddSubcategoryDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Nova Subcategoria", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Categoria Pai:", fontSize = 12.sp, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.parentCategories.forEach { parent ->
                            val isSelected = parent.id == selectedParentId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AccentBlue else DarkSurfaceVariant)
                                    .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedParentId = parent.id }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = parent.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Nome da Subcategoria") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gasto/Rendimento Fixo?", fontSize = 13.sp, color = TextPrimary)
                        Switch(
                            checked = isFixed,
                            onCheckedChange = { isFixed = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subName.isNotBlank() && selectedParentId.isNotBlank()) {
                            viewModel.addSubcategory(
                                parentId = selectedParentId,
                                name = subName,
                                isFixed = isFixed
                            )
                            showAddSubcategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Adicionar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubcategoryDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Apagar Subcategoria
    pendingDeleteSubcategory?.let { sub ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSubcategory = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    "Apagar subcategoria?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Esta ação é definitiva e não pode ser anulada.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubcategory(sub)
                        pendingDeleteSubcategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
                ) {
                    Text("Apagar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSubcategory = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Novo Contexto
    if (showAddContextDialog) {
        var contextTagInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddContextDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Novo Contexto «Onde»", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exemplos: ESCRITORIO, FERIAS, ALGARVE, SUPERMERCADO",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = contextTagInput,
                        onValueChange = { contextTagInput = it },
                        label = { Text("Nome do Contexto") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contextTagInput.isNotBlank()) {
                            viewModel.addContextTag(contextTagInput)
                            showAddContextDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContextDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Restaurar Backup
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Restaurar Backup JSON", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pode escolher um ficheiro do dispositivo ou colar diretamente o conteúdo JSON:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = {
                            showRestoreDialog = false
                            filePickerLauncher.launch("*/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escolher Ficheiro .json", color = AccentBlue)
                    }

                    Text("Ou colar o texto JSON:", fontSize = 12.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = jsonRestoreInput,
                        onValueChange = { jsonRestoreInput = it },
                        placeholder = { Text("{\"version\": 2, ...}", color = TextMuted) },
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonRestoreInput.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.importJson(jsonRestoreInput)
                                showRestoreDialog = false
                                jsonRestoreInput = ""
                            }
                        }
                    },
                    enabled = jsonRestoreInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Restaurar Texto", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Novo Orçamento
    if (showAddBudgetDialog) {
        var selectedCategoryId by remember {
            mutableStateOf(categoriesAvailableForBudget.firstOrNull()?.id ?: "")
        }
        var limitInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Novo Orçamento", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (categoriesAvailableForBudget.isEmpty()) {
                        Text(
                            "Todas as categorias de despesa já têm um orçamento ativo.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    } else {
                        Text("Categoria:", fontSize = 12.sp, color = TextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            categoriesAvailableForBudget.forEach { category ->
                                val isSelected = category.id == selectedCategoryId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) AccentBlue else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) AccentBlue else DarkBorder, RoundedCornerShape(6.dp))
                                        .clickable { selectedCategoryId = category.id }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = category.name,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Limite Mensal (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitCents = parseEurosToCents(limitInput)
                        if (selectedCategoryId.isNotBlank() && limitCents != null) {
                            viewModel.addBudget(selectedCategoryId, limitCents)
                            showAddBudgetDialog = false
                        }
                    },
                    enabled = categoriesAvailableForBudget.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Definir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Editar limite do Orçamento
    editingBudget?.let { budget ->
        var limitInput by remember(budget.id) {
            mutableStateOf(
                DecimalFormat("0.00", DecimalFormatSymbols(Locale("pt", "PT")))
                    .format(budget.limitCents / 100.0)
            )
        }

        AlertDialog(
            onDismissRequest = { editingBudget = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    "Editar Orçamento",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Categoria: ${categoriesById[budget.categoryId]?.name ?: budget.categoryId}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Limite Mensal (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLimitCents = parseEurosToCents(limitInput)
                        if (newLimitCents != null) {
                            viewModel.updateBudgetLimit(budget, newLimitCents)
                            editingBudget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBudget = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Apagar Orçamento
    pendingDeleteBudget?.let { budget ->
        AlertDialog(
            onDismissRequest = { pendingDeleteBudget = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    "Apagar orçamento?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Esta ação é definitiva e não pode ser anulada.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBudget(budget)
                        pendingDeleteBudget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
                ) {
                    Text("Apagar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBudget = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Nova Meta
    if (showAddGoalDialog) {
        var nameInput by remember { mutableStateOf("") }
        var targetInput by remember { mutableStateOf("") }
        var hasDeadline by remember { mutableStateOf(false) }
        var yearInput by remember { mutableStateOf("") }
        var monthInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Nova Meta", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nome da Meta") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        label = { Text("Alvo (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Definir prazo?", fontSize = 13.sp, color = TextPrimary)
                        Switch(
                            checked = hasDeadline,
                            onCheckedChange = { hasDeadline = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                        )
                    }

                    if (hasDeadline) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = yearInput,
                                onValueChange = { yearInput = it },
                                label = { Text("Ano (AAAA)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = monthInput,
                                onValueChange = { monthInput = it },
                                label = { Text("Mês (1-12)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetCents = parseEurosToCents(targetInput)
                        val deadline = if (hasDeadline) {
                            parseDeadlineYearMonth(yearInput, monthInput)
                        } else {
                            null
                        }
                        if (nameInput.isNotBlank() && targetCents != null && (!hasDeadline || deadline != null)) {
                            viewModel.createGoal(nameInput, targetCents, deadline)
                            showAddGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Criar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Dialog: Apagar Meta
    pendingDeleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGoal = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    "Apagar meta?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Esta ação é definitiva e não pode ser anulada.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal)
                        pendingDeleteGoal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
                ) {
                    Text("Apagar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGoal = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
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

@Composable
private fun BudgetRowItem(
    budget: BudgetEntity,
    categoryName: String,
    colorHex: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(colorHex))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = categoryName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Limite: ${MoneyCents.formatCents(budget.limitCents)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar Orçamento",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar Orçamento",
                    tint = AccentCoral.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalRowItem(
    item: GoalWithProgress,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = item.goal
    val deadlineText = goal.deadlineYearMonth?.let { "Prazo: ${formatMonthYear(it)}" } ?: "Sem prazo"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (goal.archived) TextMuted else TextPrimary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (goal.archived) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Arquivada",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (item.progress.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (goal.archived) TextMuted else AccentEmerald,
                trackColor = DarkSurfaceHighlight
            )

            Text(
                text = "${MoneyCents.formatPercent(item.progress.percentComplete)} · Alvo: ${MoneyCents.formatCents(goal.targetCents)} · $deadlineText",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!goal.archived) {
                IconButton(
                    onClick = onArchive,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Arquivar Meta",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar Meta",
                    tint = AccentCoral.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun parseEurosToCents(raw: String): Long? {
    val cleaned = raw.trim().replace("€", "").trim()
    if (cleaned.isEmpty()) return null
    val value = cleaned.replace(',', '.').toDoubleOrNull() ?: return null
    if (value <= 0.0) return null
    return Math.round(value * 100.0)
}

private fun parseDeadlineYearMonth(yearInput: String, monthInput: String): Int? {
    val year = yearInput.trim().toIntOrNull() ?: return null
    val month = monthInput.trim().toIntOrNull() ?: return null
    if (month !in 1..12) return null
    if (year !in 1900..2999) return null
    return year * 100 + month
}

private fun formatMonthYear(yearMonth: Int): String {
    val ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
    return ym.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "PT")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }
}

@Composable
private fun ParentCategoryGroupItem(
    parent: CategoryEntity,
    subcategories: List<CategoryEntity>,
    onAddSubcategory: () -> Unit,
    onDeleteSubcategory: (CategoryEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(parent.colorHex))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = parent.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${subcategories.size} subcategorias • ${if (parent.type == "INCOME") "Receita" else "Despesa"}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onAddSubcategory,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Subcategoria",
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (subcategories.isEmpty()) {
                    Text(
                        text = "Sem subcategorias criadas.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 18.dp)
                    )
                } else {
                    subcategories.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(sub.colorHex).copy(alpha = 0.7f))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sub.name,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                if (sub.isDefaultFixed) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Fixo",
                                        fontSize = 10.sp,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteSubcategory(sub) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = AccentCoral.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ReleaseNotesDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Histórico de Versões",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Changelog de releases e patches",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${AppReleaseManager.CURRENT_VERSION}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(AppReleaseManager.RELEASES.size) { index ->
                    val release = AppReleaseManager.RELEASES[index]
                    val (badgeColor, badgeContainer) = when (release.type) {
                        ReleaseType.MAJOR -> AccentBlue to AccentBlue.copy(alpha = 0.15f)
                        ReleaseType.MINOR -> AccentEmerald to AccentEmerald.copy(alpha = 0.15f)
                        ReleaseType.PATCH -> AccentAmber to AccentAmber.copy(alpha = 0.15f)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "v${release.version}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = release.type.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    }
                                }

                                Text(
                                    text = release.date,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Text(
                                text = release.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            Text(
                                text = release.description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = DarkBorder,
                                thickness = 0.5.dp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                release.changes.forEach { change ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = change,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Fechar", color = Color.White)
            }
        }
    )
}

@Composable
private fun ProfileTabContent(
    uiState: AccountUiState,
    profileBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    initials: String,
    onEditProfile: () -> Unit,
    onEditPhoto: () -> Unit,
    onToggleBiometrics: (Boolean) -> Unit,
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
                    subtext = "Nome exibido no topo e nas mensagens da app"
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                ProfileInfoRow(
                    label = if (language == AppLanguage.EN) "Photo" else "Foto",
                    value = if (profileBitmap != null) "Foto personalizada ativa" else "Foto padrão ($initials)",
                    subtext = "Toca para alterar ou carregar foto da galeria",
                    onClick = onEditPhoto
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                ProfileInfoRow(
                    label = strings.currency,
                    value = uiState.defaultCurrency,
                    subtext = "Formato numérico com precisão em cêntimos"
                )

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                ProfileInfoRow(
                    label = if (language == AppLanguage.EN) "Cycle" else "Ciclo",
                    value = "Dia ${uiState.monthlyCycleDay} de cada mês",
                    subtext = "Data de renovação de metas e orçamentos"
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
                    subtext = "Alternar entre modo diurno, noturno ou seguir o sistema",
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
                    text = "Segurança do Perfil",
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
                            text = "Bloqueio Biométrico",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Exigir impressão digital ou Face Unlock ao abrir",
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
                        text = "Privacidade 100% Offline",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Todos os seus dados financeiros, nome e fotografia estão salvaguardados exclusivamente na memória segura deste telemóvel. O Finflow não possui servidores, telemetria nem rastreadores externos.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
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
private fun VersionTabContent(
    uiState: AccountUiState,
    updateState: com.financas.app.data.updater.UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (com.financas.app.domain.model.AppUpdateInfo) -> Unit,
    onInstallApk: (java.io.File) -> Unit,
    onResetUpdateState: () -> Unit,
    onResetDemoData: () -> Unit = {},
    onOpenReleaseNotes: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (com.financas.app.BuildConfig.IS_DEMO) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentPurple.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎭", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Modo Demonstração (Showcase)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )
                            Text(
                                text = "Esta aplicação contém dados fictícios para apresentações e testes.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onResetDemoData,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Repor Dados de Demonstração",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        // Card 1: Versão do Software & Auto-Updater
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
                    Column {
                        Text(
                            text = "Finflow",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${strings.versionTitle} ${com.financas.app.BuildConfig.VERSION_NAME} (${strings.buildNumber} ${com.financas.app.BuildConfig.VERSION_CODE})",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "v${com.financas.app.BuildConfig.VERSION_NAME}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                }

                // In-App Auto-Updater Status Card
                when (updateState) {
                    is com.financas.app.data.updater.UpdateUiState.Idle -> {
                        Button(
                            onClick = onCheckForUpdates,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.checkForUpdates,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is com.financas.app.data.updater.UpdateUiState.Checking -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AccentBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.checkingForUpdates,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    is com.financas.app.data.updater.UpdateUiState.UpToDate -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = strings.updateUpToDate,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentEmerald
                                        )
                                        Text(
                                            text = "v${com.financas.app.BuildConfig.VERSION_NAME}",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Text(
                                    text = strings.checkForUpdates,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentBlue,
                                    modifier = Modifier.clickable { onCheckForUpdates() }
                                )
                            }
                        }
                    }

                    is com.financas.app.data.updater.UpdateUiState.Available -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.10f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${strings.updateAvailableTitle} (${updateState.info.tagName})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlue
                                    )
                                }

                                if (updateState.info.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = updateState.info.releaseNotes.take(200),
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }

                                Button(
                                    onClick = { onDownloadUpdate(updateState.info) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.updateDownloadButton,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    is com.financas.app.data.updater.UpdateUiState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.updateDownloading,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${updateState.progressPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlue
                                )
                            }
                            LinearProgressIndicator(
                                progress = { updateState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AccentBlue,
                                trackColor = DarkBorder
                            )
                        }
                    }

                    is com.financas.app.data.updater.UpdateUiState.ReadyToInstall -> {
                        Button(
                            onClick = { onInstallApk(updateState.apkFile) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.updateReadyToInstall,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    is com.financas.app.data.updater.UpdateUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentCoral.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = updateState.message,
                                fontSize = 12.sp,
                                color = AccentCoral
                            )
                            Text(
                                text = strings.checkForUpdates,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                modifier = Modifier.clickable { onCheckForUpdates() }
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenReleaseNotes,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.versionHistory,
                        color = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Card 2: Estatísticas da Base de Dados & Sistema Local
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sistema & Base de Dados",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Motor da Base de Dados", fontSize = 13.sp, color = TextSecondary)
                    Text("SQLite Room v1", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Movimentos Gravados", fontSize = 13.sp, color = TextSecondary)
                    Text("${uiState.totalTransactionsCount} transações", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentEmerald)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Categorias Principais", fontSize = 13.sp, color = TextSecondary)
                    Text("${uiState.parentCategories.size} categorias", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Modo Operacional", fontSize = 13.sp, color = TextSecondary)
                    Text("100% Offline (Sem Servidor)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentEmerald)
                }
            }
        }

        // Card 3: Aviso Legal & Disclaimer Financeiro
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Aviso Legal & Isenção",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "O Finflow é uma ferramenta de gestão e organização pessoal offline. Os cálculos, estimativas de poupança, taxas de esforço e benefícios fiscais de PPR (art. 21º do EBF) têm finalidade meramente pedagógica e indicativa, não constituindo consultoria financeira, jurídica ou fiscal.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
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
                text = "Configurações do Perfil",
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
                    label = { Text("Nome de Utilizador", color = TextSecondary) },
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
                        text = "Moeda Principal",
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
                        text = "Início do Ciclo Mensal: Dia $cycleDay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dia em que os orçamentos e relatórios mensais renovam.",
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
                                    text = "Dia $day",
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
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun PhotoOptionsDialog(
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
                text = "Foto de Perfil",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Personalize o avatar da sua conta no Finflow.",
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
                    Text("Escolher Foto da Galeria", color = Color.White)
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
                        Text("Remover Foto", color = AccentCoral)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentMode: String,
    onSelectMode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple("LIGHT", "Modo Diurno (Claro)", "Cores claras otimizadas para ambientes iluminados"),
        Triple("DARK", "Modo Noturno (Escuro)", "Cores escuras de alto contraste para menor fadiga ocular"),
        Triple("SYSTEM", "Seguir o Sistema", "Adapta-se automaticamente à configuração do Android")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modo de Visualização",
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
                Text("Fechar", color = AccentBlue)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val options = listOf(
        Triple("EN", "English (Global)", "Default English version for global international use 🇬🇧"),
        Triple("PT", "Português", "Versão em Português com terminologia adaptada a Portugal 🇵🇹")
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
