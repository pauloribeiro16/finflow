package com.financas.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.data.entity.PendingTransactionEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowAmberLight
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowBackground
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowCoral
import com.financas.app.ui.theme.FinflowCoralLight
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryContainer
import com.financas.app.ui.theme.FinflowPrimaryLight
import com.financas.app.ui.theme.FinflowPurple
import com.financas.app.ui.theme.FinflowPurpleLight
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSecondaryLight
import com.financas.app.ui.theme.FinflowSky
import com.financas.app.ui.theme.FinflowSkyLight
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainer
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.i18n.AppLanguage
import com.financas.app.ui.i18n.CategoryTranslator
import com.financas.app.ui.i18n.LocalAppLanguage
import com.financas.app.ui.i18n.LocalAppStrings
import com.financas.app.ui.i18n.LocalPrivacyMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: (Int) -> Unit = {},
    onNavigateToInvestments: () -> Unit = {},
    onEdit: (TransactionEntity) -> Unit = {},
    onEditPending: (PendingTransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.monthlySummary
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val isPrivacyMode = LocalPrivacyMode.current
    val isEnglish = language == AppLanguage.EN
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showPendingSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(if (isEnglish) "All" else "Todas") }
    var showAllTransactions by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isNotificationAccessGranted by remember {
        mutableStateOf(com.financas.app.data.service.FinancialNotificationListenerService.isNotificationAccessGranted(context))
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = com.financas.app.data.service.FinancialNotificationListenerService.isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinflowBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pill Banner: Net Savings Badge (+€... poupados)
        if (summary.netSavingsCents > 0L) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(FinflowSecondaryLight)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = FinflowSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+${MoneyCents(summary.netSavingsCents).formatPt()} poupados",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowSecondary
                        )
                    }
                }
            }
        }

        // Banner: Notification Access Request if not granted
        if (!isNotificationAccessGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FinflowPrimaryLight.copy(alpha = 0.35f))
                    .border(1.dp, FinflowPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(FinflowPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Ativar Deteção MB WAY & Bancos",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowTextPrimary
                            )
                            Text(
                                text = "Toca para autorizar a captura automática",
                                fontSize = 11.sp,
                                color = FinflowTextSecondary
                            )
                        }
                    }
                    Text(
                        text = "Ativar ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowPrimary
                    )
                }
            }
        }

        // Banner: Pending Transactions Review
        if (uiState.pendingCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(FinflowAmberLight)
                    .clickable { showPendingSheet = true }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = FinflowAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Movimentos Pendentes (${uiState.pendingCount})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowTextPrimary
                            )
                            Text(
                                text = "Despesas detetadas para rever",
                                fontSize = 12.sp,
                                color = FinflowTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showPendingSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FinflowAmber),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Rever",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Hero Card: DESPESAS DESTE MÊS (Stitch Design)
        val budgetTargetCents = if (uiState.budgetReport.totalLimitCents > 0L) {
            uiState.budgetReport.totalLimitCents
        } else if (summary.totalIncomeCents > 0L) {
            summary.totalIncomeCents
        } else {
            300000L
        }

        val remainingCents = budgetTargetCents - summary.totalExpenseCents
        val isWithinBudget = remainingCents >= 0L
        val budgetPercent = if (budgetTargetCents > 0L) {
            (summary.totalExpenseCents.toDouble() / budgetTargetCents.toDouble()) * 100.0
        } else 0.0

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
                // Header: Title + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DESPESAS DESTE MÊS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isWithinBudget) FinflowSecondaryLight else FinflowCoralLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isWithinBudget) "• Dentro da meta" else "• Teto excedido",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWithinBudget) FinflowSecondary else FinflowCoral
                        )
                    }
                }

                // Big Expense Display
                Text(
                    text = MoneyCents(summary.totalExpenseCents).formatPt(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextPrimary
                )

                // Progress Bar
                val frac = (summary.totalExpenseCents.toFloat() / budgetTargetCents.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { frac },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isWithinBudget) FinflowPrimary else FinflowCoral,
                    trackColor = FinflowSurfaceContainerLow
                )

                // Sub-row 1: Remaining vs Goal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${MoneyCents(remainingCents).formatPt()} restantes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinflowTextPrimary
                    )

                    Text(
                        text = "Meta: ${MoneyCents(budgetTargetCents).formatPt()}",
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }

                // Sub-row 2: Variation vs Ceiling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previousExpense = uiState.previousMonthExpenseCents
                    if (previousExpense > 0L) {
                        val deltaPercent = (summary.totalExpenseCents - previousExpense).toDouble() / previousExpense.toDouble() * 100.0
                        val isDown = deltaPercent <= 0.0
                        Text(
                            text = "${if (isDown) "↘" else "↗"} ${MoneyCents.formatPercent(deltaPercent)} em relação ao mês anterior",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDown) FinflowSecondary else FinflowCoral
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = "${MoneyCents.formatPercent(budgetPercent)} do teto",
                        fontSize = 11.sp,
                        color = FinflowTextMuted
                    )
                }
            }
        }

        // Card: PATRIMÓNIO APLICADO (Investimentos, PPRs, ETFs)
        val portfolio = uiState.portfolioSummary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToInvestments() },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = FinflowSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
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
                                .background(FinflowPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏛️", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "PATRIMÓNIO APLICADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FinflowTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (portfolio.totalAssetsCount > 0)
                                    "${portfolio.totalAssetsCount} aplicações ativas (PPR, ETFs...)"
                                else "PPRs, Depósitos a Prazo, ETFs",
                                fontSize = 11.sp,
                                color = FinflowTextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ver Carteira",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = FinflowPrimary,
                            modifier = Modifier.size(14.dp)
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
                            text = MoneyCents(portfolio.totalCurrentValueCents).formatPt(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextPrimary
                        )
                        Text(
                            text = "investido: ${MoneyCents(portfolio.totalInvestedCents).formatPt()}",
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                    }

                    if (portfolio.totalAssetsCount > 0) {
                        val isProfitPositive = portfolio.totalProfitLossCents >= 0
                        val profitColor = if (isProfitPositive) FinflowSecondary else FinflowCoral
                        val profitBg = if (isProfitPositive) FinflowSecondaryLight else FinflowCoralLight
                        val sign = if (isProfitPositive) "+" else ""

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(profitBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$sign${MoneyCents(portfolio.totalProfitLossCents).formatPt()} ($sign${MoneyCents.formatPercent(portfolio.totalProfitLossPercentage)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = profitColor
                            )
                        }
                    }
                }
            }
        }

        // Grid 2 Cards: Média Diária & Maior Categoria
        val selectedYm = java.time.YearMonth.of(uiState.selectedYearMonth / 100, uiState.selectedYearMonth % 100)
        val daysElapsed = (if (selectedYm == java.time.YearMonth.now()) LocalDate.now().dayOfMonth else selectedYm.lengthOfMonth()).coerceAtLeast(1)
        val dailyAverageCents = summary.totalExpenseCents / daysElapsed
        val topCategory = summary.categoryShares.firstOrNull()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Média Diária
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FinflowSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Média Diária",
                            fontSize = 12.sp,
                            color = FinflowTextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FinflowPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = FinflowPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = MoneyCents(dailyAverageCents).formatPt(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )

                    Text(
                        text = "✓ Ritmo sustentável",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinflowSecondary
                    )
                }
            }

            // Card 2: Maior Categoria
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FinflowSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinflowBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Maior Categoria",
                            fontSize = 12.sp,
                            color = FinflowTextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FinflowAmberLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = FinflowAmber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = MoneyCents(topCategory?.amountCents ?: 0L).formatPt(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )

                    Text(
                        text = topCategory?.let { "${it.categoryName} (${MoneyCents.formatPercent(it.percentageOfExpense, 0)})" }
                            ?: "Sem despesas",
                        fontSize = 11.sp,
                        color = FinflowTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section: Ritmo Semanal (4 Semanas)
        val allMonthExpenses = if (uiState.allMonthTransactions.isNotEmpty()) uiState.allMonthTransactions else uiState.recentTransactions
        val monthExpenses = allMonthExpenses.filter { it.type.equals("EXPENSE", ignoreCase = true) }
        val week1Expenses = monthExpenses.filter { LocalDate.ofEpochDay(it.epochDay).dayOfMonth in 1..7 }.sumOf { it.amountCents }
        val week2Expenses = monthExpenses.filter { LocalDate.ofEpochDay(it.epochDay).dayOfMonth in 8..14 }.sumOf { it.amountCents }
        val week3Expenses = monthExpenses.filter { LocalDate.ofEpochDay(it.epochDay).dayOfMonth in 15..21 }.sumOf { it.amountCents }
        val week4Expenses = monthExpenses.filter { LocalDate.ofEpochDay(it.epochDay).dayOfMonth >= 22 }.sumOf { it.amountCents }

        val weekAmounts = listOf(week1Expenses, week2Expenses, week3Expenses, week4Expenses)
        val maxWeekAmount = (weekAmounts.maxOrNull() ?: 1L).coerceAtLeast(1L)
        val totalWeekExpenses = weekAmounts.sum()
        val avgWeekExpense = if (totalWeekExpenses > 0L) totalWeekExpenses / 4L else 0L

        val now = LocalDate.now()
        val currentYm = now.year * 100 + now.monthValue
        val isViewingCurrentMonth = uiState.selectedYearMonth == currentYm
        val currentWeekIdx = if (isViewingCurrentMonth) ((now.dayOfMonth - 1) / 7).coerceIn(0, 3) else -1

        val weekDateRanges = listOf("1-7", "8-14", "15-21", "22+")

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ritmo Semanal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextPrimary
                        )
                        Text(
                            text = "Distribuição de gastos por semana",
                            fontSize = 12.sp,
                            color = FinflowTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FinflowPrimaryLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Média: ${MoneyCents.formatCents(avgWeekExpense)}/sem",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowPrimary
                        )
                    }
                }

                // 4 Vertical Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekAmounts.forEachIndexed { index, amountCents ->
                        val barHeightFrac = if (maxWeekAmount > 0L && amountCents > 0L) {
                            (amountCents.toFloat() / maxWeekAmount.toFloat()).coerceIn(0.12f, 1f)
                        } else {
                            0.04f
                        }
                        val isHighest = amountCents == maxWeekAmount && amountCents > 0L
                        val isCurrentWeek = index == currentWeekIdx

                        val barColor = when {
                            isCurrentWeek -> FinflowPrimary
                            isHighest -> FinflowSecondary
                            amountCents > 0L -> FinflowSurfaceContainer
                            else -> FinflowSurfaceContainerLow
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = "${amountCents / 100} €",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isHighest || isCurrentWeek) FinflowTextPrimary else FinflowTextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height((64 * barHeightFrac).dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(barColor)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sem ${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = if (isCurrentWeek) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrentWeek) FinflowTextPrimary else FinflowTextSecondary
                            )
                            Text(
                                text = weekDateRanges[index],
                                fontSize = 9.sp,
                                color = FinflowTextMuted
                            )
                        }
                    }
                }
            }
        }

        // Section: Transações Recentes
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
                    Text(
                        text = "Movimentos (${uiState.monthTitle})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )

                    Row(
                        modifier = Modifier
                            .clickable(onClick = { onNavigateToTransactions(uiState.selectedYearMonth) })
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Ver todas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = FinflowPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Todas", "Hoje", "Ontem", "Filtro").forEach { filter ->
                        val isSel = selectedFilter == filter
                        val pillBgColor by animateColorAsState(
                            targetValue = if (isSel) FinflowPrimary else FinflowSurfaceContainerLow,
                            animationSpec = tween(200),
                            label = "homePillBgColor"
                        )
                        val pillTextColor by animateColorAsState(
                            targetValue = if (isSel) Color.White else FinflowTextSecondary,
                            animationSpec = tween(180),
                            label = "homePillTextColor"
                        )
                        val pillScale by animateFloatAsState(
                            targetValue = if (isSel) 1.03f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "homePillScale"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = pillScale
                                    scaleY = pillScale
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(pillBgColor)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = pillTextColor
                            )
                        }
                    }
                }

                // Transactions List
                val allTxs = if (uiState.allMonthTransactions.isNotEmpty()) uiState.allMonthTransactions else uiState.recentTransactions
                val todayEpoch = LocalDate.now().toEpochDay()
                val yesterdayEpoch = LocalDate.now().minusDays(1).toEpochDay()
                val filteredByPill = when (selectedFilter) {
                    "Hoje" -> allTxs.filter { it.epochDay == todayEpoch }
                    "Ontem" -> allTxs.filter { it.epochDay == yesterdayEpoch }
                    else -> allTxs
                }

                AnimatedContent(
                    targetState = Pair(selectedFilter, showAllTransactions),
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(130))
                    },
                    label = "HomeTransactionsListTransition"
                ) { (filterState, showAllState) ->
                    val stateTxs = when (filterState) {
                        "Hoje" -> allTxs.filter { it.epochDay == todayEpoch }
                        "Ontem" -> allTxs.filter { it.epochDay == yesterdayEpoch }
                        else -> allTxs
                    }
                    val displayedTxs = if (showAllState) stateTxs else stateTxs.take(5)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (displayedTxs.isEmpty()) {
                            Text(
                                text = if (filterState == "Todas") "Sem transações registadas em ${uiState.monthTitle}."
                                       else "Sem transações para o filtro selecionado ($filterState).",
                                fontSize = 13.sp,
                                color = FinflowTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            displayedTxs.forEach { tx ->
                                FinflowTransactionRow(
                                    transaction = tx,
                                    category = uiState.categoryMap[tx.categoryId],
                                    parentCategory = uiState.categoryMap[tx.categoryId]?.parentId?.let { uiState.categoryMap[it] },
                                    onEdit = { onEdit(tx) },
                                    onDelete = { pendingDelete = tx }
                                )
                            }

                            if (filteredByPill.size > 5) {
                                TextButton(
                                    onClick = { showAllTransactions = !showAllTransactions },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        text = if (showAllTransactions) "Mostrar menos" else "Mostrar mais (${filteredByPill.size - 5})",
                                        fontSize = 12.sp,
                                        color = FinflowPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Full Month History Button
                Button(
                    onClick = { onNavigateToTransactions(uiState.selectedYearMonth) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinflowSurfaceContainerLow)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = FinflowPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver Todo o Histórico de ${uiState.monthTitle} (${allTxs.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinflowPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Pending review bottom sheet
    if (showPendingSheet) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
        PendingReviewBottomSheet(
            sheetState = sheetState,
            pendingList = uiState.pendingTransactions,
            categories = uiState.categoryMap,
            onDismiss = { showPendingSheet = false },
            investmentAssets = uiState.investmentAssets,
            onConfirm = { pending, cat, ctx ->
                viewModel.confirmPending(pending, cat, ctx)
            },
            onConfirmInvestment = { pending, assetId, categoryId ->
                viewModel.confirmPending(pending, chosenCategoryId = categoryId, investmentAssetId = assetId)
            },
            onEditFull = { pending ->
                showPendingSheet = false
                onEditPending(pending)
            },
            onDiscard = { id ->
                viewModel.discardPending(id)
            },
            onConfirmAll = {
                viewModel.confirmAllPending()
            },
            onUpdatePending = { pending ->
                viewModel.updatePendingItem(pending)
            }
        )
    }

    // Delete Confirmation Dialog
    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = "Eliminar Movimento", fontWeight = FontWeight.Bold) },
            text = { Text("Tens a certeza que queres eliminar '${tx.merchant}' (${MoneyCents(tx.amountCents).formatPt()})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinflowCoral)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    refundAmountCents: Long = 0L,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    showActions: Boolean = true,
    parentCategory: CategoryEntity? = null
) {
    FinflowTransactionRow(
        transaction = transaction,
        category = category,
        onEdit = onEdit,
        onDelete = onDelete,
        parentCategory = parentCategory
    )
}

@Composable
fun FinflowTransactionRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    parentCategory: CategoryEntity? = null
) {
    val rootCategoryName = (parentCategory ?: category)?.name
    val language = LocalAppLanguage.current
    val isEnglish = language == AppLanguage.EN
    val isExpense = transaction.type.equals("EXPENSE", ignoreCase = true)
    val date = LocalDate.ofEpochDay(transaction.epochDay)
    val now = LocalDate.now()
    val dateLabel = when {
        date == now -> if (isEnglish) "Today" else "Hoje"
        date == now.minusDays(1) -> if (isEnglish) "Yesterday" else "Ontem"
        else -> date.format(DateTimeFormatter.ofPattern(if (isEnglish) "MMM dd" else "dd MMM", language.locale))
    }

    val iconVector: ImageVector = when {
        rootCategoryName?.contains("Aliment", ignoreCase = true) == true ||
                rootCategoryName?.contains("Mercado", ignoreCase = true) == true -> Icons.Default.Restaurant
        rootCategoryName?.contains("Transp", ignoreCase = true) == true -> Icons.Default.DirectionsCar
        rootCategoryName?.contains("Habita", ignoreCase = true) == true -> Icons.Default.Home
        rootCategoryName?.contains("Saúde", ignoreCase = true) == true -> Icons.Default.LocalHospital
        else -> Icons.Default.ShoppingBag
    }

    val iconBgColor = when {
        !isExpense -> FinflowSecondaryLight
        rootCategoryName?.contains("Aliment", ignoreCase = true) == true -> FinflowAmberLight
        rootCategoryName?.contains("Transp", ignoreCase = true) == true -> FinflowSkyLight
        rootCategoryName?.contains("Habita", ignoreCase = true) == true -> FinflowPurpleLight
        rootCategoryName?.contains("Saúde", ignoreCase = true) == true -> FinflowCoralLight
        else -> FinflowSurfaceContainerLow
    }

    val iconTintColor = when {
        !isExpense -> FinflowSecondary
        rootCategoryName?.contains("Aliment", ignoreCase = true) == true -> FinflowAmber
        rootCategoryName?.contains("Transp", ignoreCase = true) == true -> FinflowSky
        rootCategoryName?.contains("Habita", ignoreCase = true) == true -> FinflowPurple
        rootCategoryName?.contains("Saúde", ignoreCase = true) == true -> FinflowCoral
        else -> FinflowPrimary
    }

    val rawCategoryName = category?.name ?: if (isEnglish) "Other" else "Outros"
    val localizedCategoryName = CategoryTranslator.translateCategory(rawCategoryName, language)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant.ifEmpty { localizedCategoryName },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$localizedCategoryName • $dateLabel",
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isExpense) "-" else "+") + MoneyCents(transaction.amountCents).formatPt(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) FinflowTextPrimary else FinflowSecondary
            )
            val localizedContext = CategoryTranslator.translateContext(transaction.contextTag, language)
            Text(
                text = localizedContext.ifEmpty { if (isEnglish) "General" else "Geral" },
                fontSize = 11.sp,
                color = FinflowTextMuted
            )
        }
    }
}
