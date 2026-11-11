package com.financas.app.ui.screens.budgets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.model.AssetAllocationItem
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.model.XtbParsedReport
import com.financas.app.domain.model.XtbSyncResult
import com.financas.app.domain.usecase.BudgetProgress
import com.financas.app.domain.usecase.BudgetStatus
import com.financas.app.domain.usecase.GoalProgress
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowAmberLight
import com.financas.app.ui.theme.FinflowBackground
import com.financas.app.ui.theme.FinflowBorder
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowCoral
import com.financas.app.ui.theme.FinflowCoralLight
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryContainer
import com.financas.app.ui.theme.FinflowPrimaryLight
import com.financas.app.ui.theme.FinflowPurple
import com.financas.app.ui.theme.FinflowPurpleLight
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSecondaryContainer
import com.financas.app.ui.theme.FinflowSecondaryLight
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainer
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.theme.FinflowTextInverse
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.CategoryTranslator
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings
import com.financas.app.ui.i18n.LocalPrivacyMode
import java.util.Locale
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsGoalsScreen(
    viewModel: BudgetsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val isPrivacyMode = LocalPrivacyMode.current
    val isEnglish = language == AppLanguage.EN
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<InvestmentAssetEntity?>(null) }
    var quickUpdateAsset by remember { mutableStateOf<InvestmentAssetEntity?>(null) }
    var allocatingAsset by remember { mutableStateOf<InvestmentAssetEntity?>(null) }
    var addingMoneyAsset by remember { mutableStateOf<InvestmentAssetEntity?>(null) }
    var showXtbChooserSheet by remember { mutableStateOf(false) }
    var pendingXtbReport by remember { mutableStateOf<XtbParsedReport?>(null) }
    var xtbStatusMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileStore = remember(context) {
        (context.applicationContext as? com.financas.app.FinancasApplication)?.container?.userProfileStore
    }
    var showMarketDisclosure by remember { mutableStateOf(false) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = com.financas.app.ui.util.readTextLimited(context, it)
                if (content.isNotBlank()) {
                    val report = viewModel.parseXtbReport(content)
                    if (report.openPositions.isNotEmpty() || report.cashOperations.isNotEmpty()) {
                        pendingXtbReport = report
                        showXtbChooserSheet = false
                    } else {
                        xtbStatusMessage = "Ficheiro XTB não reconhecido ou sem dados."
                    }
                }
            } catch (e: Exception) {
                xtbStatusMessage = "Erro ao ler ficheiro: ${e.message}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinflowBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Banner (XTB Sync feedback)
        if (xtbStatusMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FinflowSurfaceContainer),
                border = BorderStroke(1.dp, FinflowSecondary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = xtbStatusMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinflowTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { xtbStatusMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = FinflowTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 3-way Segmented Control: [ 🏛️ Investimentos | 🎯 Metas | 📊 Orçamentos ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FinflowSurface)
                .border(1.dp, FinflowBorderSubtle, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WealthTab.entries.forEach { tab ->
                val isSelected = uiState.selectedTab == tab
                val tabBg = if (isSelected) FinflowPrimaryContainer else Color.Transparent
                val tabTextColor = if (isSelected) Color.White else FinflowTextSecondary
                val tabFontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabBg)
                        .clickable { viewModel.selectTab(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.localizedIconLabel(strings),
                        fontSize = 12.sp,
                        fontWeight = tabFontWeight,
                        color = tabTextColor,
                        maxLines = 1
                    )
                }
            }
        }

        // Scrollable Tab Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState.selectedTab) {
                WealthTab.INVESTMENTS -> {
                    InvestmentsTabContent(
                        uiState = uiState,
                        onSelectFilter = { viewModel.selectAssetTypeFilter(it) },
                        onAddNewAsset = { showAddAssetDialog = true },
                        onImportXtb = { showXtbChooserSheet = true },
                        onSyncMarket = {
                            if (profileStore?.isMarketQuotesDisclosureAccepted() == true) {
                                viewModel.syncMarketQuotes()
                            } else {
                                showMarketDisclosure = true
                            }
                        },
                        onEditAsset = { editingAsset = it },
                        onQuickUpdate = { quickUpdateAsset = it },
                        onAllocate = { allocatingAsset = it },
                        onAddMoney = { addingMoneyAsset = it },
                        onDeleteAsset = { viewModel.deleteInvestmentAsset(it.id) }
                    )
                }
                WealthTab.GOALS -> {
                    GoalsTabContent(
                        uiState = uiState,
                        onAddNewGoal = { showAddGoalDialog = true }
                    )
                }
                WealthTab.BUDGETS -> {
                    BudgetsTabContent(
                        uiState = uiState,
                        onAddNewBudget = { showAddBudgetDialog = true },
                        onToggleRollover = { viewModel.toggleRollover() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // BottomSheet: Criar/Editar Aplicação de Investimento
    if (showAddAssetDialog || editingAsset != null) {
        val targetAsset = editingAsset
        AddEditInvestmentSheet(
            initialAsset = targetAsset,
            onDismiss = {
                showAddAssetDialog = false
                editingAsset = null
            },
            onSave = { name, type, inst, investedCents, valueCents, monthlyCents, notes, ticker, units ->
                viewModel.saveInvestmentAsset(
                    id = targetAsset?.id,
                    name = name,
                    type = type,
                    institution = inst,
                    investedCents = investedCents,
                    currentValueCents = valueCents,
                    monthlyContributionCents = monthlyCents,
                    notes = notes,
                    ticker = ticker,
                    units = units
                )
                showAddAssetDialog = false
                editingAsset = null
            }
        )
    }

    if (showMarketDisclosure) {
        MarketQuotesDisclosureDialog(
            onDismiss = { showMarketDisclosure = false },
            onConfirm = { enableDaily ->
                showMarketDisclosure = false
                profileStore?.setMarketQuotesDisclosureAccepted(true)
                profileStore?.setMarketQuotesAutoSyncEnabled(enableDaily)
                viewModel.syncMarketQuotes()
            }
        )
    }

    addingMoneyAsset?.let { target ->
        AddMoneyDialog(
            asset = target,
            onDismiss = { addingMoneyAsset = null },
            onConfirm = { cents ->
                viewModel.addMoneyToAsset(target.id, cents)
                addingMoneyAsset = null
            }
        )
    }

    allocatingAsset?.let { source ->
        AllocateAssetDialog(
            source = source,
            targets = uiState.investmentAssets.filter { it.id != source.id && !it.archived && !InvestmentTransferRules.isHoldingAsset(it) },
            onDismiss = { allocatingAsset = null },
            onConfirm = { targetId, cents ->
                viewModel.allocateAsset(source.id, targetId, cents)
                allocatingAsset = null
            }
        )
    }

    // Dialog: Atualização Rápida de Saldo de Ativo
    if (quickUpdateAsset != null) {
        val asset = quickUpdateAsset!!
        QuickUpdateAssetDialog(
            asset = asset,
            onDismiss = { quickUpdateAsset = null },
            onUpdate = { newCents ->
                viewModel.updateAssetValue(asset.id, newCents)
                quickUpdateAsset = null
            }
        )
    }

    // BottomSheet: Escolher Ficheiro ou Colar Texto XTB
    if (showXtbChooserSheet) {
        XtbImportChooserSheet(
            onDismiss = { showXtbChooserSheet = false },
            onPickFile = {
                filePickerLauncher.launch("*/*")
            },
            onAnalyzeText = { text ->
                val report = viewModel.parseXtbReport(text)
                if (report.openPositions.isNotEmpty() || report.cashOperations.isNotEmpty()) {
                    pendingXtbReport = report
                    showXtbChooserSheet = false
                } else {
                    xtbStatusMessage = "Texto XTB não reconhecido ou sem dados."
                }
            }
        )
    }

    // BottomSheet: Pré-visualização e Confirmação de Sincronização XTB
    if (pendingXtbReport != null) {
        val report = pendingXtbReport!!
        XtbImportPreviewSheet(
            report = report,
            onDismiss = { pendingXtbReport = null },
            onConfirmSync = { importCashOperations ->
                viewModel.importXtbReport(report, importCashOperations) { result ->
                    pendingXtbReport = null
                    xtbStatusMessage = "✅ XTB Sincronizado: ${result.updatedAssetsCount} atualizados, ${result.newAssetsCount} novos, ${result.importedTransactionsCount} movimentos (${result.skippedTransactionsCount} duplicados ignorados)."
                }
            }
        )
    }

    // Dialog: Criar Orçamento de Categoria
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            categories = uiState.allCategories,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { catId, limitCents ->
                viewModel.saveBudget(catId, limitCents)
                showAddBudgetDialog = false
            }
        )
    }

    // Dialog: Criar Meta de Poupança
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, targetCents ->
                viewModel.saveGoal(name, targetCents, null)
                showAddGoalDialog = false
            }
        )
    }
}

// =========================================================================
// ABA 1: INVESTIMENTOS & CARTEIRA
// =========================================================================

@Composable
fun InvestmentsTabContent(
    uiState: BudgetsUiState,
    onSelectFilter: (InvestmentType?) -> Unit,
    onAddNewAsset: () -> Unit,
    onImportXtb: () -> Unit = {},
    onSyncMarket: () -> Unit = {},
    onEditAsset: (InvestmentAssetEntity) -> Unit,
    onQuickUpdate: (InvestmentAssetEntity) -> Unit,
    onAllocate: (InvestmentAssetEntity) -> Unit = {},
    onAddMoney: (InvestmentAssetEntity) -> Unit = {},
    onDeleteAsset: (InvestmentAssetEntity) -> Unit
) {
    val portfolio = uiState.portfolioSummary
    val isPrivacyMode = LocalPrivacyMode.current
    val isProfitPositive = portfolio.totalProfitLossCents >= 0
    val profitColor = if (isProfitPositive) FinflowSecondary else FinflowCoral
    val profitBg = if (isProfitPositive) FinflowSecondaryLight else FinflowCoralLight
    val sign = if (isProfitPositive) "+" else ""

    // Card de Alerta no Topo: Saldo Pendente de Alocação (ex.: XTB)
    if (uiState.unallocatedBalanceCents > 0L && uiState.unallocatedHoldingAsset != null) {
        val holding = uiState.unallocatedHoldingAsset
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinflowSurface),
            border = BorderStroke(1.dp, FinflowAmber)
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FinflowAmberLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = FinflowAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saldo por Alocar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowTextPrimary
                            )
                            Text(
                                text = "${holding.institution} • Transferência em espera",
                                fontSize = 11.sp,
                                color = FinflowTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { onAllocate(holding) },
                        colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Alocar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextInverse
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = MoneyCents(uiState.unallocatedBalanceCents).formatPt(isPrivacyMode),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowAmber
                        )
                        Text(
                            text = "Podes alocar o valor total ou dividir por múltiplos ativos",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                    }
                }
            }
        }
    }

    // Hero Card: PORTFÓLIO APLICADO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = FinflowSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FinflowPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏛️", fontSize = 16.sp)
                    }
                    Text(
                        text = "TOTAL APLICADO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(profitBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPrivacyMode) MoneyCents.MASKED_STRING else "$sign${MoneyCents(portfolio.totalProfitLossCents).formatPt(false)} ($sign${MoneyCents.formatPercent(portfolio.totalProfitLossPercentage)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }

            // Amounts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = MoneyCents(portfolio.totalCurrentValueCents).formatPt(isPrivacyMode),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "capital investido: ${MoneyCents(portfolio.totalInvestedCents).formatPt(isPrivacyMode)}",
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }

                if (portfolio.totalMonthlyContributionCents > 0L) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPrivacyMode) MoneyCents.MASKED_STRING else "+${MoneyCents(portfolio.totalMonthlyContributionCents).formatPt(false)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowPrimary
                        )
                        Text(
                            text = "reforço/mês",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                    }
                }
            }

            // Allocation Bar
            if (portfolio.allocations.isNotEmpty()) {
                AssetAllocationCanvasBar(allocations = portfolio.allocations)
            }
        }
    }

    // Dois Pilares: Reserva de Segurança vs Investimentos de Crescimento
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Pilar 1: Reserva de Segurança (Depósitos a Prazo + Certificados de Aforro)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FinflowSurface),
            border = BorderStroke(1.dp, FinflowBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🛡️", fontSize = 14.sp)
                    Text(
                        text = "Reserva Segura",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                }
                Text(
                    text = MoneyCents(uiState.safeSavingsTotalCents).formatPt(isPrivacyMode),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinflowSecondary
                )
                Text(
                    text = "Capital 100% garantido",
                    fontSize = 10.sp,
                    color = FinflowTextMuted
                )
            }
        }

        // Pilar 2: Investimentos de Longo Prazo (ETFs, PPR, Crypto, Outros)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FinflowSurface),
            border = BorderStroke(1.dp, FinflowBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "📈", fontSize = 14.sp)
                    Text(
                        text = "Investimentos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                }
                Text(
                    text = MoneyCents(uiState.longTermCurrentValueCents).formatPt(isPrivacyMode),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextPrimary
                )
                val ltProfitPositive = uiState.longTermProfitLossCents >= 0
                val ltSign = if (ltProfitPositive) "+" else ""
                Text(
                    text = if (isPrivacyMode) MoneyCents.MASKED_STRING else "$ltSign${MoneyCents(uiState.longTermProfitLossCents).formatPt(false)} ($ltSign${MoneyCents.formatPercent(uiState.longTermProfitLossPct)})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ltProfitPositive) FinflowSecondary else FinflowCoral
                )
            }
        }
    }

    // Filter Chips Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Todos" chip
        val isAllSelected = uiState.selectedAssetTypeFilter == null
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isAllSelected) FinflowPrimaryContainer else FinflowSurface)
                .border(1.dp, if (isAllSelected) FinflowPrimary else FinflowBorderSubtle, RoundedCornerShape(12.dp))
                .clickable { onSelectFilter(null) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Todos (${portfolio.totalAssetsCount})",
                fontSize = 12.sp,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isAllSelected) Color.White else FinflowTextSecondary
            )
        }

        // Each InvestmentType chip
        InvestmentType.entries.forEach { type ->
            val isSelected = uiState.selectedAssetTypeFilter == type
            val countForType = uiState.investmentAssets
                .filter { !it.archived && !InvestmentTransferRules.isHoldingAsset(it) }
                .count { InvestmentType.fromKey(it.type) == type }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) FinflowPrimaryContainer else FinflowSurface)
                    .border(1.dp, if (isSelected) FinflowPrimary else FinflowBorderSubtle, RoundedCornerShape(12.dp))
                .clickable { onSelectFilter(if (isSelected) null else type) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${emojiForInvestmentType(type)} ${type.label} ($countForType)",
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else FinflowTextSecondary
                )
            }
        }
    }

    // Action Buttons: Nova Aplicação & Importar XTB
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onAddNewAsset,
            modifier = Modifier
                .weight(1.15f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimaryContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Nova Aplicação",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        OutlinedButton(
            onClick = onImportXtb,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, FinflowPrimary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = FinflowSurfaceContainerLow)
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = FinflowPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Importar XTB",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinflowPrimary
            )
        }

        OutlinedButton(
            onClick = onSyncMarket,
            enabled = !uiState.isSyncingMarket,
            modifier = Modifier
                .weight(0.9f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, FinflowSecondary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = FinflowSurfaceContainerLow)
        ) {
            if (uiState.isSyncingMarket) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = FinflowSecondary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Cotações",
                    tint = FinflowSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Cotações",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowSecondary
                )
            }
        }
    }

    if (!uiState.lastMarketSyncMessage.isNullOrBlank()) {
        Surface(
            color = FinflowSecondary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📈 ${uiState.lastMarketSyncMessage}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = FinflowSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }

    // Header of List
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Carteira de Aplicações",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
            Text(
                text = "${uiState.filteredInvestmentAssets.size} posições ativas",
                fontSize = 12.sp,
                color = FinflowTextSecondary
            )
        }
    }

    // List of Assets
    if (uiState.filteredInvestmentAssets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(FinflowSurface)
                .border(1.dp, FinflowBorderSubtle, RoundedCornerShape(18.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🏛️",
                    fontSize = 32.sp
                )
                Text(
                    text = if (uiState.investmentAssets.isEmpty())
                        "Ainda não registaste nenhuma aplicação financeira."
                    else "Nenhuma aplicação corresponde ao filtro selecionado.",
                    fontSize = 13.sp,
                    color = FinflowTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Adiciona os teus PPRs, ETFs, Certificados ou Depósitos a Prazo para teres a visão 360º da tua riqueza.",
                    fontSize = 11.sp,
                    color = FinflowTextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        uiState.filteredInvestmentAssets.forEach { asset ->
            InvestmentAssetCard(
                asset = asset,
                onEdit = { onEditAsset(asset) },
                onQuickUpdate = { onQuickUpdate(asset) },
                onAddMoney = { onAddMoney(asset) },
                onAllocate = if (InvestmentTransferRules.isHoldingAsset(asset) && asset.currentValueCents > 0L) {
                    { onAllocate(asset) }
                } else {
                    null
                },
                onDelete = { onDeleteAsset(asset) }
            )
        }
    }
}

@Composable
fun InvestmentAssetCard(
    asset: InvestmentAssetEntity,
    onEdit: () -> Unit,
    onQuickUpdate: () -> Unit,
    onDelete: () -> Unit,
    onAllocate: (() -> Unit)? = null,
    onAddMoney: (() -> Unit)? = null
) {
    val type = InvestmentType.fromKey(asset.type)
    val isPrivacyMode = LocalPrivacyMode.current
    val isProfitPositive = asset.profitLossCents >= 0
    val profitColor = if (isProfitPositive) FinflowSecondary else FinflowCoral
    val profitBg = if (isProfitPositive) FinflowSecondaryLight else FinflowCoralLight
    val sign = if (isProfitPositive) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FinflowSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Type emoji + Name + Institution + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorForInvestmentType(type).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emojiForInvestmentType(type), fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = asset.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = type.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorForInvestmentType(type)
                            )
                            Text(text = "•", fontSize = 11.sp, color = FinflowTextMuted)
                            Text(
                                text = asset.institution,
                                fontSize = 11.sp,
                                color = FinflowTextSecondary
                            )
                            if (com.financas.app.domain.usecase.XtbTickerMapper.isLeveragedInstrument(asset.name, asset.ticker ?: "")) {
                                Text(text = "•", fontSize = 11.sp, color = FinflowTextMuted)
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "⚡ Alavancado",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB74D),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = FinflowTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Apagar",
                            tint = FinflowTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Values Row: Current Value vs Invested
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = MoneyCents(asset.currentValueCents).formatPt(isPrivacyMode),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "investido: ${MoneyCents(asset.investedCents).formatPt(isPrivacyMode)}",
                        fontSize = 11.sp,
                        color = FinflowTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(profitBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPrivacyMode) MoneyCents.MASKED_STRING else "$sign${MoneyCents(asset.profitLossCents).formatPt(false)} ($sign${MoneyCents.formatPercent(asset.profitLossPercentage)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor
                    )
                }
            }

            if (asset.units != null && asset.units > 0.0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FinflowSurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Posição Inicial",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinflowTextSecondary
                            )
                            val avgBuyCents = kotlin.math.round((asset.averageBuyPriceEuros ?: 0.0) * 100.0).toLong()
                            Text(
                                text = "${String.format(Locale.ROOT, "%.4f", asset.units)} un. @ ${MoneyCents(avgBuyCents).formatPt(isPrivacyMode)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FinflowTextPrimary
                            )
                        }
                        if (!asset.ticker.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Yahoo Finance (${asset.ticker})",
                                    fontSize = 10.sp,
                                    color = FinflowPrimary
                                )
                                val currPriceCents = kotlin.math.round((asset.currentPriceEuros ?: 0.0) * 100.0).toLong()
                                Text(
                                    text = "Cotação: ${MoneyCents(currPriceCents).formatPt(isPrivacyMode)}",
                                    fontSize = 10.sp,
                                    color = FinflowTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Footer: Monthly Contribution text, then action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (asset.monthlyContributionCents > 0L) {
                    Text(
                        text = if (isPrivacyMode) "Reforço: ${MoneyCents.MASKED_STRING}/mês" else "Reforço: +${MoneyCents(asset.monthlyContributionCents).formatPt(false)}/mês",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinflowPrimary
                    )
                } else if (asset.notes.isNotBlank()) {
                    Text(
                        text = asset.notes,
                        fontSize = 11.sp,
                        color = FinflowTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                ) {
                if (onAddMoney != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FinflowSecondaryLight)
                            .clickable { onAddMoney() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ Reforço",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                if (onAllocate != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FinflowPrimary)
                            .clickable { onAllocate() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Alocar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextInverse,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FinflowSurfaceContainer)
                        .clickable { onQuickUpdate() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = FinflowPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Atualizar Saldo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun MarketQuotesDisclosureDialog(
    onDismiss: () -> Unit,
    onConfirm: (enableDaily: Boolean) -> Unit
) {
    var enableDaily by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinflowSurface,
        title = { Text("Consultar cotações", fontWeight = FontWeight.Bold, color = FinflowTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Para estimar o valor atual, o Finflow pede ao Yahoo Finance (query1.finance.yahoo.com) a cotação de cada ticker que guardou. O Yahoo fica a saber esses tickers e o endereço IP do telemóvel. Não são enviadas quantidades, valores nem dados pessoais.",
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Atualizar automaticamente uma vez por dia",
                        fontSize = 13.sp,
                        color = FinflowTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = enableDaily, onCheckedChange = { enableDaily = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(enableDaily) },
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text("Consultar", color = FinflowTextInverse, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = FinflowTextSecondary)
            }
        }
    )
}

@Composable
fun AddMoneyDialog(
    asset: InvestmentAssetEntity,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val amountCents = parseEuroToCents(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinflowSurface,
        title = {
            Column {
                Text("Reforçar investimento", fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                Text(text = asset.name, fontSize = 12.sp, color = FinflowPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Soma ao capital investido e ao valor atual, ficando registado como transferência (não conta como despesa).",
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor do reforço (€)", color = FinflowTextSecondary) },
                    placeholder = { Text("ex.: 100,00", color = FinflowTextMuted) },
                    trailingIcon = {
                        Text(
                            text = "€",
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextSecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FinflowTextPrimary,
                        unfocusedTextColor = FinflowTextPrimary,
                        focusedBorderColor = FinflowPrimary,
                        unfocusedBorderColor = FinflowBorder,
                        focusedLabelColor = FinflowPrimary,
                        unfocusedLabelColor = FinflowTextSecondary,
                        focusedContainerColor = FinflowSurface,
                        unfocusedContainerColor = FinflowSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountCents) },
                enabled = amountCents > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text("Reforçar", color = FinflowTextInverse, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = FinflowTextSecondary)
            }
        }
    )
}

@Composable
fun AllocateAssetDialog(
    source: InvestmentAssetEntity,
    targets: List<InvestmentAssetEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    val maxCents = source.currentValueCents
    var selectedTargetId by remember { mutableStateOf<String?>(null) }
    var amountText by remember {
        mutableStateOf(MoneyCents(maxCents).formatPt().replace("€", "").trim())
    }
    val amountCents = parseEuroToCents(amountText)
    val isValid = selectedTargetId != null && amountCents > 0L && amountCents <= maxCents
    val selectedTarget = targets.firstOrNull { it.id == selectedTargetId }
    val isPrivacyMode = LocalPrivacyMode.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinflowSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Alocar Saldo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FinflowTextPrimary)
                Text(
                    text = "${source.name} · Saldo disponível: ${MoneyCents(maxCents).formatPt(isPrivacyMode)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinflowPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Escolhe o ativo de destino:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowTextSecondary
                )
                if (targets.isEmpty()) {
                    Text(
                        text = "Ainda não tens outros ativos criados. Adiciona primeiro uma aplicação (ex.: um ETF ou PPR) em \"Nova Aplicação\".",
                        fontSize = 12.sp,
                        color = FinflowTextMuted
                    )
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        targets.forEach { target ->
                            val isSelected = selectedTargetId == target.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) FinflowPrimary else FinflowSurfaceContainer)
                                    .clickable { selectedTargetId = target.id }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = target.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) FinflowTextInverse else FinflowTextPrimary
                                    )
                                    Text(
                                        text = target.institution,
                                        fontSize = 11.sp,
                                        color = if (isSelected) FinflowTextInverse.copy(alpha = 0.8f) else FinflowTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor a alocar (€)", color = FinflowTextSecondary) },
                    placeholder = { Text("Insere qualquer quantia", color = FinflowTextMuted) },
                    trailingIcon = {
                        Text(
                            text = "€",
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextSecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FinflowTextPrimary,
                        unfocusedTextColor = FinflowTextPrimary,
                        focusedBorderColor = FinflowPrimary,
                        unfocusedBorderColor = FinflowBorder,
                        focusedLabelColor = FinflowPrimary,
                        unfocusedLabelColor = FinflowTextSecondary,
                        focusedContainerColor = FinflowSurface,
                        unfocusedContainerColor = FinflowSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Atalhos modulares rápidos: Percentagens calculadas dinamicamente
                val pct25 = MoneyCents(maxCents / 4L).formatPt().replace("€", "").trim()
                val pct50 = MoneyCents(maxCents / 2L).formatPt().replace("€", "").trim()
                val pct75 = MoneyCents((maxCents * 3L) / 4L).formatPt().replace("€", "").trim()
                val pct100 = MoneyCents(maxCents).formatPt().replace("€", "").trim()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "25%" to pct25,
                        "50%" to pct50,
                        "75%" to pct75,
                        "Tudo" to pct100
                    ).forEach { (label, value) ->
                        val isPicked = amountText.trim() == value.trim()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPicked) FinflowPrimary else FinflowSurfaceContainer)
                                .clickable { amountText = value }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPicked) FinflowTextInverse else FinflowTextPrimary
                            )
                        }
                    }
                }

                // Feedback em tempo real sobre a alocação e o remanescente
                if (amountCents > maxCents) {
                    Text(
                        text = "O valor excede o saldo disponível de ${MoneyCents(maxCents).formatPt(isPrivacyMode)}.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinflowCoral
                    )
                } else if (amountCents in 1L until maxCents) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "A alocar: ${MoneyCents(amountCents).formatPt(isPrivacyMode)}${if (selectedTarget != null) " ➔ ${selectedTarget.name}" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowTextPrimary
                        )
                        Text(
                            text = "Ficarão ${MoneyCents(maxCents - amountCents).formatPt(isPrivacyMode)} em espera para alocares depois.",
                            fontSize = 11.sp,
                            color = FinflowAmber
                        )
                    }
                } else if (amountCents == maxCents && maxCents > 0L) {
                    Text(
                        text = "Todo o saldo disponível (${MoneyCents(maxCents).formatPt(isPrivacyMode)}) será alocado.",
                        fontSize = 11.sp,
                        color = FinflowSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedTargetId?.let { onConfirm(it, amountCents) } },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinflowPrimary,
                    disabledContainerColor = FinflowSurfaceContainer
                )
            ) {
                Text(
                    text = "Confirmar Alocação",
                    color = if (isValid) FinflowTextInverse else FinflowTextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = FinflowTextSecondary)
            }
        }
    )
}

@Composable
fun AssetAllocationCanvasBar(
    allocations: List<AssetAllocationItem>,
    modifier: Modifier = Modifier
) {
    if (allocations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            val totalWidth = size.width
            val totalHeight = size.height
            var currentX = 0f

            allocations.forEach { item ->
                val segmentWidth = (totalWidth * (item.percentage.toFloat() / 100f)).coerceAtLeast(0f)
                if (segmentWidth > 0f) {
                    drawRect(
                        color = colorForInvestmentType(item.type),
                        topLeft = Offset(currentX, 0f),
                        size = Size(segmentWidth, totalHeight)
                    )
                    currentX += segmentWidth
                }
            }
        }

        // Legend row with horizontal scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allocations.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colorForInvestmentType(item.type))
                    )
                    Text(
                        text = "${item.type.label}: ${MoneyCents.formatPercent(item.percentage)}",
                        fontSize = 11.sp,
                        color = FinflowTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun colorForInvestmentType(type: InvestmentType): Color {
    return when (type) {
        InvestmentType.PPR -> Color(0xFF00E5FF) // Finflow Cyan
        InvestmentType.DEPOSITO_PRAZO -> Color(0xFF10B981) // Emerald Green
        InvestmentType.ETF -> Color(0xFFA855F7) // Purple
        InvestmentType.CERTIFICADOS_AFORRO -> Color(0xFFF59E0B) // Amber
        InvestmentType.CRYPTO -> Color(0xFFEC4899) // Pink
        InvestmentType.OUTRO -> Color(0xFF94A3B8) // Slate Gray
    }
}

fun emojiForInvestmentType(type: InvestmentType): String {
    return when (type) {
        InvestmentType.PPR -> "🏛️"
        InvestmentType.DEPOSITO_PRAZO -> "🏦"
        InvestmentType.ETF -> "📈"
        InvestmentType.CERTIFICADOS_AFORRO -> "📜"
        InvestmentType.CRYPTO -> "🪙"
        InvestmentType.OUTRO -> "💎"
    }
}

// =========================================================================
// ABA 2: METAS DE POUPANÇA
// =========================================================================

@Composable
fun GoalsTabContent(
    uiState: BudgetsUiState,
    onAddNewGoal: () -> Unit
) {
    val isPrivacyMode = LocalPrivacyMode.current
    // Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Metas de Poupança",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
            Text(
                text = "Crescimento de património e objetivos de vida",
                fontSize = 12.sp,
                color = FinflowTextSecondary
            )
        }

        TextButton(onClick = onAddNewGoal) {
            Text(
                text = "+ Nova Meta",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowPrimary
            )
        }
    }

    if (uiState.goals.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FinflowSurface)
                .border(1.dp, FinflowBorderSubtle, RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Adiciona metas como Fundo de Emergência ou Férias.",
                fontSize = 13.sp,
                color = FinflowTextSecondary
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.goals.forEach { goal ->
                GoalMiniCard(
                    goal = goal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =========================================================================
// ABA 3: ORÇAMENTOS MENSAIS
// =========================================================================

@Composable
fun BudgetsTabContent(
    uiState: BudgetsUiState,
    onAddNewBudget: () -> Unit,
    onToggleRollover: () -> Unit
) {
    val isPrivacyMode = LocalPrivacyMode.current
    // Hero Card: ORÇAMENTO MENSAL GLOBAL
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = FinflowSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FinflowPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = FinflowPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "ORÇAMENTO GLOBAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                // Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (uiState.percentUsed > 100.0) FinflowCoralLight else FinflowSecondaryLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "• ${MoneyCents.formatPercent(uiState.percentUsed)} Usado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.percentUsed > 100.0) FinflowCoral else FinflowSecondary
                    )
                }
            }

            // Spent vs Remaining row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = MoneyCents(uiState.totalSpentCents).formatPt(isPrivacyMode),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "teto planeado de ${MoneyCents(uiState.totalLimitCents).formatPt(isPrivacyMode)}",
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyCents(uiState.totalRemainingCents).formatPt(isPrivacyMode),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.totalRemainingCents < 0) FinflowCoral else FinflowSecondary
                    )
                    Text(
                        text = if (uiState.totalRemainingCents < 0) "Excedido" else "Disponível",
                        fontSize = 12.sp,
                        color = FinflowTextMuted
                    )
                }
            }

            // Progress Bar
            val progressFrac = if (uiState.totalLimitCents > 0L) {
                (uiState.totalSpentCents.toFloat() / uiState.totalLimitCents.toFloat()).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progressFrac },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (uiState.percentUsed > 100.0) FinflowCoral else FinflowPrimary,
                trackColor = FinflowSurfaceContainerLow
            )

            // Subtext Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = FinflowPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Faltam ${uiState.daysRemainingInMonth} dias para o fim do mês",
                        fontSize = 11.sp,
                        color = FinflowTextSecondary
                    )
                }

                Text(
                    text = if (isPrivacyMode) "Média: ${MoneyCents.MASKED_STRING}/dia" else "Média: ${MoneyCents(uiState.dailyBudgetRemainingCents).formatPt(false)}/dia",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowTextSecondary
                )
            }
        }
    }

    // Primary Button: Novo Orçamento de Categoria
    Button(
        onClick = onAddNewBudget,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimaryContainer)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Novo Orçamento de Categoria",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }

    // Section: Categorias Ativas
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Categorias Ativas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
            Text(
                text = "${uiState.categoryBudgets.size} orçamentos sob monitorização",
                fontSize = 12.sp,
                color = FinflowTextSecondary
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Filtrar",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FinflowPrimary
            )
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = FinflowPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }

    // List of Active Category Budget Cards
    if (uiState.categoryBudgets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FinflowSurface)
                .border(1.dp, FinflowBorderSubtle, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ainda não tens orçamentos configurados para este mês.",
                fontSize = 13.sp,
                color = FinflowTextSecondary
            )
        }
    } else {
        uiState.categoryBudgets.forEach { budgetItem ->
            CategoryBudgetCard(budgetItem = budgetItem)
        }
    }

    // Section: Regras & Automatismos
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FinflowSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FinflowPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FinflowPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Regras & Automatismos",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "Gestão inteligente de excedentes",
                        fontSize = 11.sp,
                        color = FinflowTextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transição Automática (Rollover)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "Transferir sobra do mês anterior para o orçamento seguinte.",
                        fontSize = 11.sp,
                        color = FinflowTextSecondary
                    )
                }
                Switch(
                    checked = uiState.rolloverEnabled,
                    onCheckedChange = { onToggleRollover() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FinflowPrimary
                    )
                )
            }
        }
    }
}

// =========================================================================
// CARDS & DIALOGS EXISTENTES
// =========================================================================

@Composable
fun CategoryBudgetCard(budgetItem: BudgetProgress) {
    val isPrivacyMode = LocalPrivacyMode.current
    val statusColor = when (budgetItem.status) {
        BudgetStatus.EXCEEDED -> FinflowCoral
        BudgetStatus.WARNING -> FinflowAmber
        BudgetStatus.OK -> FinflowSecondary
    }

    val statusBg = when (budgetItem.status) {
        BudgetStatus.EXCEEDED -> FinflowCoralLight
        BudgetStatus.WARNING -> FinflowAmberLight
        BudgetStatus.OK -> FinflowSecondaryLight
    }

    val statusLabel = when (budgetItem.status) {
        BudgetStatus.EXCEEDED -> "Ultrapassado"
        BudgetStatus.WARNING -> "Atenção"
        BudgetStatus.OK -> "No Plano"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinflowSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(budgetItem.colorHex).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForCategory(budgetItem.icon),
                            contentDescription = null,
                            tint = Color(budgetItem.colorHex),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = budgetItem.categoryName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${MoneyCents(budgetItem.spentCents).formatPt(isPrivacyMode)} de ${MoneyCents(budgetItem.limitCents).formatPt(isPrivacyMode)}",
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
                Text(
                    text = "${MoneyCents.formatPercent(budgetItem.percentUsed)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            val progress = (budgetItem.percentUsed / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = FinflowSurfaceContainerLow
            )
        }
    }
}

@Composable
fun GoalMiniCard(
    goal: GoalProgress,
    modifier: Modifier = Modifier
) {
    val isComplete = goal.percentComplete >= 100.0
    val badgeBg = if (isComplete) FinflowSecondaryLight else FinflowPrimaryContainer.copy(alpha = 0.35f)
    val badgeColor = if (isComplete) FinflowSecondary else FinflowPrimary
    val isPrivacyMode = LocalPrivacyMode.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinflowSurface),
        border = BorderStroke(1.dp, FinflowBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon + Title/Deadline + Percentage Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FinflowSecondaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎯", fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val deadlineText = if (goal.deadlineYearMonth != null) {
                            val ym = YearMonth.of(goal.deadlineYearMonth / 100, goal.deadlineYearMonth % 100)
                            "Prazo: ${ym.monthValue.toString().padStart(2, '0')}/${ym.year}"
                        } else {
                            "Sem data limite"
                        }
                        Text(
                            text = deadlineText,
                            fontSize = 11.sp,
                            color = FinflowTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Percentage Badge Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isComplete) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "${MoneyCents.formatPercent(goal.percentComplete, 0)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Values & Remaining Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = MoneyCents(goal.accumulatedSavingsCents).formatPt(isPrivacyMode),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "de ${MoneyCents(goal.targetCents).formatPt(isPrivacyMode)}",
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val remainingCents = (goal.targetCents - goal.accumulatedSavingsCents).coerceAtLeast(0L)
                    if (isComplete) {
                        Text(
                            text = "Concluída!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowSecondary
                        )
                        Text(
                            text = "Objetivo atingido",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                    } else {
                        Text(
                            text = "Faltam ${MoneyCents(remainingCents).formatPt(isPrivacyMode)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowTextPrimary
                        )
                        if (goal.monthsRemaining != null) {
                            Text(
                                text = "${goal.monthsRemaining} meses restantes",
                                fontSize = 11.sp,
                                color = if (goal.onTrack == false) FinflowAmber else FinflowTextSecondary
                            )
                        }
                    }
                }
            }

            // Progress Bar
            val progress = (goal.percentComplete / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isComplete) FinflowSecondary else FinflowPrimary,
                trackColor = FinflowSurfaceContainerLow
            )

            // Optional Footer: Monthly Pace
            if (goal.averageMonthlySavingsCents > 0L || goal.onTrack != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (goal.averageMonthlySavingsCents > 0L) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = FinflowTextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isPrivacyMode) "Ritmo médio: ${MoneyCents.MASKED_STRING}/mês" else "Ritmo médio: +${MoneyCents(goal.averageMonthlySavingsCents).formatPt(false)}/mês",
                                fontSize = 11.sp,
                                color = FinflowTextMuted
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (goal.onTrack != null && !isComplete) {
                        val trackText = if (goal.onTrack == true) "No ritmo planeado" else "Abaixo do ritmo"
                        val trackColor = if (goal.onTrack == true) FinflowSecondary else FinflowAmber
                        Text(
                            text = trackText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = trackColor
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// BOTTOMSHEET / DIALOGS
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInvestmentSheet(
    initialAsset: InvestmentAssetEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: String,
        institution: String,
        investedCents: Long,
        currentValueCents: Long,
        monthlyContributionCents: Long,
        notes: String,
        ticker: String?,
        units: Double?
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialAsset?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialAsset?.let { InvestmentType.fromKey(it.type) } ?: InvestmentType.PPR) }
    var institution by remember { mutableStateOf(initialAsset?.institution ?: "") }
    var investedText by remember { mutableStateOf(initialAsset?.let { MoneyCents(it.investedCents).formatPt().replace("€", "").trim() } ?: "") }
    var currentValueText by remember { mutableStateOf(initialAsset?.let { MoneyCents(it.currentValueCents).formatPt().replace("€", "").trim() } ?: "") }
    var monthlyText by remember { mutableStateOf(initialAsset?.let { if (it.monthlyContributionCents > 0) MoneyCents(it.monthlyContributionCents).formatPt().replace("€", "").trim() else "" } ?: "") }
    var ticker by remember { mutableStateOf(initialAsset?.ticker ?: "") }
    var unitsText by remember { mutableStateOf(initialAsset?.units?.let { String.format(Locale.ROOT, "%.4f", it) } ?: "") }
    var notes by remember { mutableStateOf(initialAsset?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FinflowSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialAsset == null) "Nova Aplicação / Ativo" else "Editar Aplicação",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "Regista os teus investimentos (PPR, ETFs, Contas a Prazo)",
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FinflowSurfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = FinflowTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 1. Tipo de Ativo
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de Ativo",
                    fontSize = 13.sp,
                    color = FinflowTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InvestmentType.entries.forEach { typeOption ->
                        val isSelected = selectedType == typeOption
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) FinflowPrimaryContainer else FinflowSurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) FinflowPrimary else FinflowBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedType = typeOption }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${emojiForInvestmentType(typeOption)} ${typeOption.label}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else FinflowTextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Nome da Aplicação
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                isError = nameError,
                label = { Text("Nome da Aplicação *") },
                placeholder = { Text("Ex: PPR Agressivo, Core S&P 500 ETF, DP 3.5%") },
                supportingText = if (nameError) {
                    { Text("O nome da aplicação é obrigatório", color = FinflowCoral) }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Entidade / Banco / Corretora
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Entidade / Corretora / Banco") },
                    placeholder = { Text("Ex: Optimize, XTB, CGD, Trade Republic, IGCP") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Optimize", "XTB", "CGD", "Trade Republic", "Banco Invest", "IGCP / CTT").forEach { inst ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FinflowSurfaceContainerLow)
                                .border(1.dp, FinflowBorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { institution = inst }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = inst,
                                fontSize = 11.sp,
                                color = FinflowTextSecondary
                            )
                        }
                    }
                }
            }

            // 4. Montantes: Capital Investido e Valor Atual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = investedText,
                    onValueChange = { investedText = it },
                    label = { Text("Capital Investido (€)") },
                    placeholder = { Text("Ex: 1000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currentValueText,
                    onValueChange = { currentValueText = it },
                    label = { Text("Saldo Atual (€)") },
                    placeholder = { Text("Ex: 1150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 5. Reforço Mensal Opcional
            OutlinedTextField(
                value = monthlyText,
                onValueChange = { monthlyText = it },
                label = { Text("Reforço Mensal (€/mês) — Opcional") },
                placeholder = { Text("Ex: 200 (para PPR Agressivo)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Ticker Yahoo Finance & Quantidade de Unidades
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it.uppercase() },
                    label = { Text("Ticker (Yahoo)") },
                    placeholder = { Text("ex: LQQ.PA") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it.replace(',', '.') },
                    label = { Text("Unidades / Cotas") },
                    placeholder = { Text("ex: 26.2636") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 7. Notas
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas Adicionais — Opcional") },
                placeholder = { Text("Benefício fiscal, maturidade, etc.") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Botão Guardar em Destaque
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val investedCents = parseEuroToCents(investedText)
                    val rawValueCents = parseEuroToCents(currentValueText)
                    val valueCents = if (rawValueCents > 0L) rawValueCents else investedCents
                    val finalInvestedCents = if (investedCents > 0L) investedCents else valueCents
                    val monthlyCents = parseEuroToCents(monthlyText)
                    val finalTicker = ticker.trim().takeIf { it.isNotBlank() }
                    val finalUnits = unitsText.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
                    onSave(
                        name.trim(),
                        selectedType.key,
                        institution.trim().ifBlank { "Geral" },
                        finalInvestedCents,
                        valueCents,
                        monthlyCents,
                        notes.trim(),
                        finalTicker,
                        finalUnits
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text(
                    text = if (initialAsset == null) "Guardar Aplicação" else "Guardar Alterações",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun QuickUpdateAssetDialog(
    asset: InvestmentAssetEntity,
    onDismiss: () -> Unit,
    onUpdate: (Long) -> Unit
) {
    var newAmountText by remember {
        mutableStateOf(MoneyCents(asset.currentValueCents).formatPt().replace("€", "").trim())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Atualizar Saldo",
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextPrimary
                )
                Text(
                    text = asset.name,
                    fontSize = 13.sp,
                    color = FinflowPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Valor registado anterior: ${MoneyCents(asset.currentValueCents).formatPt()}",
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
                OutlinedTextField(
                    value = newAmountText,
                    onValueChange = { newAmountText = it },
                    label = { Text("Novo Valor de Mercado (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = parseEuroToCents(newAmountText)
                    if (cents >= 0L) {
                        onUpdate(cents)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text("Atualizar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = FinflowTextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, limitCents: Long) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var limitText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Novo Orçamento",
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Selecionar Categoria",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Teto Mensal (€)") },
                    placeholder = { Text("Ex: 350") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cat = selectedCategory
                    val cents = parseEuroToCents(limitText)
                    if (cat != null && cents > 0) {
                        onConfirm(cat.id, cents)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text("Criar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = FinflowTextSecondary)
            }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetCents: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova Meta de Poupança",
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Meta") },
                    placeholder = { Text("Ex: Férias de Verão") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Montante Alvo (€)") },
                    placeholder = { Text("Ex: 1500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = parseEuroToCents(targetText)
                    if (name.isNotBlank() && cents > 0) {
                        onConfirm(name.trim(), cents)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text("Criar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = FinflowTextSecondary)
            }
        }
    )
}

fun parseEuroToCents(text: String): Long {
    val clean = text.replace("€", "").replace("+", "").replace(Regex("\\s+"), "").trim()
    if (clean.isBlank()) return 0L

    val normalized = if (clean.contains(".") && clean.contains(",")) {
        if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
            // Portuguese format: 1.250,50 -> 1250.50
            clean.replace(".", "").replace(",", ".")
        } else {
            // US format: 1,250.50 -> 1250.50
            clean.replace(",", "")
        }
    } else if (clean.contains(",")) {
        // Only comma: 1250,50 -> 1250.50
        clean.replace(",", ".")
    } else if (clean.contains(".")) {
        val parts = clean.split(".")
        if (parts.size > 2) {
            // Multiple dots: 1.000.000 -> 1000000
            clean.replace(".", "")
        } else if (parts[1].length == 3 && parts[1].all { it.isDigit() }) {
            // Single dot with exactly 3 digits in PT context: 2.400 -> 2400 or 9.500 -> 9500
            clean.replace(".", "")
        } else {
            // US decimal dot: 2400.50 or 24.5
            clean
        }
    } else {
        clean
    }

    val value = normalized.toDoubleOrNull() ?: 0.0
    return Math.round(value * 100.0)
}

fun iconForCategory(iconName: String): ImageVector {
    return when (iconName) {
        "shopping_cart", "supermarket" -> Icons.Default.ShoppingBag
        "restaurant", "food" -> Icons.Default.Restaurant
        "car", "transport" -> Icons.Default.DirectionsCar
        "home", "house" -> Icons.Default.Home
        "airplane", "travel" -> Icons.Default.AirplanemodeActive
        "security", "health" -> Icons.Default.Security
        "person", "personal" -> Icons.Default.Person
        else -> Icons.Default.AccountBalanceWallet
    }
}

// =========================================================================
// BOTTOMSHEET: IMPORTAR XTB
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtbImportChooserSheet(
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onAnalyzeText: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FinflowSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Importar Relatório XTB",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "Processado no telemóvel. Atualiza cotações e posições sem duplicar.",
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(FinflowSurfaceContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = FinflowTextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Opção 1: Escolher Ficheiro
            Button(
                onClick = onPickFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimaryContainer)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escolher Ficheiro XTB (.csv / .xlsx / .txt)", fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(FinflowBorderSubtle))
                Text("OU COLAR TEXTO DO RELATÓRIO", fontSize = 11.sp, color = FinflowTextSecondary, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(FinflowBorderSubtle))
            }

            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it },
                placeholder = { Text("Cole aqui o texto CSV exportado da XTB...", fontSize = 12.sp, color = FinflowTextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { if (rawText.isNotBlank()) onAnalyzeText(rawText) },
                enabled = rawText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowSecondary)
            ) {
                Text("Analisar Texto Colado", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtbImportPreviewSheet(
    report: XtbParsedReport,
    onDismiss: () -> Unit,
    onConfirmSync: (importCashOperations: Boolean) -> Unit
) {
    var importCashOps by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FinflowSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 Relatório XTB Detetado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "${report.openPositions.size} posições abertas encontradas",
                        fontSize = 12.sp,
                        color = FinflowSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(FinflowSurfaceContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = FinflowTextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Resumo dos Totais XTB
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FinflowSurfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val isPrivacy = LocalPrivacyMode.current
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Valor de Mercado", fontSize = 11.sp, color = FinflowTextSecondary)
                        Text(if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(report.openPositionValueCents, false), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Lucro Total", fontSize = 11.sp, color = FinflowTextSecondary)
                        val isPos = report.openPositionProfitCents >= 0
                        val pColor = if (isPos) FinflowSecondary else FinflowCoral
                        val pSign = if (isPos) "+" else ""
                        Text(if (isPrivacy) MoneyCents.MASKED_STRING else "$pSign${MoneyCents.formatCents(report.openPositionProfitCents, false)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pColor)
                    }
                }
            }

            // Lista das posições detetadas
            Text("Posições a Sincronizar:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FinflowTextPrimary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPrivacy = LocalPrivacyMode.current
                report.openPositions.forEach { pos ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FinflowSurfaceContainerLow, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pos.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FinflowTextPrimary)
                            Text("${pos.ticker} • Vol: ${pos.volume}", fontSize = 11.sp, color = FinflowTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(pos.currentValueCents, false), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                            val isPos = pos.profitLossCents >= 0
                            val pColor = if (isPos) FinflowSecondary else FinflowCoral
                            val pSign = if (isPos) "+" else ""
                            Text(if (isPrivacy) MoneyCents.MASKED_STRING else "$pSign${MoneyCents.formatCents(pos.profitLossCents, false)} ($pSign${MoneyCents.formatPercent(pos.profitLossPercentage)})", fontSize = 11.sp, color = pColor)
                        }
                    }
                }
            }

            // Secção de Operações de Caixa
            if (report.cashOperations.isNotEmpty()) {
                val dividendsAndInterest = report.cashOperations.filter {
                    it.type.contains("Dividend", ignoreCase = true) || it.type.contains("Interest", ignoreCase = true)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FinflowSurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Registar Dividendos e Juros", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                                Text("${dividendsAndInterest.size} movimentos detetados (com anti-duplicação)", fontSize = 11.sp, color = FinflowTextSecondary)
                            }
                            Switch(
                                checked = importCashOps,
                                onCheckedChange = { importCashOps = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FinflowSecondary)
                            )
                        }
                    }
                }
            }

            // Botão Confirmar Sincronização
            Button(
                onClick = { onConfirmSync(importCashOps) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimaryContainer)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Sincronização", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
