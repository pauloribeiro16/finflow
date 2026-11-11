package com.financas.app.ui.screens.budgets

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.model.MoneyCents
import com.financas.app.ui.theme.FinflowAmber
import com.financas.app.ui.theme.FinflowBorder
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowCoral
import com.financas.app.ui.theme.FinflowPrimary
import com.financas.app.ui.theme.FinflowPrimaryContainer
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainer
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextMuted
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.theme.FinflowTextInverse
import com.financas.app.ui.i18n.LocalPrivacyMode
import java.util.Locale

@Composable
fun MarketQuotesDisclosureDialog(
    onDismiss: () -> Unit,
    onConfirm: (enableDaily: Boolean) -> Unit
) {
    var enableDaily by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinflowSurface,
        title = { Text(tr("Consultar cotações", "Fetch quotes"), fontWeight = FontWeight.Bold, color = FinflowTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("Para estimar o valor atual, o Finflow pede ao Yahoo Finance (query1.finance.yahoo.com) a cotação de cada ticker que guardou. O Yahoo fica a saber esses tickers e o endereço IP do telemóvel. Não são enviadas quantidades, valores nem dados pessoais.", "To estimate the current value, Finflow asks Yahoo Finance (query1.finance.yahoo.com) for the quote of each ticker you saved. Yahoo learns those tickers and the phone's IP address. No quantities, amounts or personal data are sent."),
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tr("Atualizar automaticamente uma vez por dia", "Update automatically once a day"),
                        fontSize = 13.sp,
                        color = FinflowTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = enableDaily, onCheckedChange = { enableDaily = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(enableDaily) },
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text(tr("Consultar", "Fetch"), color = FinflowTextInverse, fontWeight = FontWeight.Bold)
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
fun AddMoneyDialog(
    asset: InvestmentAssetEntity,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val amountCents = parseEuroToCents(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinflowSurface,
        title = {
            Column {
                Text(tr("Reforçar investimento", "Top up investment"), fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                Text(text = asset.name, fontSize = 12.sp, color = FinflowPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr("Soma ao capital investido e ao valor atual, ficando registado como transferência (não conta como despesa).", "Adds to the invested capital and the current value, recorded as a transfer (not counted as an expense)."),
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(tr("Valor do reforço (€)", "Top-up amount (€)"), color = FinflowTextSecondary) },
                    placeholder = { Text(tr("ex.: 100,00", "e.g. 100,00"), color = FinflowTextMuted) },
                    trailingIcon = {
                        Text(
                            text = "€",
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextSecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FinflowTextPrimary,
                        unfocusedTextColor = FinflowTextPrimary,
                        focusedBorderColor = FinflowPrimary,
                        unfocusedBorderColor = FinflowBorder,
                        focusedLabelColor = FinflowPrimary,
                        unfocusedLabelColor = FinflowTextSecondary,
                        focusedContainerColor = FinflowSurface,
                        unfocusedContainerColor = FinflowSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountCents) },
                enabled = amountCents > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text(tr("Reforçar", "Top up"), color = FinflowTextInverse, fontWeight = FontWeight.Bold)
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
fun AllocateAssetDialog(
    source: InvestmentAssetEntity,
    targets: List<InvestmentAssetEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    val maxCents = source.currentValueCents
    var selectedTargetId by remember { mutableStateOf<String?>(null) }
    var amountText by remember {
        mutableStateOf(MoneyCents.formatForInput(maxCents))
    }
    val amountCents = parseEuroToCents(amountText)
    val isValid = selectedTargetId != null && amountCents > 0L && amountCents <= maxCents
    val selectedTarget = targets.firstOrNull { it.id == selectedTargetId }
    val isPrivacyMode = LocalPrivacyMode.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinflowSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("Alocar Saldo", "Allocate Balance"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FinflowTextPrimary)
                Text(
                    text = tr("${source.name} · Saldo disponível: ${MoneyCents(maxCents).formatPt(isPrivacyMode)}", "${source.name} · Available balance: ${MoneyCents(maxCents).formatPt(isPrivacyMode)}"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = FinflowPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr("Escolhe o ativo de destino:", "Choose the destination holding:"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FinflowTextSecondary
                )
                if (targets.isEmpty()) {
                    Text(
                        text = tr("Ainda não tens outros ativos criados. Adiciona primeiro uma aplicação (ex.: um ETF ou PPR) em \"Nova Aplicação\".", "You don't have any other holdings yet. First add one (e.g. an ETF or PPR) under \"New Holding\"."),
                        fontSize = 12.sp,
                        color = FinflowTextMuted
                    )
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        targets.forEach { target ->
                            val isSelected = selectedTargetId == target.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) FinflowPrimary else FinflowSurfaceContainer)
                                    .clickable { selectedTargetId = target.id }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = target.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) FinflowTextInverse else FinflowTextPrimary
                                    )
                                    Text(
                                        text = target.institution,
                                        fontSize = 11.sp,
                                        color = if (isSelected) FinflowTextInverse.copy(alpha = 0.8f) else FinflowTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(tr("Valor a alocar (€)", "Amount to allocate (€)"), color = FinflowTextSecondary) },
                    placeholder = { Text(tr("Insere qualquer quantia", "Enter any amount"), color = FinflowTextMuted) },
                    trailingIcon = {
                        Text(
                            text = "€",
                            fontWeight = FontWeight.Bold,
                            color = FinflowTextSecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FinflowTextPrimary,
                        unfocusedTextColor = FinflowTextPrimary,
                        focusedBorderColor = FinflowPrimary,
                        unfocusedBorderColor = FinflowBorder,
                        focusedLabelColor = FinflowPrimary,
                        unfocusedLabelColor = FinflowTextSecondary,
                        focusedContainerColor = FinflowSurface,
                        unfocusedContainerColor = FinflowSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Atalhos modulares rápidos: Percentagens calculadas dinamicamente
                val pct25 = MoneyCents.formatForInput(maxCents / 4L)
                val pct50 = MoneyCents.formatForInput(maxCents / 2L)
                val pct75 = MoneyCents.formatForInput((maxCents * 3L) / 4L)
                val pct100 = MoneyCents.formatForInput(maxCents)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "25%" to pct25,
                        "50%" to pct50,
                        "75%" to pct75,
                        "Tudo" to pct100
                    ).forEach { (label, value) ->
                        val isPicked = amountText.trim() == value.trim()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPicked) FinflowPrimary else FinflowSurfaceContainer)
                                .clickable { amountText = value }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPicked) FinflowTextInverse else FinflowTextPrimary
                            )
                        }
                    }
                }

                // Feedback em tempo real sobre a alocação e o remanescente
                if (amountCents > maxCents) {
                    Text(
                        text = tr("O valor excede o saldo disponível de ${MoneyCents(maxCents).formatPt(isPrivacyMode)}.", "The amount exceeds the available balance of ${MoneyCents(maxCents).formatPt(isPrivacyMode)}."),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinflowCoral
                    )
                } else if (amountCents in 1L until maxCents) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "A alocar: ${MoneyCents(amountCents).formatPt(isPrivacyMode)}${if (selectedTarget != null) " ➔ ${selectedTarget.name}" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinflowTextPrimary
                        )
                        Text(
                            text = tr("Ficarão ${MoneyCents(maxCents - amountCents).formatPt(isPrivacyMode)} em espera para alocares depois.", "${MoneyCents(maxCents - amountCents).formatPt(isPrivacyMode)} will stay on hold for you to allocate later."),
                            fontSize = 11.sp,
                            color = FinflowAmber
                        )
                    }
                } else if (amountCents == maxCents && maxCents > 0L) {
                    Text(
                        text = tr("Todo o saldo disponível (${MoneyCents(maxCents).formatPt(isPrivacyMode)}) será alocado.", "The full available balance (${MoneyCents(maxCents).formatPt(isPrivacyMode)}) will be allocated."),
                        fontSize = 11.sp,
                        color = FinflowSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedTargetId?.let { onConfirm(it, amountCents) } },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinflowPrimary,
                    disabledContainerColor = FinflowSurfaceContainer
                )
            ) {
                Text(
                    text = tr("Confirmar Alocação", "Confirm Allocation"),
                    color = if (isValid) FinflowTextInverse else FinflowTextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"), color = FinflowTextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInvestmentSheet(
    initialAsset: InvestmentAssetEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: String,
        institution: String,
        investedCents: Long,
        currentValueCents: Long,
        monthlyContributionCents: Long,
        notes: String,
        ticker: String?,
        units: Double?
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialAsset?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialAsset?.let { InvestmentType.fromKey(it.type) } ?: InvestmentType.PPR) }
    var institution by remember { mutableStateOf(initialAsset?.institution ?: "") }
    var investedText by remember { mutableStateOf(initialAsset?.let { MoneyCents.formatForInput(it.investedCents) } ?: "") }
    var currentValueText by remember { mutableStateOf(initialAsset?.let { MoneyCents.formatForInput(it.currentValueCents) } ?: "") }
    var monthlyText by remember { mutableStateOf(initialAsset?.let { if (it.monthlyContributionCents > 0) MoneyCents.formatForInput(it.monthlyContributionCents) else "" } ?: "") }
    var ticker by remember { mutableStateOf(initialAsset?.ticker ?: "") }
    var unitsText by remember { mutableStateOf(initialAsset?.units?.let { String.format(Locale.ROOT, "%.4f", it) } ?: "") }
    var notes by remember { mutableStateOf(initialAsset?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FinflowSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialAsset == null) tr("Nova Aplicação / Ativo", "New Holding / Asset") else tr("Editar Aplicação", "Edit Holding"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = tr("Regista os teus investimentos (PPR, ETFs, Contas a Prazo)", "Track your investments (PPR, ETFs, term deposits)"),
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FinflowSurfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr("Fechar", "Close"),
                        tint = FinflowTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 1. Tipo de Ativo
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tr("Tipo de Ativo", "Asset Type"),
                    fontSize = 13.sp,
                    color = FinflowTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InvestmentType.entries.forEach { typeOption ->
                        val isSelected = selectedType == typeOption
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) FinflowPrimaryContainer else FinflowSurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) FinflowPrimary else FinflowBorderSubtle,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedType = typeOption }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${emojiForInvestmentType(typeOption)} ${typeOption.label}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else FinflowTextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Nome da Aplicação
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                isError = nameError,
                label = { Text(tr("Nome da Aplicação *", "Holding Name *")) },
                placeholder = { Text(tr("Ex: PPR Agressivo, Core S&P 500 ETF, DP 3.5%", "E.g. Aggressive PPR, Core S&P 500 ETF, 3.5% term deposit")) },
                supportingText = if (nameError) {
                    { Text(tr("O nome da aplicação é obrigatório", "The holding name is required"), color = FinflowCoral) }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Entidade / Banco / Corretora
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text(tr("Entidade / Corretora / Banco", "Provider / Broker / Bank")) },
                    placeholder = { Text(tr("Ex: Optimize, XTB, CGD, Trade Republic, IGCP", "E.g. Optimize, XTB, CGD, Trade Republic, IGCP")) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Optimize", "XTB", "CGD", "Trade Republic", "Banco Invest", "IGCP / CTT").forEach { inst ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FinflowSurfaceContainerLow)
                                .border(1.dp, FinflowBorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { institution = inst }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = inst,
                                fontSize = 11.sp,
                                color = FinflowTextSecondary
                            )
                        }
                    }
                }
            }

            // 4. Montantes: Capital Investido e Valor Atual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = investedText,
                    onValueChange = { investedText = it },
                    label = { Text(tr("Capital Investido (€)", "Invested Capital (€)")) },
                    placeholder = { Text("Ex: 1000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currentValueText,
                    onValueChange = { currentValueText = it },
                    label = { Text(tr("Saldo Atual (€)", "Current Balance (€)")) },
                    placeholder = { Text("Ex: 1150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 5. Reforço Mensal Opcional
            OutlinedTextField(
                value = monthlyText,
                onValueChange = { monthlyText = it },
                label = { Text(tr("Reforço Mensal (€/mês) — Opcional", "Monthly Top-up (€/month) — Optional")) },
                placeholder = { Text(tr("Ex: 200 (para PPR Agressivo)", "E.g. 200 (for an aggressive PPR)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Ticker Yahoo Finance & Quantidade de Unidades
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it.uppercase() },
                    label = { Text("Ticker (Yahoo)") },
                    placeholder = { Text(tr("ex: LQQ.PA", "e.g. LQQ.PA")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unitsText,
                    onValueChange = { unitsText = it.replace(',', '.') },
                    label = { Text(tr("Unidades / Cotas", "Units / Shares")) },
                    placeholder = { Text(tr("ex: 26.2636", "e.g. 26.2636")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // 7. Notas
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(tr("Notas Adicionais — Opcional", "Additional Notes — Optional")) },
                placeholder = { Text(tr("Benefício fiscal, maturidade, etc.", "Tax benefit, maturity, etc.")) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Botão Guardar em Destaque
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val investedCents = parseEuroToCents(investedText)
                    val rawValueCents = parseEuroToCents(currentValueText)
                    val valueCents = if (rawValueCents > 0L) rawValueCents else investedCents
                    val finalInvestedCents = if (investedCents > 0L) investedCents else valueCents
                    val monthlyCents = parseEuroToCents(monthlyText)
                    val finalTicker = ticker.trim().takeIf { it.isNotBlank() }
                    val finalUnits = unitsText.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
                    onSave(
                        name.trim(),
                        selectedType.key,
                        institution.trim().ifBlank { "Geral" },
                        finalInvestedCents,
                        valueCents,
                        monthlyCents,
                        notes.trim(),
                        finalTicker,
                        finalUnits
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text(
                    text = if (initialAsset == null) tr("Guardar Aplicação", "Save Holding") else tr("Guardar Alterações", "Save Changes"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun QuickUpdateAssetDialog(
    asset: InvestmentAssetEntity,
    onDismiss: () -> Unit,
    onUpdate: (Long) -> Unit
) {
    var newAmountText by remember {
        mutableStateOf(MoneyCents.formatForInput(asset.currentValueCents))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = tr("Atualizar Saldo", "Update Balance"),
                    fontWeight = FontWeight.Bold,
                    color = FinflowTextPrimary
                )
                Text(
                    text = asset.name,
                    fontSize = 13.sp,
                    color = FinflowPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr("Valor registado anterior: ${MoneyCents(asset.currentValueCents).formatPt()}", "Previously recorded value: ${MoneyCents(asset.currentValueCents).formatPt()}"),
                    fontSize = 12.sp,
                    color = FinflowTextSecondary
                )
                OutlinedTextField(
                    value = newAmountText,
                    onValueChange = { newAmountText = it },
                    label = { Text(tr("Novo Valor de Mercado (€)", "New Market Value (€)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = parseEuroToCents(newAmountText)
                    if (cents >= 0L) {
                        onUpdate(cents)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimary)
            ) {
                Text(tr("Atualizar", "Update"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancelar", "Cancel"), color = FinflowTextSecondary)
            }
        }
    )
}
