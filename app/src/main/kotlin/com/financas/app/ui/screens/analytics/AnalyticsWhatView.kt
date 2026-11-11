package com.financas.app.ui.screens.analytics

import com.financas.app.core.i18n.tr

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.financas.app.ui.theme.FinflowBackground
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryLight
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSecondaryLight
import com.financas.app.ui.theme.FinflowCoral
import com.financas.app.ui.theme.FinflowCoralLight
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.components.DonutSlice
import com.financas.app.ui.components.InteractiveDonutCanvas
import com.financas.app.ui.i18n.LocalPrivacyMode

// -----------------------------------------------------------------------------
// VISTA 1: No Quê (Categorias & Subcategorias — Finflow Stitch Design)
// -----------------------------------------------------------------------------
@Composable
internal fun NoQueView(
    uiState: AnalyticsUiState,
    onSelectParentCategory: (String?) -> Unit
) {
    val scrollState = rememberScrollState()
    val topParent = uiState.parentCategories.firstOrNull()
    val comparison = uiState.comparison
    var expandedSubcategoryId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinflowBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Despesa Total + Donut Chart + Legenda Vertical
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FinflowSurface),
            border = BorderStroke(1.dp, FinflowBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tr("Despesa Total", "Total Spending"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FinflowPrimaryLight)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = uiState.monthTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowPrimary
                            )
                        }
                    }

                    val comparison = uiState.comparison
                    val isIncreased = comparison?.expenseDeltaPositive == true
                    val deltaPercentVal = comparison?.expenseDeltaPercent
                    val absDeltaText = deltaPercentVal?.removePrefix("-")?.removePrefix("+") ?: "0,0%"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isIncreased) FinflowCoralLight else FinflowSecondaryLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when {
                                absDeltaText == "0,0%" -> "→ 0,0%"
                                isIncreased -> "↗ +$absDeltaText"
                                else -> "↘ -$absDeltaText"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncreased) FinflowCoral else FinflowSecondary
                        )
                    }
                }

                Text(
                    text = uiState.formattedTotalExpense,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextPrimary
                )

                val isPrivacy = LocalPrivacyMode.current
                Text(
                    text = "${comparison?.expenseDelta ?: (if (isPrivacy) MoneyCents.MASKED_STRING else "0,00 €")} face ao mês anterior",
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )

                // Interactive Donut
                val slices = uiState.parentCategories.map { parent ->
                    DonutSlice(
                        id = parent.id,
                        name = parent.name,
                        amountCents = parent.amountCents,
                        percentage = parent.percentageOfExpense,
                        color = Color(parent.colorHex)
                    )
                }

                val selectedParent = uiState.parentCategories.find { it.id == uiState.selectedParentCategoryId }
                val centerTitle = selectedParent?.name ?: tr("Despesa Total", "Total Spending")
                val centerValue = selectedParent?.formattedValue ?: uiState.formattedTotalExpense
                val centerSubtitle = selectedParent?.let { MoneyCents.formatPercent(it.percentageOfExpense) }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    InteractiveDonutCanvas(
                        slices = slices,
                        selectedSliceId = uiState.selectedParentCategoryId,
                        onSelectSlice = onSelectParentCategory,
                        centerTitle = centerTitle,
                        centerValue = centerValue,
                        centerSubtitle = centerSubtitle,
                        modifier = Modifier.size(220.dp)
                    )
                }

                // Vertical legend with all parent categories
                val maxParentAmount = uiState.parentCategories.maxOfOrNull { it.amountCents } ?: 0L
                if (uiState.parentCategories.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.parentCategories.forEach { parent ->
                            val isSelected = parent.id == uiState.selectedParentCategoryId
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectParentCategory(if (isSelected) null else parent.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(parent.colorHex))
                                    )
                                    Text(
                                        text = parent.name,
                                        fontSize = 12.sp,
                                        color = FinflowTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = parent.formattedValue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FinflowTextPrimary
                                    )
                                    Text(
                                        text = MoneyCents.formatPercent(parent.percentageOfExpense),
                                        fontSize = 11.sp,
                                        color = FinflowTextMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(FinflowSurfaceContainerLow)
                                ) {
                                    val fillRatio = if (maxParentAmount > 0L) {
                                        (parent.amountCents.toFloat() / maxParentAmount.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                    if (fillRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fillRatio)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(parent.colorHex))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Smart Insight Card: Solid Purple container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinflowPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "INSIGHT INTELIGENTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = tr("Hoje", "Today"),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = topParent?.let {
                        tr("A categoria '${it.name}' representa ${MoneyCents.formatPercent(it.percentageOfExpense)} das tuas despesas. Mantém o controlo para atingir a tua meta de poupança!", "The '${it.name}' category is ${MoneyCents.formatPercent(it.percentageOfExpense)} of your spending. Keep it in check to reach your savings goal!")
                    } ?: tr("Continua a registar os teus movimentos diários para gerar análises detalhadas.", "Keep logging your daily transactions to build detailed insights."),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.White
                )
            }
        }

        // Section: Comparativo de Gastos (Histórico Trimestral)
        val quarterlyHistory = uiState.quarterlyHistory
        val maxQuarterlyAmount = (quarterlyHistory.items.maxOfOrNull { it.expenseCents } ?: 1L).coerceAtLeast(1L)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinflowSurface),
            border = BorderStroke(1.dp, FinflowBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("Histórico Trimestral", "Quarterly History"),
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                        Text(
                            text = tr("Comparativo de Gastos", "Spending Comparison"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(FinflowSecondaryLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tr("Média: ${quarterlyHistory.formattedAverageExpense}", "Average: ${quarterlyHistory.formattedAverageExpense}"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowSecondary
                        )
                    }
                }

                // 3 Vertical Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    quarterlyHistory.items.forEach { item ->
                        val barHeightFrac = if (maxQuarterlyAmount > 0L && item.expenseCents > 0L) {
                            (item.expenseCents.toFloat() / maxQuarterlyAmount.toFloat()).coerceIn(0.12f, 1f)
                        } else {
                            0.04f
                        }
                        val barColor = if (item.isCurrent) FinflowPrimary else FinflowSurfaceContainerLow

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = item.formattedExpense,
                                fontSize = 11.sp,
                                fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (item.isCurrent) FinflowPrimary else FinflowTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height((64 * barHeightFrac).dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(barColor)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.isCurrent) FinflowTextPrimary else FinflowTextMuted
                            )
                        }
                    }
                }
            }
        }

        // Section: Detalhamento por Categoria
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Detalhamento por Categoria (${uiState.parentCategories.size})", "Breakdown by Category (${uiState.parentCategories.size})"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        }

        uiState.parentCategories.forEach { parent ->
            val isExpanded = parent.id == uiState.selectedParentCategoryId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectParentCategory(parent.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FinflowSurface),
                border = BorderStroke(1.dp, FinflowBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(parent.colorHex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(parent.colorHex))
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = parent.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FinflowTextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(FinflowPrimaryLight)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${MoneyCents.formatPercent(parent.percentageOfExpense)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FinflowPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = tr("${parent.transactionCount} movimentos registados", "${parent.transactionCount} transactions recorded"),
                                    fontSize = 11.sp,
                                    color = FinflowTextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = parent.formattedValue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowTextPrimary
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                tint = FinflowTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { (parent.percentageOfExpense / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(parent.colorHex),
                        trackColor = FinflowSurfaceContainerLow
                    )

                    // Subcategories & Discriminated Transactions drill-down
                    if (isExpanded && parent.subcategories.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FinflowSurfaceContainerLow)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            parent.subcategories.forEach { sub ->
                                val isSubExpanded = expandedSubcategoryId == sub.id

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSubExpanded) FinflowSurface else Color.Transparent)
                                        .clickable {
                                            expandedSubcategoryId = if (isSubExpanded) null else sub.id
                                        }
                                        .padding(8.dp),
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
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(sub.colorHex))
                                            )
                                            Text(
                                                text = sub.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = FinflowTextPrimary
                                            )
                                            Text(
                                                text = tr("(${sub.transactionCount} mov.)", "(${sub.transactionCount} txns)"),
                                                fontSize = 11.sp,
                                                color = FinflowTextMuted
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = sub.formattedValue,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = FinflowTextPrimary
                                            )
                                            Icon(
                                                imageVector = if (isSubExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = FinflowTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Discriminated individual transactions for this subcategory
                                    if (isSubExpanded && sub.transactions.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            sub.transactions.forEach { tx ->
                                                DiscriminatedTransactionRow(
                                                    transaction = tx,
                                                    categoryName = sub.name,
                                                    displayMode = uiState.displayMode,
                                                    totalIncomeCents = uiState.totalIncomeCents
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons Row (Stitch Bottom Bar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowSurfaceContainerLow)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = FinflowPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("Exportar PDF", "Export PDF"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowPrimary
                )
            }

            Button(
                onClick = { },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("Novo Limite", "New Limit"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
