package com.financas.app.ui.screens.budgets

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.model.AssetAllocationItem
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowAmberLight
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowCoral
import com.financas.app.ui.theme.FinflowCoralLight
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryContainer
import com.financas.app.ui.theme.FinflowPrimaryLight
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSecondaryLight
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainer
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.theme.FinflowTextInverse
import com.financas.app.ui.i18n.LocalPrivacyMode
import java.util.Locale

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
                                text = tr("Saldo por Alocar", "Unallocated Balance"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowTextPrimary
                            )
                            Text(
                                text = tr("${holding.institution} • Transferência em espera", "${holding.institution} • Pending transfer"),
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
                            text = tr("Alocar", "Allocate"),
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
                            text = tr("Podes alocar o valor total ou dividir por múltiplos ativos", "You can allocate the full amount or split it across several holdings"),
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
                        text = tr("capital investido: ${MoneyCents(portfolio.totalInvestedCents).formatPt(isPrivacyMode)}", "invested capital: ${MoneyCents(portfolio.totalInvestedCents).formatPt(isPrivacyMode)}"),
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
                            text = tr("reforço/mês", "top-up/month"),
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
                        text = tr("Reserva Segura", "Safe Reserve"),
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
                    text = tr("Capital 100% garantido", "100% capital guaranteed"),
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
                        text = tr("Investimentos", "Investments"),
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
                text = tr("Todos (${portfolio.totalAssetsCount})", "All (${portfolio.totalAssetsCount})"),
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
                text = tr("Nova Aplicação", "New Holding"),
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
                text = tr("Importar XTB", "Import XTB"),
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
                    contentDescription = tr("Cotações", "Quotes"),
                    tint = FinflowSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tr("Cotações", "Quotes"),
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
                text = tr("Carteira de Aplicações", "Holdings Portfolio"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
            Text(
                text = tr("${uiState.filteredInvestmentAssets.size} posições ativas", "${uiState.filteredInvestmentAssets.size} active positions"),
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
                        tr("Ainda não registaste nenhuma aplicação financeira.", "You haven't added any financial holdings yet.")
                    else tr("Nenhuma aplicação corresponde ao filtro selecionado.", "No holdings match the selected filter."),
                    fontSize = 13.sp,
                    color = FinflowTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = tr("Adiciona os teus PPRs, ETFs, Certificados ou Depósitos a Prazo para teres a visão 360º da tua riqueza.", "Add your PPRs, ETFs, savings certificates or term deposits to get a 360º view of your wealth."),
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
                                        text = tr("⚡ Alavancado", "⚡ Leveraged"),
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
                            contentDescription = tr("Editar", "Edit"),
                            tint = FinflowTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = tr("Apagar", "Delete"),
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
                        text = tr("investido: ${MoneyCents(asset.investedCents).formatPt(isPrivacyMode)}", "invested: ${MoneyCents(asset.investedCents).formatPt(isPrivacyMode)}"),
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
                                text = tr("Posição Inicial", "Initial Position"),
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
                                    text = tr("Cotação: ${MoneyCents(currPriceCents).formatPt(isPrivacyMode)}", "Quote: ${MoneyCents(currPriceCents).formatPt(isPrivacyMode)}"),
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
                        text = if (isPrivacyMode) tr("Reforço: ${MoneyCents.MASKED_STRING}/mês", "Top-up: ${MoneyCents.MASKED_STRING}/month") else tr("Reforço: +${MoneyCents(asset.monthlyContributionCents).formatPt(false)}/mês", "Top-up: +${MoneyCents(asset.monthlyContributionCents).formatPt(false)}/month"),
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
                            text = tr("+ Reforço", "+ Top-up"),
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
                            text = tr("Alocar", "Allocate"),
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
                            text = tr("Atualizar Saldo", "Update Balance"),
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
