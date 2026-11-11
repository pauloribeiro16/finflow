package com.financas.app.ui.screens.analytics

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.CategoryEntity
import com.financas.app.domain.model.DetectedSubscription
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.model.SubscriptionReport
import com.financas.app.ui.components.EffortBar
import com.financas.app.ui.components.HorizontalParetoBars
import com.financas.app.ui.components.ParetoBarItem
import com.financas.app.ui.theme.AccentAmber
import com.financas.app.ui.theme.AccentBlue
import com.financas.app.ui.theme.AccentCoral
import com.financas.app.ui.theme.AccentEmerald
import com.financas.app.ui.theme.DarkBorder
import com.financas.app.ui.theme.DarkSurface
import com.financas.app.ui.theme.DarkSurfaceVariant
import com.financas.app.ui.theme.TextMuted
import com.financas.app.ui.theme.TextPrimary
import com.financas.app.ui.theme.TextSecondary
import com.financas.app.ui.i18n.LocalPrivacyMode

// -----------------------------------------------------------------------------
// VISTA 2: Onde (Contextos & Top Estabelecimentos)
// -----------------------------------------------------------------------------
@Composable
internal fun OndeView(uiState: AnalyticsUiState) {
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
                        text = tr("Gastos por Contexto", "Spending by Context"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = tr("Onde o teu dinheiro foi gasto este mês", "Where your money went this month"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (uiState.contexts.isEmpty()) {
                    Text(
                        text = tr("Sem gastos registados neste mês.", "No spending recorded this month."),
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
                            secondaryText = tr("${ctx.transactionCount} movimentos · ${MoneyCents.formatPercent(ctx.percentageOfExpense)} dos gastos", "${ctx.transactionCount} transactions · ${MoneyCents.formatPercent(ctx.percentageOfExpense)} of spending")
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
                        text = tr("Top 10 Estabelecimentos", "Top 10 Merchants"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = tr("Maiores valores acumulados e média por ida", "Highest totals and average per visit"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (uiState.topMerchants.isEmpty()) {
                    Text(
                        text = tr("Sem estabelecimentos registados neste mês.", "No merchants recorded this month."),
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
                                        text = tr("${merchant.transactionCount} idas · média ${merchant.formattedAverageTicket}", "${merchant.transactionCount} visits · average ${merchant.formattedAverageTicket}"),
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
internal fun ComoView(uiState: AnalyticsUiState) {
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
                        text = tr("Estrutura de Rigidez", "Fixed-Cost Structure"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = tr("Fixos vs. Variáveis vs. Poupança Líquida", "Fixed vs. Variable vs. Net Savings"),
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
                        title = tr("Fixos", "Fixed"),
                        value = rigidity.formattedFixed,
                        percent = rigidity.fixedPercentOfIncome,
                        indicatorColor = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    RigidityMiniCard(
                        title = tr("Variáveis", "Variable"),
                        value = rigidity.formattedVariable,
                        percent = rigidity.variablePercentOfIncome,
                        indicatorColor = AccentCoral,
                        modifier = Modifier.weight(1f)
                    )
                    RigidityMiniCard(
                        title = tr("Poupança", "Savings"),
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
                        text = tr("Meios de Pagamento", "Payment Methods"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = tr("Como movimentas o teu dinheiro", "How you move your money"),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (uiState.paymentMethods.isEmpty()) {
                    Text(
                        text = tr("Sem pagamentos registados neste mês.", "No payments recorded this month."),
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
                                    text = tr("${MoneyCents.formatPercent(method.percentageOfExpense)} dos gastos", "${MoneyCents.formatPercent(method.percentageOfExpense)} of spending"),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = tr("${method.transactionCount} mov.", "${method.transactionCount} txns"),
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
internal fun RigidityMiniCard(
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
                text = tr("${MoneyCents.formatPercent(percent)} do rec.", "${MoneyCents.formatPercent(percent)} of inc."),
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
                        text = tr("Subscrições & Custos Recorrentes", "Subscriptions & Recurring Costs"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = tr("Compromissos mensais e impacto no orçamento", "Monthly commitments and budget impact"),
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
                            text = tr("${subscriptionReport.subscriptions.size} ativas", "${subscriptionReport.subscriptions.size} active"),
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
                            text = tr("Total Mensal", "Monthly Total"),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val isPrivacy = LocalPrivacyMode.current
                        Text(
                            text = if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(subscriptionReport.totalMonthlyCents, false),
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
                            text = tr("Custo Anualizado", "Annualised Cost"),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val isPrivacy = LocalPrivacyMode.current
                        Text(
                            text = if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(subscriptionReport.totalAnnualCents, false),
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
                            val isPrivacy = LocalPrivacyMode.current
                            val hikeText = when {
                                isPrivacy -> if (subscriptionReport.hikesCount == 1) {
                                    tr("1 subscrição aumentou de preço este mês (${MoneyCents.MASKED_STRING})", "1 subscription went up in price this month (${MoneyCents.MASKED_STRING})")
                                } else {
                                    tr("${subscriptionReport.hikesCount} subscrições aumentaram de preço este mês (${MoneyCents.MASKED_STRING})", "${subscriptionReport.hikesCount} subscriptions went up in price this month (${MoneyCents.MASKED_STRING})")
                                }
                                subscriptionReport.hikesCount == 1 -> tr("1 subscrição aumentou de preço este mês (+${MoneyCents.formatCents(subscriptionReport.totalHikeIncreaseCents, false)}/mês)", "1 subscription went up in price this month (+${MoneyCents.formatCents(subscriptionReport.totalHikeIncreaseCents, false)}/month)")
                                else -> tr("${subscriptionReport.hikesCount} subscrições aumentaram de preço este mês (+${MoneyCents.formatCents(subscriptionReport.totalHikeIncreaseCents, false)}/mês)", "${subscriptionReport.hikesCount} subscriptions went up in price this month (+${MoneyCents.formatCents(subscriptionReport.totalHikeIncreaseCents, false)}/month)")
                            }
                            Text(
                                text = tr("Atenção: Subida de Mensalidade", "Warning: Subscription Price Increase"),
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
                    text = tr("Sem subscrições ou custos recorrentes detetados.", "No subscriptions or recurring costs detected."),
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
internal fun SubscriptionItemRow(
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
                                text = tr("Fixo", "Fixed"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentBlue
                            )
                        }
                    }
                }

                val isPrivacy = LocalPrivacyMode.current
                Text(
                    text = if (isPrivacy) tr("${MoneyCents.MASKED_STRING} / mês", "${MoneyCents.MASKED_STRING} / month") else tr("${MoneyCents.formatCents(subscription.monthlyAmountCents, false)} / mês", "${MoneyCents.formatCents(subscription.monthlyAmountCents, false)} / month"),
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

                val isPrivacy = LocalPrivacyMode.current
                Text(
                    text = if (isPrivacy) tr("${MoneyCents.MASKED_STRING} / ano", "${MoneyCents.MASKED_STRING} / year") else tr("${MoneyCents.formatCents(subscription.annualizedCostCents, false)} / ano", "${MoneyCents.formatCents(subscription.annualizedCostCents, false)} / year"),
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
                    val isPrivacy = LocalPrivacyMode.current
                    val prevText = subscription.previousAmountCents?.let {
                        if (isPrivacy) "Anterior: ${MoneyCents.MASKED_STRING}" else "Anterior: ${MoneyCents.formatCents(it, false)}"
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
                            text = if (isPrivacy) MoneyCents.MASKED_STRING else "+${MoneyCents.formatCents(subscription.priceDifferenceCents, false)}",
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
