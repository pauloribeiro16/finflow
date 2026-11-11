package com.financas.app.ui.screens.analytics

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.financas.app.ui.theme.FinflowBackground
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryLight
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSecondaryLight
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.components.InteractiveMonthlyBarChart
import com.financas.app.ui.components.MonthlyBarData
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald

// -----------------------------------------------------------------------------
// VISTA ANUAL: Visão Anual Completa (2024, 2025, 2026) com Movimentos Discriminados
// -----------------------------------------------------------------------------
@Composable
internal fun AnualView(
    uiState: AnalyticsUiState,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onOpenMonthInDetail: (Int) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectSubcategory: (String?) -> Unit
) {
    val scrollState = rememberScrollState()
    val annual = uiState.annualAnalysis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinflowBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Year Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            annual.availableYears.forEach { year ->
                val isSelected = year == annual.selectedYear
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) FinflowPrimary else FinflowSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) FinflowPrimary else FinflowBorderSubtle,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectYear(year) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "$year",
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else FinflowTextSecondary
                    )
                }
            }
        }

        // 2. Hero Card: Totais Anuais (Finflow style)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Text(
                            text = "DESPESA TOTAL ANUAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FinflowPrimaryLight)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${annual.selectedYear}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FinflowSecondaryLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tr("Taxa ${annual.formattedSavingsRate}", "Rate ${annual.formattedSavingsRate}"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowSecondary
                        )
                    }
                }

                Text(
                    text = annual.formattedTotalExpense,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextPrimary
                )

                // 3 Highlights: Recebido | Gasto | Poupado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EvolutionMetricCard(
                        title = tr("Recebido", "Received"),
                        value = annual.formattedTotalIncome,
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    EvolutionMetricCard(
                        title = tr("Gasto", "Spent"),
                        value = annual.formattedTotalExpense,
                        accentColor = AccentCoral,
                        modifier = Modifier.weight(1f)
                    )
                    EvolutionMetricCard(
                        title = tr("Poupança", "Savings"),
                        value = annual.formattedNetSavings,
                        accentColor = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Average per active month
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FinflowSurfaceContainerLow)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("Média Mensal de Gastos", "Average Monthly Spending"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FinflowTextSecondary
                        )
                        Text(
                            text = tr("Calculada sobre ${annual.activeMonthsCount} meses com despesas", "Calculated over ${annual.activeMonthsCount} months with expenses"),
                            fontSize = 10.sp,
                            color = FinflowTextMuted
                        )
                    }
                    Text(
                        text = annual.formattedMonthlyAverageExpense,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                }
            }
        }

        // 3. 12-Month Interactive Bar Chart
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
                Column {
                    Text(
                        text = tr("Evolução dos 12 Meses (${annual.selectedYear})", "12-Month Evolution (${annual.selectedYear})"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = tr("Receitas vs. Despesas · Toca num mês para ver detalhes", "Income vs. Expenses · Tap a month to see details"),
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }

                val monthlyData = annual.months.map { m ->
                    MonthlyBarData(
                        yearMonth = m.yearMonth,
                        label = m.monthLabel,
                        incomeCents = m.incomeCents,
                        expenseCents = m.expenseCents,
                        netSavingsCents = m.netSavingsCents,
                        savingsRatePercent = m.savingsRatePercent
                    )
                }

                InteractiveMonthlyBarChart(
                    months = monthlyData,
                    selectedYearMonth = annual.selectedMonthDetail?.yearMonth,
                    onSelectYearMonth = onSelectMonth,
                    monthlyAverageExpenseCents = annual.monthlyAverageExpenseCents,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selected month details
                val selectedMonth = annual.selectedMonthDetail
                if (selectedMonth != null && selectedMonth.hasActivity) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FinflowSurfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedMonth.monthLabel} ${annual.selectedYear}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FinflowTextPrimary
                                )
                                Text(
                                    text = tr("Taxa ${MoneyCents.formatPercent(selectedMonth.savingsRatePercent)}", "Rate ${MoneyCents.formatPercent(selectedMonth.savingsRatePercent)}"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FinflowSecondary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tr("Rec: ${selectedMonth.formattedIncome}", "Inc: ${selectedMonth.formattedIncome}"),
                                    fontSize = 11.sp,
                                    color = FinflowTextSecondary
                                )
                                Text(
                                    text = tr("Gasto: ${selectedMonth.formattedExpense}", "Spent: ${selectedMonth.formattedExpense}"),
                                    fontSize = 11.sp,
                                    color = FinflowTextSecondary
                                )
                                Text(
                                    text = tr("Poup: ${selectedMonth.formattedSavings}", "Sav: ${selectedMonth.formattedSavings}"),
                                    fontSize = 11.sp,
                                    color = FinflowTextSecondary
                                )
                            }

                            Button(
                                onClick = { onOpenMonthInDetail(selectedMonth.yearMonth) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
                            ) {
                                Text(
                                    text = tr("Inspecionar Mês Completo no 'No Quê' ➔", "Inspect the Full Month in 'What' ➔"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Detalhamento Anual por Categoria (Com todos os movimentos discriminados)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("Detalhamento Anual por Categoria (${annual.categories.size})", "Yearly Breakdown by Category (${annual.categories.size})"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        }

        annual.categories.forEach { category ->
            val isExpanded = category.id == annual.selectedCategoryId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectCategory(category.id) },
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
                                    .background(Color(category.colorHex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(category.colorHex))
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = category.name,
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
                                            text = "${MoneyCents.formatPercent(category.percentageOfExpense)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FinflowPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = tr("${category.transactionCount} movimentos no ano", "${category.transactionCount} transactions this year"),
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
                                text = category.formattedValue,
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
                        progress = { (category.percentageOfExpense / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(category.colorHex),
                        trackColor = FinflowSurfaceContainerLow
                    )

                    // Subcategories & Discriminated Transactions Drill-down
                    if (isExpanded && category.subcategories.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FinflowSurfaceContainerLow)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            category.subcategories.forEach { sub ->
                                val isSubExpanded = sub.id == annual.expandedSubcategoryId

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSubExpanded) FinflowSurface else Color.Transparent)
                                        .clickable { onSelectSubcategory(sub.id) }
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
                                                    totalIncomeCents = annual.totalIncomeCents
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
    }
}
