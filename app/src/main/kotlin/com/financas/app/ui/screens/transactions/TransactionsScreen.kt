package com.financas.app.ui.screens.transactions

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.seed.DefaultSeed
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.screens.home.TransactionRowItem
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onEdit: (TransactionEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Real-Time Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = {
                Text(
                    text = "Pesquisar por estabelecimento, categoria, contexto...",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Pesquisar",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Filter Summary Card: X movimentos · Total: Y,YY € (Z% do recebido)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${uiState.filteredCount} movimentos",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    AnimatedContent(
                        targetState = if (uiState.flowFilter == FlowFilter.ALL) {
                            "Gasto: ${MoneyCents.formatCents(uiState.filteredExpenseCents)} · " +
                                    "Recebido: ${MoneyCents.formatCents(uiState.filteredIncomeCents)}"
                        } else {
                            MoneyCents.formatCents(uiState.filteredTotalCents)
                        },
                        transitionSpec = {
                            fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                        },
                        label = "TransactionsSummaryValue"
                    ) { formattedText ->
                        Text(
                            text = formattedText,
                            fontSize = if (uiState.flowFilter == FlowFilter.ALL) 14.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                val showShare = if (uiState.flowFilter == FlowFilter.ALL) {
                    uiState.filteredExpenseCents > 0L && uiState.periodIncomeCents > 0L
                } else {
                    uiState.periodIncomeCents > 0L
                }

                if (showShare) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Peso no Recebido",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = MoneyCents.formatPercent(uiState.filteredIncomeSharePercent),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Filter Capsules
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Period + Flow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Period filters
                for (period in PeriodFilter.entries) {
                    val isSelected = uiState.periodFilter == period
                    FilterCapsule(
                        label = period.label,
                        isSelected = isSelected,
                        activeColor = AccentBlue,
                        onClick = { viewModel.onPeriodFilterChanged(period) }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Flow filters
                for (flow in FlowFilter.entries) {
                    val isSelected = uiState.flowFilter == flow
                    FilterCapsule(
                        label = flow.label,
                        isSelected = isSelected,
                        activeColor = AccentEmerald,
                        onClick = { viewModel.onFlowFilterChanged(flow) }
                    )
                }
            }

            // Row 2: Context chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (ctx in DefaultSeed.CONTEXTS) {
                    val isSelected = uiState.contextFilter == ctx.tag
                    FilterCapsule(
                        label = ctx.label,
                        isSelected = isSelected,
                        activeColor = AccentBlue,
                        onClick = { viewModel.onContextFilterChanged(ctx.tag) }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                for (pm in DefaultSeed.PAYMENT_METHODS) {
                    val isSelected = uiState.methodFilter == pm.code
                    FilterCapsule(
                        label = pm.label,
                        isSelected = isSelected,
                        activeColor = AccentEmerald,
                        onClick = { viewModel.onMethodFilterChanged(pm.code) }
                    )
                }
            }

            // Row 3: Amount bands
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (amount in AmountFilter.entries) {
                    val isSelected = uiState.amountFilter == amount
                    FilterCapsule(
                        label = amount.label,
                        isSelected = isSelected,
                        activeColor = AccentCoral,
                        onClick = { viewModel.onAmountFilterChanged(amount) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transaction List Grouped by Date
        val groupedByDate = uiState.filteredTransactions.groupBy { it.epochDay }

        if (uiState.filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum movimento encontrado para estes filtros.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((epochDay, transactions) in groupedByDate) {
                    val date = LocalDate.ofEpochDay(epochDay)
                    val dateHeader = formatEpochHeader(date)

                    item(key = "header_$epochDay") {
                        Text(
                            text = dateHeader,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }

                    items(transactions, key = { it.id }) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            category = uiState.categoryMap[tx.categoryId],
                            refundAmountCents = uiState.refundsMap[tx.id] ?: 0L,
                            onDelete = { pendingDelete = tx },
                            onEdit = { onEdit(tx) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    "Apagar movimento?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Esta ação é definitiva e não pode ser anulada.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCoral)
                ) {
                    Text("Apagar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun FilterCapsule(
    label: String,
    isSelected: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.22f) else DarkSurfaceVariant,
        animationSpec = tween(180),
        label = "filterCapsuleBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else DarkBorder,
        animationSpec = tween(180),
        label = "filterCapsuleBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) TextPrimary else TextSecondary,
        animationSpec = tween(180),
        label = "filterCapsuleText"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "filterCapsuleScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun formatEpochHeader(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date == today -> "Hoje · " + date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "PT")))
        date == today.minusDays(1) -> "Ontem · " + date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("pt", "PT")))
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", Locale("pt", "PT")))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }
    }
}
