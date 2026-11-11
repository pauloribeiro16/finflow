package com.financas.app.ui.screens.budgets

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.usecase.BudgetProgress
import com.financas.app.domain.usecase.BudgetStatus
import com.financas.app.domain.usecase.GoalProgress
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowAmberLight
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
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.i18n.LocalPrivacyMode
import java.time.YearMonth

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
                text = tr("Metas de Poupança", "Savings Goals"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
            Text(
                text = tr("Crescimento de património e objetivos de vida", "Wealth growth and life goals"),
                fontSize = 12.sp,
                color = FinflowTextSecondary
            )
        }

        TextButton(onClick = onAddNewGoal) {
            Text(
                text = tr("+ Nova Meta", "+ New Goal"),
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
                text = tr("Adiciona metas como Fundo de Emergência ou Férias.", "Add goals such as an Emergency Fund or Holidays."),
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
                        text = tr("ORÇAMENTO GLOBAL", "OVERALL BUDGET"),
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
                        text = tr("• ${MoneyCents.formatPercent(uiState.percentUsed)} Usado", "• ${MoneyCents.formatPercent(uiState.percentUsed)} Used"),
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
                        text = tr("teto planeado de ${MoneyCents(uiState.totalLimitCents).formatPt(isPrivacyMode)}", "planned cap of ${MoneyCents(uiState.totalLimitCents).formatPt(isPrivacyMode)}"),
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
                        text = if (uiState.totalRemainingCents < 0) "Excedido" else tr("Disponível", "Available"),
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
                        text = tr("Faltam ${uiState.daysRemainingInMonth} dias para o fim do mês", "${uiState.daysRemainingInMonth} days left until the end of the month"),
                        fontSize = 11.sp,
                        color = FinflowTextSecondary
                    )
                }

                Text(
                    text = if (isPrivacyMode) tr("Média: ${MoneyCents.MASKED_STRING}/dia", "Average: ${MoneyCents.MASKED_STRING}/day") else tr("Média: ${MoneyCents(uiState.dailyBudgetRemainingCents).formatPt(false)}/dia", "Average: ${MoneyCents(uiState.dailyBudgetRemainingCents).formatPt(false)}/day"),
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
            text = tr("Novo Orçamento de Categoria", "New Category Budget"),
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
                text = tr("Categorias Ativas", "Active Categories"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
            Text(
                text = tr("${uiState.categoryBudgets.size} orçamentos sob monitorização", "${uiState.categoryBudgets.size} budgets being tracked"),
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
                text = tr("Filtrar", "Filter"),
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
                text = tr("Ainda não tens orçamentos configurados para este mês.", "You don't have any budgets set up for this month yet."),
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
                        text = tr("Regras & Automatismos", "Rules & Automations"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = tr("Gestão inteligente de excedentes", "Smart surplus management"),
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
                        text = tr("Transição Automática (Rollover)", "Automatic Carry-over (Rollover)"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = tr("Transferir sobra do mês anterior para o orçamento seguinte.", "Carry over last month's surplus to the next budget."),
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
        BudgetStatus.WARNING -> tr("Atenção", "Warning")
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
                    text = tr("${MoneyCents(budgetItem.spentCents).formatPt(isPrivacyMode)} de ${MoneyCents(budgetItem.limitCents).formatPt(isPrivacyMode)}", "${MoneyCents(budgetItem.spentCents).formatPt(isPrivacyMode)} of ${MoneyCents(budgetItem.limitCents).formatPt(isPrivacyMode)}"),
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
                        text = tr("de ${MoneyCents(goal.targetCents).formatPt(isPrivacyMode)}", "of ${MoneyCents(goal.targetCents).formatPt(isPrivacyMode)}"),
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val remainingCents = (goal.targetCents - goal.accumulatedSavingsCents).coerceAtLeast(0L)
                    if (isComplete) {
                        Text(
                            text = tr("Concluída!", "Completed!"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinflowSecondary
                        )
                        Text(
                            text = tr("Objetivo atingido", "Goal reached"),
                            fontSize = 11.sp,
                            color = FinflowTextMuted
                        )
                    } else {
                        Text(
                            text = tr("Faltam ${MoneyCents(remainingCents).formatPt(isPrivacyMode)}", "${MoneyCents(remainingCents).formatPt(isPrivacyMode)} to go"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowTextPrimary
                        )
                        if (goal.monthsRemaining != null) {
                            Text(
                                text = tr("${goal.monthsRemaining} meses restantes", "${goal.monthsRemaining} months left"),
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
                                text = if (isPrivacyMode) tr("Ritmo médio: ${MoneyCents.MASKED_STRING}/mês", "Average pace: ${MoneyCents.MASKED_STRING}/month") else tr("Ritmo médio: +${MoneyCents(goal.averageMonthlySavingsCents).formatPt(false)}/mês", "Average pace: +${MoneyCents(goal.averageMonthlySavingsCents).formatPt(false)}/month"),
                                fontSize = 11.sp,
                                color = FinflowTextMuted
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (goal.onTrack != null && !isComplete) {
                        val trackText = if (goal.onTrack == true) "No ritmo planeado" else tr("Abaixo do ritmo", "Below pace")
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
                text = tr("Novo Orçamento", "New Budget"),
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
                        value = selectedCategory?.name ?: tr("Selecionar Categoria", "Select Category"),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(tr("Categoria", "Category")) },
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
                    label = { Text(tr("Teto Mensal (€)", "Monthly Cap (€)")) },
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
                Text(tr("Criar", "Create"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"), color = FinflowTextSecondary)
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
                text = tr("Nova Meta de Poupança", "New Savings Goal"),
                fontWeight = FontWeight.Bold,
                color = FinflowTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Nome da Meta", "Goal Name")) },
                    placeholder = { Text(tr("Ex: Férias de Verão", "E.g. Summer Holidays")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text(tr("Montante Alvo (€)", "Target Amount (€)")) },
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
                Text(tr("Criar", "Create"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"), color = FinflowTextSecondary)
            }
        }
    )
}
