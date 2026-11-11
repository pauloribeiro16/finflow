package com.financas.app.ui.screens.account

import com.financas.app.core.i18n.tr

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.BudgetEntity
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.GoalEntity
import com.financas.app.data.service.FinancialNotificationListenerService
import com.financas.app.ui.theme.AccentAmber
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary

@Composable
internal fun ProfileHeaderCard(
    uiState: AccountUiState,
    profileBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    hasCustomPhoto: Boolean,
    initials: String,
    onAvatarClick: () -> Unit,
    onEditProfile: () -> Unit
) {
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
                            onAvatarClick()
                        } else {
                            onAvatarClick()
                        }
                    }
            ) {
                if (profileBitmap != null) {
                    Image(
                        bitmap = profileBitmap,
                        contentDescription = tr("Foto de Perfil", "Profile Photo"),
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
                        contentDescription = tr("Alterar Foto", "Change Photo"),
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
                        onClick = onEditProfile,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = tr("Editar Nome", "Edit Name"),
                            tint = AccentBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = tr("Conta Pessoal · Finflow Pro", "Personal Account · Finflow Pro"),
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
                        text = tr("Dados locais & cifrados", "Local & encrypted data"),
                        fontSize = 11.sp,
                        color = AccentEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
internal fun CategoriesSettingsCard(
    uiState: AccountUiState,
    onAddSubcategory: (CategoryEntity?) -> Unit,
    onDeleteSubcategory: (CategoryEntity) -> Unit,
    onAddContext: () -> Unit
) {
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
                    text = tr("Categorias Pai & Subcategorias", "Parent Categories & Subcategories"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = {
                        onAddSubcategory(uiState.parentCategories.firstOrNull())
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Nova Subcategoria", "New Subcategory"), color = AccentBlue, fontSize = 12.sp)
                }
            }

            uiState.parentCategories.forEach { parent ->
                ParentCategoryGroupItem(
                    parent = parent,
                    subcategories = uiState.subcategoriesByParent[parent.id] ?: emptyList(),
                    onAddSubcategory = {
                        onAddSubcategory(parent)
                    },
                    onDeleteSubcategory = { sub ->
                        onDeleteSubcategory(sub)
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
                    text = tr("Contextos «Onde»", "«Where» Contexts"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(onClick = onAddContext) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Novo Contexto", "New Context"), color = AccentBlue, fontSize = 12.sp)
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

@Composable
internal fun BudgetsGoalsSettingsCard(
    viewModel: AccountViewModel,
    uiState: AccountUiState,
    categoriesById: Map<String, CategoryEntity>,
    onAddBudget: () -> Unit,
    onEditBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit,
    onAddGoal: () -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit
) {
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
                    text = tr("Orçamentos Mensais", "Monthly Budgets"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(onClick = onAddBudget) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Novo Orçamento", "New Budget"), color = AccentBlue, fontSize = 12.sp)
                }
            }

            if (uiState.budgets.isEmpty()) {
                Text(
                    text = tr("Sem orçamentos definidos. Cria um limite mensal por categoria.", "No budgets set. Create a monthly limit per category."),
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else {
                uiState.budgets.forEach { budget ->
                    BudgetRowItem(
                        budget = budget,
                        categoryName = categoriesById[budget.categoryId]?.name ?: budget.categoryId,
                        colorHex = categoriesById[budget.categoryId]?.colorHex ?: 0xFF888888,
                        onEdit = { onEditBudget(budget) },
                        onDelete = { onDeleteBudget(budget) }
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
                    text = tr("Metas de Poupança", "Savings Goals"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(onClick = onAddGoal) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Nova Meta", "New Goal"), color = AccentBlue, fontSize = 12.sp)
                }
            }

            if (uiState.goalItems.isEmpty()) {
                Text(
                    text = tr("Sem metas criadas. Define um alvo de poupança com prazo opcional.", "No goals yet. Set a savings target with an optional deadline."),
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else {
                uiState.goalItems.forEach { item ->
                    GoalRowItem(
                        item = item,
                        onArchive = { viewModel.archiveGoal(item.goal) },
                        onDelete = { onDeleteGoal(item.goal) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun BackupSettingsCard(
    onExportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onRestore: () -> Unit
) {
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
                title = tr("Exportar Backup Completo", "Export Full Backup"),
                subtitle = tr("Cópia completa de categorias, movimentos, orçamentos e metas, cifrada com palavra-passe", "Full copy of categories, transactions, budgets and goals, encrypted with a password"),
                onClick = {
                    onExportBackup()
                }
            )

            HorizontalDivider(color = DarkBorder, thickness = 1.dp)

            // Export CSV Button
            ActionButtonItem(
                icon = Icons.Default.TableChart,
                iconTint = AccentEmerald,
                title = tr("Exportar para Folha de Cálculo (CSV)", "Export to Spreadsheet (CSV)"),
                subtitle = tr("Compatível com Excel em PT-PT com as 4 dimensões completas", "Excel-compatible (PT-PT) with all 4 dimensions"),
                onClick = {
                    onExportCsv()
                }
            )

            HorizontalDivider(color = DarkBorder, thickness = 1.dp)

            // Restore JSON Button
            ActionButtonItem(
                icon = Icons.Default.FileDownload,
                iconTint = Color(0xFFF59E0B),
                title = tr("Restaurar Backup (JSON)", "Restore Backup (JSON)"),
                subtitle = tr("Importação atómica e idempotente de categorias, movimentos, orçamentos e metas", "Atomic, idempotent import of categories, transactions, budgets and goals"),
                onClick = {
                    onRestore()
                }
            )
        }
    }
}

@Composable
internal fun SecuritySettingsCard(
    viewModel: AccountViewModel,
    uiState: AccountUiState,
    onToggleBiometrics: (Boolean) -> Unit
) {
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
                            text = tr("Bloqueio Biométrico", "Biometric Lock"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.isBiometricsEnabled) tr("Ativo ao iniciar a aplicação", "Active when the app starts") else "Desativado (arranque em 0s)",
                            fontSize = 12.sp,
                            color = if (uiState.isBiometricsEnabled) AccentEmerald else TextSecondary
                        )
                    }
                }

                Switch(
                    checked = uiState.isBiometricsEnabled,
                    onCheckedChange = { onToggleBiometrics(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentBlue,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Text(
                text = tr("Por defeito, a aplicação abre instantaneamente em 0 segundos. Ative esta opção se pretender exigir autenticação (biometria ou PIN do telemóvel) sempre que abrir a aplicação ou voltar a ela após 30 segundos.", "By default the app opens instantly. Turn this on to require authentication (biometrics or phone PIN) every time you open the app or return to it after 30 seconds."),
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
internal fun NotificationCaptureCard(viewModel: AccountViewModel) {
    val context = LocalContext.current
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
                            text = tr("Captura Local de Notificações", "Local Notification Capture"),
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
                            text = tr("Permissão do Android Necessária", "Android Permission Required"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )
                        Text(
                            text = tr("Para detetar pagamentos quando recebes notificação do MB WAY ou bancos, ativa o Acesso a Notificações no sistema Android.", "To detect payments when you get MB WAY or bank notifications, enable Notification Access in Android settings."),
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
                            Text(tr("1. Ativar Acesso a Notificações", "1. Enable Notification Access"), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = tr("Se a opção estiver cinzenta (Definições Restritas no Android 13+): vai a Info da Aplicação, toca nos 3 pontos no canto superior direito e seleciona 'Permitir definições restritas'.", "If the option is greyed out (Restricted Settings on Android 13+): go to App Info, tap the 3 dots in the top-right corner and select 'Allow restricted settings'."),
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
                            Text(tr("2. Abrir Info da App (Desbloquear Restritas)", "2. Open App Info (Unlock Restricted Settings)"), color = AccentAmber, fontSize = 11.sp)
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
                            text = tr("Serviço Ativo e Autorizado no Android", "Service Active and Authorised in Android"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentEmerald
                        )
                    }
                }
            }

            Text(
                text = tr("Todas as compras detetadas vão para a 'Fila de Pendentes' no Início, onde podes rever, alterar a categoria/contexto ou descartar antes de guardar.", "All detected purchases go to the 'Pending Queue' on Home, where you can review, change the category/context or discard them before saving."),
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
                    text = tr("Simular Notificação de Teste (12,50 € MB WAY)", "Simulate Test Notification (12,50 € MB WAY)"),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
