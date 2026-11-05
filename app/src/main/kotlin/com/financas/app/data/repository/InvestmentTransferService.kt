package com.financas.app.data.repository

import com.financas.app.data.db.InvestmentAssetDao
import com.financas.app.data.db.TransactionDao
import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.domain.usecase.InvestmentTransferRules
import java.time.LocalDate

class InvestmentTransferService(
    private val transactionDao: TransactionDao,
    private val assetDao: InvestmentAssetDao,
    private val todayEpochDay: () -> Long = { LocalDate.now().toEpochDay() }
) {

    suspend fun ensureHoldingAsset(): InvestmentAssetEntity {
        val existing = assetDao.getById(InvestmentTransferRules.XTB_HOLDING_ASSET_ID)
        if (existing != null) return existing
        val created = InvestmentTransferRules.newHoldingAsset(todayEpochDay())
        assetDao.insert(created)
        return created
    }

    suspend fun recordTransfer(transaction: TransactionEntity, assetId: String): Boolean {
        val asset = if (assetId == InvestmentTransferRules.XTB_HOLDING_ASSET_ID) {
            ensureHoldingAsset()
        } else {
            assetDao.getById(assetId)
        } ?: return false

        transactionDao.insert(
            transaction.copy(
                type = InvestmentTransferRules.TRANSFER_TYPE,
                investmentAssetId = asset.id
            )
        )
        assetDao.update(InvestmentTransferRules.contribute(asset, transaction.amountCents, todayEpochDay()))
        return true
    }

    suspend fun addContribution(
        assetId: String,
        amountCents: Long,
        epochDay: Long = todayEpochDay(),
        label: String? = null
    ): Boolean {
        if (amountCents <= 0L) return false
        val asset = if (assetId == InvestmentTransferRules.XTB_HOLDING_ASSET_ID) {
            ensureHoldingAsset()
        } else {
            assetDao.getById(assetId)
        } ?: return false
        val date = LocalDate.ofEpochDay(epochDay)
        val transaction = TransactionEntity(
            type = InvestmentTransferRules.TRANSFER_TYPE,
            amountCents = amountCents,
            yearMonth = date.year * 100 + date.monthValue,
            epochDay = epochDay,
            categoryId = InvestmentTransferRules.INVESTMENT_CATEGORY_ID,
            contextTag = "",
            merchant = label ?: "Aporte – ${asset.name}",
            paymentMethod = "TRANSFERENCIA"
        )
        return recordTransfer(transaction, asset.id)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteById(transaction.id)
        val assetId = transaction.investmentAssetId ?: return
        val asset = assetDao.getById(assetId) ?: return
        assetDao.update(InvestmentTransferRules.revert(asset, transaction.amountCents, todayEpochDay()))
    }

    suspend fun adjustContribution(assetId: String, deltaCents: Long) {
        if (deltaCents == 0L) return
        val asset = assetDao.getById(assetId) ?: return
        val updated = if (deltaCents > 0L) {
            InvestmentTransferRules.contribute(asset, deltaCents, todayEpochDay())
        } else {
            InvestmentTransferRules.revert(asset, -deltaCents, todayEpochDay())
        }
        assetDao.update(updated)
    }

    suspend fun allocate(fromAssetId: String, toAssetId: String, amountCents: Long): Boolean {
        val from = assetDao.getById(fromAssetId) ?: return false
        val to = assetDao.getById(toAssetId) ?: return false
        val (newFrom, newTo) = InvestmentTransferRules.allocate(from, to, amountCents, todayEpochDay()) ?: return false
        assetDao.update(newFrom)
        assetDao.update(newTo)
        return true
    }
}
