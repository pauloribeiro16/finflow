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
import com.financas.app.domain.usecase.CalendarDayData
import com.financas.app.ui.components.MonthlyCalendarGrid
import com.financas.app.ui.screens.home.TransactionRowItem
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary

// -----------------------------------------------------------------------------
// VISTA 5: Comparar (Mês Atual vs. Base de Comparação)
// -----------------------------------------------------------------------------
@Composable
internal fun CompararView(
    uiState: AnalyticsUiState,
    onSelectBaseline: (ComparisonBaseline) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Baseline selector pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonBaseline.entries.forEach { baseline ->
                val isSelected = baseline == uiState.comparisonBaseline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentBlue.copy(alpha = 0.15f) else DarkSurfaceVariant)
                        .border(
                            width = if (isSelected) 1.dp else 1.dp,
                            color = if (isSelected) AccentBlue.copy(alpha = 0.5f) else DarkBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectBaseline(baseline) }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = baseline.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AccentBlue else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        val comparison = uiState.comparison
        if (comparison == null) {
            Text(
                text = tr("Sem dados para comparar com esta base.", "No data to compare against this baseline."),
                fontSize = 12.sp,
                color = TextMuted
            )
        } else {
            // Totals delta card
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
                            text = tr("Comparação com ${comparison.baselineLabel}", "Comparison with ${comparison.baselineLabel}"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = tr("Deltas do mês selecionado face à base", "Changes in the selected month vs. the baseline"),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    ComparisonDeltaRow(
                        label = tr("Gasto", "Spent"),
                        current = comparison.currentExpense,
                        baseline = comparison.baselineExpense,
                        delta = comparison.expenseDelta,
                        deltaPercent = comparison.expenseDeltaPercent,
                        positive = comparison.expenseDeltaPositive,
                        positiveColor = AccentCoral,
                        negativeColor = AccentEmerald
                    )
                    ComparisonDeltaRow(
                        label = tr("Recebido", "Received"),
                        current = comparison.currentIncome,
                        baseline = comparison.baselineIncome,
                        delta = comparison.incomeDelta,
                        deltaPercent = comparison.incomeDeltaPercent,
                        positive = comparison.incomeDeltaPositive,
                        positiveColor = AccentEmerald,
                        negativeColor = AccentCoral
                    )
                    ComparisonDeltaRow(
                        label = tr("Poupança", "Savings"),
                        current = comparison.currentSavings,
                        baseline = comparison.baselineSavings,
                        delta = comparison.savingsDelta,
                        deltaPercent = comparison.savingsDeltaPercent,
                        positive = comparison.savingsDeltaPositive,
                        positiveColor = AccentEmerald,
                        negativeColor = AccentCoral
                    )
                }
            }

            if (comparison.increases.isNotEmpty()) {
                CategoryDeltaSection(
                    title = tr("Maiores Subidas", "Biggest Increases"),
                    items = comparison.increases,
                    deltaColor = AccentCoral
                )
            }

            if (comparison.decreases.isNotEmpty()) {
                CategoryDeltaSection(
                    title = tr("Maiores Descidas", "Biggest Decreases"),
                    items = comparison.decreases,
                    deltaColor = AccentEmerald
                )
            }
        }
    }
}

@Composable
internal fun ComparisonDeltaRow(
    label: String,
    current: String,
    baseline: String,
    delta: String,
    deltaPercent: String?,
    positive: Boolean,
    positiveColor: Color,
    negativeColor: Color
) {
    val accent = if (positive) positiveColor else negativeColor

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "$current vs. $baseline",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (positive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = delta,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            if (deltaPercent != null) {
                Text(
                    text = deltaPercent,
                    fontSize = 11.sp,
                    color = accent
                )
            }
        }
    }
}

@Composable
internal fun CategoryDeltaSection(
    title: String,
    items: List<CategoryDeltaUi>,
    deltaColor: Color
) {
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
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(item.colorHex))
                        )
                        Column {
                            Text(
                                text = item.categoryName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.current} vs. ${item.baseline}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.delta,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = deltaColor
                        )
                        if (item.deltaPercent != null) {
                            Text(
                                text = item.deltaPercent,
                                fontSize = 11.sp,
                                color = deltaColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// VISTA 6: Calendário (Gasto por Dia)
// -----------------------------------------------------------------------------
@Composable
internal fun CalendarioView(
    uiState: AnalyticsUiState,
    onSelectDay: (Long) -> Unit
) {
    val scrollState = rememberScrollState()

    val calendarDays = uiState.calendarDays.map { item ->
        CalendarDayData(
            epochDay = item.epochDay,
            dayOfMonth = item.dayOfMonth,
            expenseCents = item.totalExpenseCents,
            transactionCount = item.transactionCount,
            isToday = item.isToday
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calendar Grid Card
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
                        text = tr("Calendário de Gastos", "Spending Calendar"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = tr("Intensidade do gasto por dia · Toca num dia para ver os movimentos", "Spending intensity per day · Tap a day to see its transactions"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                MonthlyCalendarGrid(
                    days = calendarDays,
                    selectedEpochDay = uiState.selectedCalendarDay?.epochDay,
                    onSelectDay = onSelectDay
                )

                CalendarIntensityLegend()
            }
        }

        // Selected Day Movements Card
        val selection = uiState.selectedCalendarDay
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
                        text = if (selection != null) tr("Movimentos de ${selection.label}", "Transactions for ${selection.label}") else tr("Movimentos do dia", "Transactions on this day"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (selection != null && selection.totalExpenseCents > 0L) {
                        Text(
                            text = selection.formattedTotal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCoral
                        )
                    }
                }

                when {
                    selection == null -> {
                        Text(
                            text = tr("Toca num dia do calendário para ver os movimentos desse dia.", "Tap a day on the calendar to see that day's transactions."),
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    selection.transactions.isEmpty() -> {
                        Text(
                            text = tr("Sem movimentos registados neste dia.", "No transactions recorded on this day."),
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    else -> {
                        selection.transactions.forEach { transaction ->
                            TransactionRowItem(
                                transaction = transaction,
                                category = uiState.categoryMap[transaction.categoryId],
                                parentCategory = uiState.categoryMap[transaction.categoryId]?.parentId?.let { uiState.categoryMap[it] },
                                onDelete = {},
                                onEdit = {},
                                showActions = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CalendarIntensityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tr("Menos", "Less"), fontSize = 10.sp, color = TextMuted)
        Spacer(modifier = Modifier.width(4.dp))
        listOf(0.15f, 0.30f, 0.45f, 0.55f).forEach { alpha ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkSurfaceVariant)
                    .background(AccentCoral.copy(alpha = alpha))
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(tr("Mais", "More"), fontSize = 10.sp, color = TextMuted)
    }
}
