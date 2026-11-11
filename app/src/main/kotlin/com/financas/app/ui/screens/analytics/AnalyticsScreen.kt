package com.financas.app.ui.screens.analytics

import com.financas.app.core.i18n.tr

import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.DarkBackground
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextSecondary
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings
import com.financas.app.ui.i18n.LocalPrivacyMode
import com.financas.app.ui.util.shareText

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onGlobalMonthChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val isPrivacyMode = LocalPrivacyMode.current
    val isEnglish = language == AppLanguage.EN

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
                            text = strings.displayModeIncomeShare,
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
                    contentDescription = tr("Partilhar relatório", "Share report"),
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
internal fun LensSelectorTabs(
    selectedLens: AnalyticsLens,
    onSelectLens: (AnalyticsLens) -> Unit
) {
    val strings = LocalAppStrings.current
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
                    text = lens.localizedLabel(strings),
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
internal fun DiscriminatedTransactionRow(
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
                    text = transaction.merchant.ifEmpty { categoryName ?: tr("Movimento", "Transaction") },
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

        val isPrivacyMode = LocalPrivacyMode.current
        Text(
            text = if (isPrivacyMode) MoneyCents.MASKED_STRING else (if (isExpense) "-" else "+") + MoneyCents(transaction.amountCents).formatPt(false),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) FinflowTextPrimary else FinflowSecondary
        )
    }
}
