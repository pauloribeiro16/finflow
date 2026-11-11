package com.financas.app.domain.usecase

import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.model.InvestmentType
import com.financas.app.domain.model.XtbOpenPosition
import com.financas.app.domain.model.XtbParsedReport
import com.financas.app.domain.model.XtbSyncResult
import java.time.LocalDate
import java.util.Locale

class XtbSyncEngine(
    private val investmentAssetDao: InvestmentAssetDao,
    private val transactionDao: TransactionDao
) {

    suspend fun sync(
        report: XtbParsedReport,
        importCashOperations: Boolean = true
    ): XtbSyncResult {
        val existingAssets = investmentAssetDao.getAllSync()
        val existingTransactions = transactionDao.getAllSync()

        var updatedAssetsCount = 0
        var newAssetsCount = 0
        var importedTxCount = 0
        var skippedTxCount = 0

        val syncEpochDay = report.reportDateEpochDay ?: LocalDate.now().toEpochDay()

        // 1. Sync Open Positions (Investimentos)
        for (pos in report.openPositions) {
            val mappedTicker = XtbTickerMapper.mapToYahooFinanceTicker(pos.ticker)
            val matchedAsset = findMatchingAsset(pos, existingAssets)
            val catalogNotes = "Posição inicial: ${String.format(Locale.ROOT, "%.4f", pos.volume)} un. a ${String.format(Locale.ROOT, "%.2f", pos.openPrice)} € • XTB · Ticker: $mappedTicker"

            if (matchedAsset != null) {
                // UPDATE existing asset
                val updated = matchedAsset.copy(
                    currentValueCents = pos.currentValueCents,
                    investedCents = pos.investedCents,
                    lastUpdatedEpochDay = syncEpochDay,
                    ticker = mappedTicker,
                    units = pos.volume,
                    notes = if (matchedAsset.notes.isBlank() || matchedAsset.notes.startsWith("XTB • Ticker:")) {
                        catalogNotes
                    } else {
                        matchedAsset.notes
                    }
                )
                investmentAssetDao.update(updated)
                updatedAssetsCount++
            } else {
                // INSERT new asset
                val typeKey = when {
                    pos.category.equals("ETC", ignoreCase = true) || pos.name.contains("Gold", ignoreCase = true) -> InvestmentType.OUTRO.key
                    pos.name.contains("Crypto", ignoreCase = true) -> InvestmentType.ETF.key
                    else -> InvestmentType.ETF.key
                }
                val formattedName = formatAssetName(pos)
                val newAsset = InvestmentAssetEntity(
                    id = "inv_xtb_${mappedTicker.lowercase().replace('.', '_')}",
                    name = formattedName,
                    type = typeKey,
                    institution = "XTB",
                    investedCents = pos.investedCents,
                    currentValueCents = pos.currentValueCents,
                    monthlyContributionCents = 0L,
                    targetCents = null,
                    notes = catalogNotes,
                    lastUpdatedEpochDay = syncEpochDay,
                    archived = false,
                    ticker = mappedTicker,
                    units = pos.volume
                )
                investmentAssetDao.insert(newAsset)
                newAssetsCount++
            }
        }

        // 2. Sync Cash Operations (Zero duplicação garantida por chave anónima determinística)
        if (importCashOperations) {
            for (op in report.cashOperations) {
                val isDividend = op.type.contains("Dividend", ignoreCase = true)
                val isInterest = op.type.contains("Interest", ignoreCase = true) && !op.type.contains("Tax", ignoreCase = true)
                val isTax = op.type.contains("Tax", ignoreCase = true)

                if (!isDividend && !isInterest && !isTax) {
                    continue // Skip stock purchases and deposits (already reflected in portfolio capital)
                }

                val legacyMarker = "[XTB:${op.id}]"
                val operationMarker = XtbTickerMapper.generateAnonymousDeduplicationKey(op.epochDay, op.amountCents, op.type)
                val alreadyExists = existingTransactions.any { it.merchant.contains(operationMarker) || it.merchant.contains(legacyMarker) }

                if (alreadyExists) {
                    skippedTxCount++
                    continue
                }

                val cleanTicker = XtbTickerMapper.mapToYahooFinanceTicker(op.ticker)
                val tx = if (isDividend) {
                    TransactionEntity(
                        type = "INCOME",
                        amountCents = op.amountCents.coerceAtLeast(0L),
                        yearMonth = op.yearMonth,
                        epochDay = op.epochDay,
                        categoryId = "inc_other",
                        contextTag = "PESSOAL",
                        merchant = "XTB Dividendo ${op.instrument.ifBlank { cleanTicker }} $operationMarker",
                        paymentMethod = "TRANSFERENCIA",
                        isFixed = false
                    )
                } else if (isInterest) {
                    TransactionEntity(
                        type = "INCOME",
                        amountCents = op.amountCents.coerceAtLeast(0L),
                        yearMonth = op.yearMonth,
                        epochDay = op.epochDay,
                        categoryId = "inc_other",
                        contextTag = "PESSOAL",
                        merchant = "XTB Juros Fundos Livres $operationMarker",
                        paymentMethod = "TRANSFERENCIA",
                        isFixed = false
                    )
                } else {
                    TransactionEntity(
                        type = "EXPENSE",
                        amountCents = kotlin.math.abs(op.amountCents),
                        yearMonth = op.yearMonth,
                        epochDay = op.epochDay,
                        categoryId = "exp_financial_taxes",
                        contextTag = "PESSOAL",
                        merchant = "XTB Retenção Imposto $operationMarker",
                        paymentMethod = "TRANSFERENCIA",
                        isFixed = false
                    )
                }

                transactionDao.insert(tx)
                importedTxCount++
            }
        }

        return XtbSyncResult(
            updatedAssetsCount = updatedAssetsCount,
            newAssetsCount = newAssetsCount,
            importedTransactionsCount = importedTxCount,
            skippedTransactionsCount = skippedTxCount,
            totalPositionsCount = report.openPositions.size
        )
    }

    private fun findMatchingAsset(pos: XtbOpenPosition, existing: List<InvestmentAssetEntity>): InvestmentAssetEntity? {
        val xtbAssets = existing.filter { it.institution.equals("XTB", ignoreCase = true) }

        // 1. Match by exact ticker in notes or id
        val tickerMatch = xtbAssets.firstOrNull {
            it.notes.contains(pos.ticker, ignoreCase = true) || it.id.contains(pos.ticker.lowercase().replace('.', '_'))
        }
        if (tickerMatch != null) return tickerMatch

        // 2. Match by normalized name
        val normPosName = normalizeName(pos.name)
        return xtbAssets.firstOrNull {
            val normExisting = normalizeName(it.name)
            normExisting == normPosName || normExisting.contains(normPosName) || normPosName.contains(normExisting)
        }
    }

    private fun normalizeName(name: String): String {
        return name.lowercase()
            .replace("etf", "")
            .replace("etc", "")
            .replace("daily", "")
            .replace("lev", "")
            .replace("(", "")
            .replace(")", "")
            .replace("2x", "")
            .trim()
    }

    private fun formatAssetName(pos: XtbOpenPosition): String {
        val base = pos.name.trim()
        return if (pos.category.equals("ETF", ignoreCase = true) && !base.contains("ETF", ignoreCase = true)) {
            "$base ETF"
        } else if (pos.category.equals("ETC", ignoreCase = true) && !base.contains("ETC", ignoreCase = true)) {
            "$base ETC"
        } else {
            base
        }
    }
}
