package com.financas.app.ui.screens.budgets

import com.financas.app.core.i18n.tr

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financas.app.domain.model.MoneyCents
import com.financas.app.domain.model.XtbParsedReport
import com.financas.app.ui.theme.FinflowBorderSubtle
import com.financas.app.ui.theme.FinflowCoral
import com.financas.app.ui.theme.FinflowPrimaryContainer
import com.financas.app.ui.theme.FinflowSecondary
import com.financas.app.ui.theme.FinflowSurface
import com.financas.app.ui.theme.FinflowSurfaceContainer
import com.financas.app.ui.theme.FinflowSurfaceContainerLow
import com.financas.app.ui.theme.FinflowTextPrimary
import com.financas.app.ui.theme.FinflowTextSecondary
import com.financas.app.ui.i18n.LocalPrivacyMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtbImportChooserSheet(
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onAnalyzeText: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FinflowSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
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
                        text = tr("Importar Relatório XTB", "Import XTB Report"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = tr("Processado no telemóvel. Atualiza cotações e posições sem duplicar.", "Processed on the phone. Updates quotes and positions without duplicates."),
                        fontSize = 12.sp,
                        color = FinflowTextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(FinflowSurfaceContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = tr("Fechar", "Close"), tint = FinflowTextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Opção 1: Escolher Ficheiro
            Button(
                onClick = onPickFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimaryContainer)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Escolher Ficheiro XTB (.csv / .xlsx / .txt)", "Choose XTB File (.csv / .xlsx / .txt)"), fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(FinflowBorderSubtle))
                Text(tr("OU COLAR TEXTO DO RELATÓRIO", "OR PASTE THE REPORT TEXT"), fontSize = 11.sp, color = FinflowTextSecondary, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(FinflowBorderSubtle))
            }

            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it },
                placeholder = { Text(tr("Cole aqui o texto CSV exportado da XTB...", "Paste the CSV text exported from XTB here..."), fontSize = 12.sp, color = FinflowTextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { if (rawText.isNotBlank()) onAnalyzeText(rawText) },
                enabled = rawText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowSecondary)
            ) {
                Text(tr("Analisar Texto Colado", "Analyse Pasted Text"), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtbImportPreviewSheet(
    report: XtbParsedReport,
    onDismiss: () -> Unit,
    onConfirmSync: (importCashOperations: Boolean) -> Unit
) {
    var importCashOps by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FinflowSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tr("📊 Relatório XTB Detetado", "📊 XTB Report Detected"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinflowTextPrimary
                    )
                    Text(
                        text = tr("${report.openPositions.size} posições abertas encontradas", "${report.openPositions.size} open positions found"),
                        fontSize = 12.sp,
                        color = FinflowSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(FinflowSurfaceContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = tr("Fechar", "Close"), tint = FinflowTextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // Resumo dos Totais XTB
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FinflowSurfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val isPrivacy = LocalPrivacyMode.current
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tr("Valor de Mercado", "Market Value"), fontSize = 11.sp, color = FinflowTextSecondary)
                        Text(if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(report.openPositionValueCents, false), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tr("Lucro Total", "Total Profit"), fontSize = 11.sp, color = FinflowTextSecondary)
                        val isPos = report.openPositionProfitCents >= 0
                        val pColor = if (isPos) FinflowSecondary else FinflowCoral
                        val pSign = if (isPos) "+" else ""
                        Text(if (isPrivacy) MoneyCents.MASKED_STRING else "$pSign${MoneyCents.formatCents(report.openPositionProfitCents, false)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pColor)
                    }
                }
            }

            // Lista das posições detetadas
            Text(tr("Posições a Sincronizar:", "Positions to Sync:"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FinflowTextPrimary)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPrivacy = LocalPrivacyMode.current
                report.openPositions.forEach { pos ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FinflowSurfaceContainerLow, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pos.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FinflowTextPrimary)
                            Text("${pos.ticker} • Vol: ${pos.volume}", fontSize = 11.sp, color = FinflowTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isPrivacy) MoneyCents.MASKED_STRING else MoneyCents.formatCents(pos.currentValueCents, false), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                            val isPos = pos.profitLossCents >= 0
                            val pColor = if (isPos) FinflowSecondary else FinflowCoral
                            val pSign = if (isPos) "+" else ""
                            Text(if (isPrivacy) MoneyCents.MASKED_STRING else "$pSign${MoneyCents.formatCents(pos.profitLossCents, false)} ($pSign${MoneyCents.formatPercent(pos.profitLossPercentage)})", fontSize = 11.sp, color = pColor)
                        }
                    }
                }
            }

            // Secção de Operações de Caixa
            if (report.cashOperations.isNotEmpty()) {
                val dividendsAndInterest = report.cashOperations.filter {
                    it.type.contains("Dividend", ignoreCase = true) || it.type.contains("Interest", ignoreCase = true)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FinflowSurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tr("Registar Dividendos e Juros", "Record Dividends and Interest"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinflowTextPrimary)
                                Text(tr("${dividendsAndInterest.size} movimentos detetados (com anti-duplicação)", "${dividendsAndInterest.size} transactions detected (with de-duplication)"), fontSize = 11.sp, color = FinflowTextSecondary)
                            }
                            Switch(
                                checked = importCashOps,
                                onCheckedChange = { importCashOps = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FinflowSecondary)
                            )
                        }
                    }
                }
            }

            // Botão Confirmar Sincronização
            Button(
                onClick = { onConfirmSync(importCashOps) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinflowPrimaryContainer)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Confirmar Sincronização", "Confirm Sync"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
