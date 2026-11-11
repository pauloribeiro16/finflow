package com.financas.app.ui.screens.budgets

import com.financas.app.core.i18n.tr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.domain.model.XtbParsedReport
import com.financas.app.ui.theme.FinflowBackground
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowPrimaryContainer
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainer
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings
import com.financas.app.ui.i18n.LocalPrivacyMode

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
                        xtbStatusMessage = tr("Ficheiro XTB não reconhecido ou sem dados.", "XTB file not recognised or empty.")
                    }
                }
            } catch (e: Exception) {
                xtbStatusMessage = tr("Erro ao ler ficheiro: ${e.message}", "Error reading file: ${e.message}")
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
                        Icon(Icons.Default.Close, contentDescription = tr("Fechar", "Close"), tint = FinflowTextSecondary, modifier = Modifier.size(16.dp))
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
                    xtbStatusMessage = tr("Texto XTB não reconhecido ou sem dados.", "XTB text not recognised or empty.")
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
