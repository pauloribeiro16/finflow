package com.financas.app.ui.screens.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.financas.app.ui.theme.FinflowBackground
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryContainer
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import com.financas.app.data.entity.TransactionEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.domain.model.DetectedSubscription
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.model.SubscriptionReport
import com.financas.app.domain.usecase.CalendarDayData
import com.financas.app.domain.usecase.ChartScale
import com.financas.app.ui.components.DonutSlice
import com.financas.app.ui.components.EffortBar
import com.financas.app.ui.components.HorizontalParetoBars
import com.financas.app.ui.components.InteractiveDonutCanvas
import com.financas.app.ui.components.InteractiveLineChart
import com.financas.app.ui.components.InteractiveMonthlyBarChart
import com.financas.app.ui.components.InteractiveYearComparisonChart
import com.financas.app.ui.components.MonthlyBarData
import com.financas.app.ui.components.MonthlyCalendarGrid
import com.financas.app.ui.components.ParetoBarItem
import com.financas.app.ui.screens.home.TransactionRowItem
import com.financas.app.ui.theme.AccentAmber
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBackground
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceHighlight
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import com.financas.app.ui.util.shareText
import kotlin.math.roundToLong

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onGlobalMonthChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Global €/% Toggle + Share Report
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = { viewModel.toggleDisplayMode() })
                    .padding(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (uiState.displayMode == DisplayMode.EURO) AccentBlue else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "€",
                            fontSize = 12.sp,
                            fontWeight = if (uiState.displayMode == DisplayMode.EURO) FontWeight.Bold else FontWeight.Medium,
                            color = if (uiState.displayMode == DisplayMode.EURO) Color.White else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (uiState.displayMode == DisplayMode.INCOME_PERCENTAGE) AccentBlue else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "% do que recebo",
                            fontSize = 12.sp,
                            fontWeight = if (uiState.displayMode == DisplayMode.INCOME_PERCENTAGE) FontWeight.Bold else FontWeight.Medium,
                            color = if (uiState.displayMode == DisplayMode.INCOME_PERCENTAGE) Color.White else TextSecondary
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    shareText(
                        context = context,
                        text = viewModel.buildMonthlyReport(),
                        title = "relatorio_financas_${System.currentTimeMillis()}.txt",
                        mimeType = "text/plain"
                    )
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Partilhar relatório",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Sub-Navigation Tabs: No Quê | Onde | Como | Evolução | Comparar | Calendário
        val coroutineScope = rememberCoroutineScope()
        val lensPagerState = rememberPagerState(
            initialPage = AnalyticsLens.entries.indexOf(uiState.activeLens).coerceAtLeast(0),
            pageCount = { AnalyticsLens.entries.size }
        )

        LaunchedEffect(lensPagerState) {
            snapshotFlow { lensPagerState.currentPage }.collect { page ->
                val lens = AnalyticsLens.entries[page]
                if (lens != uiState.activeLens) {
                    viewModel.setActiveLens(lens)
                }
            }
        }

        LaunchedEffect(uiState.activeLens) {
            val targetIndex = AnalyticsLens.entries.indexOf(uiState.activeLens)
            val settledIndex = if (lensPagerState.isScrollInProgress) {
                lensPagerState.targetPage
            } else {
                lensPagerState.currentPage
            }
            if (targetIndex != settledIndex) {
                lensPagerState.animateScrollToPage(targetIndex)
            }
        }

        LensSelectorTabs(
            selectedLens = uiState.activeLens,
            onSelectLens = { lens ->
                coroutineScope.launch {
                    lensPagerState.animateScrollToPage(AnalyticsLens.entries.indexOf(lens))
                }
            }
        )

        // Active Lens View Content with Swipeable Pager
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            HorizontalPager(
                state = lensPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (AnalyticsLens.entries[page]) {
                    AnalyticsLens.ANUAL -> {
                        AnualView(
                            uiState = uiState,
                            onSelectYear = { viewModel.selectAnnualYear(it) },
                            onSelectMonth = { ym -> viewModel.selectAnnualMonth(ym) },
                            onOpenMonthInDetail = { ym ->
                                onGlobalMonthChanged(ym)
                                viewModel.setActiveLens(AnalyticsLens.NO_QUE)
                            },
                            onSelectCategory = { viewModel.selectAnnualCategory(it) },
                            onSelectSubcategory = { viewModel.selectAnnualSubcategory(it) }
                        )
                    }
                    AnalyticsLens.NO_QUE -> {
                        NoQueView(
                            uiState = uiState,
                            onSelectParentCategory = { viewModel.selectParentCategory(it) }
                        )
                    }
                    AnalyticsLens.ONDE -> {
                        OndeView(uiState = uiState)
                    }
                    AnalyticsLens.COMO -> {
                        ComoView(uiState = uiState)
                    }
                    AnalyticsLens.EVOLUCAO -> {
                        EvolucaoView(
                            uiState = uiState,
                            onSelectMonth = { viewModel.selectEvolutionMonth(it) },
                            onSelectEvolutionCategory = { viewModel.selectEvolutionCategory(it) }
                        )
                    }
                    AnalyticsLens.COMPARAR -> {
                        CompararView(
                            uiState = uiState,
                            onSelectBaseline = { viewModel.setComparisonBaseline(it) }
                        )
                    }
                    AnalyticsLens.CALENDARIO -> {
                        CalendarioView(
                            uiState = uiState,
                            onSelectDay = { viewModel.selectCalendarDay(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LensSelectorTabs(
    selectedLens: AnalyticsLens,
    onSelectLens: (AnalyticsLens) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnalyticsLens.entries.forEach { lens ->
            val isSelected = lens == selectedLens
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) AccentBlue.copy(alpha = 0.16f) else Color.Transparent,
                animationSpec = tween(200),
                label = "lensBgColor"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) AccentBlue.copy(alpha = 0.55f) else Color.Transparent,
                animationSpec = tween(200),
                label = "lensBorderColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) AccentBlue else TextSecondary,
                animationSpec = tween(180),
                label = "lensTextColor"
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.03f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "lensScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectLens(lens) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lens.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// LINHA DE TRANSAÇÃO DISCRIMINADA
// -----------------------------------------------------------------------------
@Composable
private fun DiscriminatedTransactionRow(
    transaction: TransactionEntity,
    categoryName: String?,
    displayMode: DisplayMode,
    totalIncomeCents: Long = 0L,
    modifier: Modifier = Modifier
) {
    val date = LocalDate.ofEpochDay(transaction.epochDay)
    val dateLabel = date.format(DateTimeFormatter.ofPattern("dd MMM", Locale("pt", "PT")))
    val isExpense = transaction.type.equals("EXPENSE", ignoreCase = true)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(0.5.dp, FinflowBorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    .clip(RoundedCornerShape(6.dp))
                    .background(FinflowSurfaceContainerLow)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dateLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowTextSecondary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant.ifEmpty { categoryName ?: "Movimento" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildList {
                    if (categoryName != null && transaction.merchant.isNotEmpty()) {
                        add(categoryName)
                    }
                    if (transaction.contextTag.isNotEmpty()) {
                        add(transaction.contextTag)
                    }
                    if (transaction.isFixed) {
                        add("Fixo")
                    }
                }.joinToString(" • ")

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = FinflowTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Text(
            text = (if (isExpense) "-" else "+") + MoneyCents(transaction.amountCents).formatPt(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) FinflowTextPrimary else FinflowSecondary
        )
    }
}

// -----------------------------------------------------------------------------
// VISTA ANUAL: Visão Anual Completa (2024, 2025, 2026) com Movimentos Discriminados
// -----------------------------------------------------------------------------
@Composable
private fun AnualView(
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
                            text = "Taxa ${annual.formattedSavingsRate}",
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
                        title = "Recebido",
                        value = annual.formattedTotalIncome,
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    EvolutionMetricCard(
                        title = "Gasto",
                        value = annual.formattedTotalExpense,
                        accentColor = AccentCoral,
                        modifier = Modifier.weight(1f)
                    )
                    EvolutionMetricCard(
                        title = "Poupança",
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
                            text = "Média Mensal de Gastos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FinflowTextSecondary
                        )
                        Text(
                            text = "Calculada sobre ${annual.activeMonthsCount} meses com despesas",
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
                        text = "Evolução dos 12 Meses (${annual.selectedYear})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = "Receitas vs. Despesas · Toca num mês para ver detalhes",
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
                                    text = "Taxa ${MoneyCents.formatPercent(selectedMonth.savingsRatePercent)}",
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
                                    text = "Rec: ${selectedMonth.formattedIncome}",
                                    fontSize = 11.sp,
                                    color = FinflowTextSecondary
                                )
                                Text(
                                    text = "Gasto: ${selectedMonth.formattedExpense}",
                                    fontSize = 11.sp,
                                    color = FinflowTextSecondary
                                )
                                Text(
                                    text = "Poup: ${selectedMonth.formattedSavings}",
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
                                    text = "Inspecionar Mês Completo no 'No Quê' ➔",
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
                text = "Detalhamento Anual por Categoria (${annual.categories.size})",
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
                                    text = "${category.transactionCount} movimentos no ano",
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
                                                text = "(${sub.transactionCount} mov.)",
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

// -----------------------------------------------------------------------------
// VISTA 1: No Quê (Categorias & Subcategorias — Finflow Stitch Design)
// -----------------------------------------------------------------------------
@Composable
private fun NoQueView(
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
                            text = "Despesa Total",
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
                            text = if (isIncreased) "↗ +$absDeltaText" else "↘ -$absDeltaText",
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

                Text(
                    text = "${comparison?.expenseDelta ?: "0,00 €"} face ao mês anterior",
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
                val centerTitle = selectedParent?.name ?: "Despesa Total"
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
                        text = "Hoje",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = topParent?.let {
                        "A categoria '${it.name}' representa ${MoneyCents.formatPercent(it.percentageOfExpense)} das tuas despesas. Mantém o controlo para atingir a tua meta de poupança!"
                    } ?: "Continua a registar os teus movimentos diários para gerar análises detalhadas.",
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
                            text = "Histórico Trimestral",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                        Text(
                            text = "Comparativo de Gastos",
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
                            text = "Média: ${quarterlyHistory.formattedAverageExpense}",
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
                text = "Detalhamento por Categoria (${uiState.parentCategories.size})",
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
                                    text = "${parent.transactionCount} movimentos registados",
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
                                                text = "(${sub.transactionCount} mov.)",
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
                    text = "Exportar PDF",
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
                    text = "Novo Limite",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// VISTA 2: Onde (Contextos & Top Estabelecimentos)
// -----------------------------------------------------------------------------
@Composable
private fun OndeView(uiState: AnalyticsUiState) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Contexts Pareto Bars Card
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
                        text = "Gastos por Contexto",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Onde o teu dinheiro foi gasto este mês",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (uiState.contexts.isEmpty()) {
                    Text(
                        text = "Sem gastos registados neste mês.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    val maxContextAmount = uiState.contexts.maxOfOrNull { it.amountCents } ?: 0L
                    val paretoItems = uiState.contexts.map { ctx ->
                        ParetoBarItem(
                            id = ctx.tag,
                            label = ctx.label,
                            amountCents = ctx.amountCents,
                            formattedValue = ctx.formattedValue,
                            percentage = ctx.percentageOfIncome,
                            color = AccentBlue,
                            secondaryText = "${ctx.transactionCount} movimentos · ${MoneyCents.formatPercent(ctx.percentageOfExpense)} dos gastos"
                        )
                    }

                    HorizontalParetoBars(
                        items = paretoItems,
                        maxAmountCents = maxContextAmount
                    )
                }
            }
        }

        // Top 10 Merchants (Pareto Ranking) Card
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
                        text = "Top 10 Estabelecimentos",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Maiores valores acumulados e média por ida",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (uiState.topMerchants.isEmpty()) {
                    Text(
                        text = "Sem estabelecimentos registados neste mês.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    uiState.topMerchants.forEachIndexed { index, merchant ->
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
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (index < 3) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < 3) AccentBlue else TextSecondary
                                    )
                                }

                                Column {
                                    Text(
                                        text = merchant.merchant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${merchant.transactionCount} idas · média ${merchant.formattedAverageTicket}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = merchant.formattedValue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// VISTA 3: Como (Fixos vs Variáveis & Meios de Pagamento)
// -----------------------------------------------------------------------------
@Composable
private fun ComoView(uiState: AnalyticsUiState) {
    val scrollState = rememberScrollState()
    val rigidity = uiState.rigidity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rigidity Structure Card
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
                        text = "Estrutura de Rigidez",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fixos vs. Variáveis vs. Poupança Líquida",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                EffortBar(
                    fixedPercent = rigidity.fixedPercentOfIncome,
                    variablePercent = rigidity.variablePercentOfIncome,
                    savingsPercent = rigidity.savingsPercentOfIncome,
                    fixedCents = rigidity.fixedExpenseCents,
                    variableCents = rigidity.variableExpenseCents,
                    savingsCents = rigidity.netSavingsCents
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RigidityMiniCard(
                        title = "Fixos",
                        value = rigidity.formattedFixed,
                        percent = rigidity.fixedPercentOfIncome,
                        indicatorColor = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    RigidityMiniCard(
                        title = "Variáveis",
                        value = rigidity.formattedVariable,
                        percent = rigidity.variablePercentOfIncome,
                        indicatorColor = AccentCoral,
                        modifier = Modifier.weight(1f)
                    )
                    RigidityMiniCard(
                        title = "Poupança",
                        value = rigidity.formattedSavings,
                        percent = rigidity.savingsPercentOfIncome,
                        indicatorColor = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Subscriptions & Recurring Expenses Card
        SubscriptionsCard(
            subscriptionReport = uiState.subscriptionReport,
            categoryMap = uiState.categoryMap
        )

        // Payment Methods Breakdown Card
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
                        text = "Meios de Pagamento",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Como movimentas o teu dinheiro",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (uiState.paymentMethods.isEmpty()) {
                    Text(
                        text = "Sem pagamentos registados neste mês.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    uiState.paymentMethods.forEach { method ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = method.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = method.formattedValue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${MoneyCents.formatPercent(method.percentageOfExpense)} dos gastos",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${method.transactionCount} mov.",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (method.percentageOfExpense / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = AccentBlue,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RigidityMiniCard(
    title: String,
    value: String,
    percent: Double,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${MoneyCents.formatPercent(percent)} do rec.",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun SubscriptionsCard(
    subscriptionReport: SubscriptionReport,
    categoryMap: Map<String, CategoryEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subscrições & Custos Recorrentes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Compromissos mensais e impacto no orçamento",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (subscriptionReport.subscriptions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${subscriptionReport.subscriptions.size} ativas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentBlue
                        )
                    }
                }
            }

            // Summary Metrics: Total Mensal + Custo Anualizado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total Mensal",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyCents.formatCents(subscriptionReport.totalMonthlyCents),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Custo Anualizado",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyCents.formatCents(subscriptionReport.totalAnnualCents),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )
                    }
                }
            }

            // Banner de Alerta de Aumento de Preço (se aplicável)
            if (subscriptionReport.hikesCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentCoral.copy(alpha = 0.12f))
                        .border(1.dp, AccentCoral.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 16.sp
                        )
                        Column {
                            val hikeText = if (subscriptionReport.hikesCount == 1) {
                                "1 subscrição aumentou de preço este mês (+${MoneyCents.formatCents(subscriptionReport.totalHikeIncreaseCents)}/mês)"
                            } else {
                                "${subscriptionReport.hikesCount} subscrições aumentaram de preço este mês (+${MoneyCents.formatCents(subscriptionReport.totalHikeIncreaseCents)}/mês)"
                            }
                            Text(
                                text = "Atenção: Subida de Mensalidade",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCoral
                            )
                            Text(
                                text = hikeText,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Lista de Subscrições
            if (subscriptionReport.subscriptions.isEmpty()) {
                Text(
                    text = "Sem subscrições ou custos recorrentes detetados.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subscriptionReport.subscriptions.forEach { sub ->
                        SubscriptionItemRow(
                            subscription = sub,
                            category = categoryMap[sub.categoryId]
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionItemRow(
    subscription: DetectedSubscription,
    category: CategoryEntity?
) {
    val categoryName = category?.name ?: subscription.categoryId

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (subscription.isPriceHike) AccentCoral.copy(alpha = 0.5f) else DarkBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = subscription.merchant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subscription.isFixed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Fixo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentBlue
                            )
                        }
                    }
                }

                Text(
                    text = "${MoneyCents.formatCents(subscription.monthlyAmountCents)} / mês",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryName,
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Text(
                    text = "${MoneyCents.formatCents(subscription.annualizedCostCents)} / ano",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            if (subscription.isPriceHike && subscription.priceDifferenceCents != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prevText = subscription.previousAmountCents?.let {
                        "Anterior: ${MoneyCents.formatCents(it)}"
                    } ?: ""
                    Text(
                        text = prevText,
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentCoral.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${MoneyCents.formatCents(subscription.priceDifferenceCents)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCoral
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// VISTA 4: Evolução (Histórico Anual Mês a Mês)
// -----------------------------------------------------------------------------
@Composable
private fun EvolucaoView(
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
                        text = "Evolução Anual Mês a Mês",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Receitas vs. Despesas · Linha tracejada de média mensal",
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
                    text = "Arrasta ou toca nos meses para inspecionar os detalhes",
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
                            text = "Detalhes de ${AnalyticsViewModel.formatMonthTitle(selectedMonth.yearMonth)}",
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
                            title = "Recebido",
                            value = selectedMonth.formattedIncome,
                            accentColor = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        EvolutionMetricCard(
                            title = "Gasto",
                            value = selectedMonth.formattedExpense,
                            accentColor = AccentCoral,
                            modifier = Modifier.weight(1f)
                        )
                        EvolutionMetricCard(
                            title = "Poupança",
                            value = selectedMonth.formattedSavings,
                            accentColor = AccentBlue,
                            subtitle = "${MoneyCents.formatPercent(selectedMonth.savingsRatePercent)} taxa",
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
                        text = "Média Mensal de Gastos",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Calculada apenas sobre meses com atividade",
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
                            text = "Evolução de ${categoryTrend.categoryName}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Gastos mês a mês no ano · Linha tracejada de média",
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
                        averageLabel = "Média",
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
                                text = "Mês selecionado",
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
                            text = "Taxa de Poupança",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Percentagem poupada mês a mês · Meta de 20%",
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
                        averageLabel = "Média",
                        referenceValue = 20.0,
                        referenceLabel = "Meta 20%",
                        tickFormatter = { ChartScale.formatCompactPercent(it) },
                        lineColor = AccentEmerald
                    )

                    Text(
                        text = "Média nos meses com atividade: ${savingsRateTrend.formattedAverage}",
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
                            text = "Comparação Anual",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Gastos mês a mês por ano",
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
                            text = "Totais por Ano",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Recebido, gasto e poupança acumulados",
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
                                        text = "Taxa ${totals.formattedRate}",
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
                                        text = "Rec. ${totals.formattedIncome}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Gasto ${totals.formattedExpense}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "Poup. ${totals.formattedSavings}",
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
private fun EvolutionMetricCard(
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

// -----------------------------------------------------------------------------
// VISTA 5: Comparar (Mês Atual vs. Base de Comparação)
// -----------------------------------------------------------------------------
@Composable
private fun CompararView(
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
                text = "Sem dados para comparar com esta base.",
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
                            text = "Comparação com ${comparison.baselineLabel}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Deltas do mês selecionado face à base",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    ComparisonDeltaRow(
                        label = "Gasto",
                        current = comparison.currentExpense,
                        baseline = comparison.baselineExpense,
                        delta = comparison.expenseDelta,
                        deltaPercent = comparison.expenseDeltaPercent,
                        positive = comparison.expenseDeltaPositive,
                        positiveColor = AccentCoral,
                        negativeColor = AccentEmerald
                    )
                    ComparisonDeltaRow(
                        label = "Recebido",
                        current = comparison.currentIncome,
                        baseline = comparison.baselineIncome,
                        delta = comparison.incomeDelta,
                        deltaPercent = comparison.incomeDeltaPercent,
                        positive = comparison.incomeDeltaPositive,
                        positiveColor = AccentEmerald,
                        negativeColor = AccentCoral
                    )
                    ComparisonDeltaRow(
                        label = "Poupança",
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
                    title = "Maiores Subidas",
                    items = comparison.increases,
                    deltaColor = AccentCoral
                )
            }

            if (comparison.decreases.isNotEmpty()) {
                CategoryDeltaSection(
                    title = "Maiores Descidas",
                    items = comparison.decreases,
                    deltaColor = AccentEmerald
                )
            }
        }
    }
}

@Composable
private fun ComparisonDeltaRow(
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
private fun CategoryDeltaSection(
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
private fun CalendarioView(
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
                        text = "Calendário de Gastos",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Intensidade do gasto por dia · Toca num dia para ver os movimentos",
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
                        text = if (selection != null) "Movimentos de ${selection.label}" else "Movimentos do dia",
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
                            text = "Toca num dia do calendário para ver os movimentos desse dia.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    selection.transactions.isEmpty() -> {
                        Text(
                            text = "Sem movimentos registados neste dia.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    else -> {
                        selection.transactions.forEach { transaction ->
                            TransactionRowItem(
                                transaction = transaction,
                                category = uiState.categoryMap[transaction.categoryId],
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
private fun CalendarIntensityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Menos", fontSize = 10.sp, color = TextMuted)
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
        Text("Mais", fontSize = 10.sp, color = TextMuted)
    }
}
