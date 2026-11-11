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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.usecase.ChartScale
import com.financas.app.ui.components.InteractiveLineChart
import com.financas.app.ui.components.InteractiveMonthlyBarChart
import com.financas.app.ui.components.InteractiveYearComparisonChart
import com.financas.app.ui.components.MonthlyBarData
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
import kotlin.math.roundToLong

// -----------------------------------------------------------------------------
// VISTA 4: Evolução (Histórico Anual Mês a Mês)
// -----------------------------------------------------------------------------
@Composable
internal fun EvolucaoView(
    uiState: AnalyticsUiState,
    onSelectMonth: (Int) -> Unit,
    onSelectEvolutionCategory: (String?) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Annual Evolution Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = tr("Evolução Anual Mês a Mês", "Year Evolution Month by Month"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = tr("Receitas vs. Despesas · Linha tracejada de média mensal", "Income vs. Expenses · Dashed line is the monthly average"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                val monthlyData = uiState.evolutionMonths.map { m ->
                    MonthlyBarData(
                        yearMonth = m.yearMonth,
                        label = m.label,
                        incomeCents = m.incomeCents,
                        expenseCents = m.expenseCents,
                        netSavingsCents = m.netSavingsCents,
                        savingsRatePercent = m.savingsRatePercent
                    )
                }

                InteractiveMonthlyBarChart(
                    months = monthlyData,
                    selectedYearMonth = uiState.selectedEvolutionMonth,
                    onSelectYearMonth = onSelectMonth,
                    monthlyAverageExpenseCents = uiState.historicalMonthlyAverageExpenseCents,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = tr("Arrasta ou toca nos meses para inspecionar os detalhes", "Drag or tap the months to inspect the details"),
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Details of Selected Month Card
        val selectedMonth = uiState.selectedEvolutionMonthItem
        if (selectedMonth != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
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
                            text = tr("Detalhes de ${AnalyticsViewModel.formatMonthTitle(selectedMonth.yearMonth)}", "Details for ${AnalyticsViewModel.formatMonthTitle(selectedMonth.yearMonth)}"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (selectedMonth.deviationVsAveragePercent != null) {
                            val dev = selectedMonth.deviationVsAveragePercent
                            val isAbove = dev > 0.0
                            val devColor = if (isAbove) AccentCoral else AccentEmerald

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAbove) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = devColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${if (isAbove) "+" else ""}${MoneyCents.formatPercent(dev)} vs. média",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = devColor
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EvolutionMetricCard(
                            title = tr("Recebido", "Received"),
                            value = selectedMonth.formattedIncome,
                            accentColor = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        EvolutionMetricCard(
                            title = tr("Gasto", "Spent"),
                            value = selectedMonth.formattedExpense,
                            accentColor = AccentCoral,
                            modifier = Modifier.weight(1f)
                        )
                        EvolutionMetricCard(
                            title = tr("Poupança", "Savings"),
                            value = selectedMonth.formattedSavings,
                            accentColor = AccentBlue,
                            subtitle = tr("${MoneyCents.formatPercent(selectedMonth.savingsRatePercent)} taxa", "${MoneyCents.formatPercent(selectedMonth.savingsRatePercent)} rate"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Historical Average Card (calculated on active months only)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tr("Média Mensal de Gastos", "Average Monthly Spending"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = tr("Calculada apenas sobre meses com atividade", "Calculated only over months with activity"),
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                Text(
                    text = uiState.formattedHistoricalMonthlyAverage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // Category Evolution Line Chart Card
        val categoryTrend = uiState.categoryTrend
        if (categoryTrend != null && uiState.trendChips.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = tr("Evolução de ${categoryTrend.categoryName}", "${categoryTrend.categoryName} over time"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = tr("Gastos mês a mês no ano · Linha tracejada de média", "Monthly spending this year · Dashed line is the average"),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.trendChips.forEach { chip ->
                            val isSelected = chip.selected
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AccentBlue.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentBlue.copy(alpha = 0.5f) else DarkBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectEvolutionCategory(chip.categoryId) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(chip.colorHex))
                                    )
                                    Text(
                                        text = chip.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) AccentBlue else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    val selectedYear = uiState.selectedYearMonth / 100
                    val selectedChartIndex = uiState.selectedEvolutionMonth
                        ?.takeIf { it / 100 == selectedYear }
                        ?.let { (it % 100) - 1 }

                    InteractiveLineChart(
                        points = categoryTrend.points,
                        selectedIndex = selectedChartIndex,
                        onSelectIndex = { index -> onSelectMonth(selectedYear * 100 + index + 1) },
                        averageValue = categoryTrend.averageExpenseCents.toDouble().takeIf { it > 0.0 },
                        averageLabel = tr("Média", "Average"),
                        tickFormatter = { ChartScale.formatCompactCents(it.roundToLong()) },
                        lineColor = Color(categoryTrend.colorHex)
                    )

                    if (categoryTrend.selectedMonthDetail != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Mês selecionado", "Selected month"),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = categoryTrend.selectedMonthDetail,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (categoryTrend.selectedMonthDeviation != null) {
                                    val dev = categoryTrend.selectedMonthDeviation
                                    val isAbove = dev > 0.0
                                    val devColor = if (isAbove) AccentCoral else AccentEmerald
                                    Icon(
                                        imageVector = if (isAbove) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = devColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${if (isAbove) "+" else ""}${MoneyCents.formatPercent(dev)} vs. média",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = devColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Savings Rate Line Chart Card
        val savingsRateTrend = uiState.savingsRateTrend
        if (savingsRateTrend != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = tr("Taxa de Poupança", "Savings Rate"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = tr("Percentagem poupada mês a mês · Meta de 20%", "Share of income saved each month · 20% goal"),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    val selectedYear = uiState.selectedYearMonth / 100
                    val selectedChartIndex = uiState.selectedEvolutionMonth
                        ?.takeIf { it / 100 == selectedYear }
                        ?.let { (it % 100) - 1 }

                    InteractiveLineChart(
                        points = savingsRateTrend.points,
                        selectedIndex = selectedChartIndex,
                        onSelectIndex = { index -> onSelectMonth(selectedYear * 100 + index + 1) },
                        averageValue = savingsRateTrend.averageRatePercent.takeIf { it != 0.0 },
                        averageLabel = tr("Média", "Average"),
                        referenceValue = 20.0,
                        referenceLabel = tr("Meta 20%", "Goal 20%"),
                        tickFormatter = { ChartScale.formatCompactPercent(it) },
                        lineColor = AccentEmerald
                    )

                    Text(
                        text = tr("Média nos meses com atividade: ${savingsRateTrend.formattedAverage}", "Average over months with activity: ${savingsRateTrend.formattedAverage}"),
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Year vs Year Grouped Bars Card
        if (uiState.yearComparison.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = tr("Comparação Anual", "Year-over-Year Comparison"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = tr("Gastos mês a mês por ano", "Monthly spending per year"),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    InteractiveYearComparisonChart(
                        years = uiState.yearComparison,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Year Totals Card
        if (uiState.yearTotals.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = tr("Totais por Ano", "Totals per Year"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = tr("Recebido, gasto e poupança acumulados", "Accumulated income, spending and savings"),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    uiState.yearTotals.forEach { totals ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${totals.year}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = tr("Taxa ${totals.formattedRate}", "Rate ${totals.formattedRate}"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlue
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tr("Rec. ${totals.formattedIncome}", "Inc. ${totals.formattedIncome}"),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = tr("Gasto ${totals.formattedExpense}", "Spent ${totals.formattedExpense}"),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = tr("Poup. ${totals.formattedSavings}", "Sav. ${totals.formattedSavings}"),
                                        fontSize = 11.sp,
                                        color = TextSecondary
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

@Composable
internal fun EvolutionMetricCard(
    title: String,
    value: String,
    accentColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
