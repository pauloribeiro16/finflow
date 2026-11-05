package com.financas.app.data

import com.financas.app.data.entity.InvestmentAssetEntity
import com.financas.app.data.entity.TransactionEntity
import com.financas.app.data.repository.InvestmentTransferService
import com.financas.app.domain.usecase.InvestmentTransferRules
import com.financas.app.ui.fakes.FakeInvestmentAssetDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentTransferServiceTest {

    private val optimize = InvestmentAssetEntity(
        id = "opt", name = "Optimize Agressivo", type = "OUTRO", institution = "Optimize",
        investedCents = 10_000L, currentValueCents = 11_000L, lastUpdatedEpochDay = 1L
    )

    private fun tx(amount: Long = 5_000L) = TransactionEntity(
        id = 7L, type = "EXPENSE", amountCents = amount, yearMonth = 202611, epochDay = 100L,
        categoryId = "c", contextTag = "", merchant = "XTB", paymentMethod = "TRANSFERENCIA"
    )

    @Test
    fun recordTransfer_toXtb_createsHoldingAssetAndStoresTransferNotExpense() = runTest {
        val txDao = FakeTransactionDao()
        val assetDao = FakeInvestmentAssetDao()
        val service = InvestmentTransferService(txDao, assetDao) { 200L }

        assertTrue(service.recordTransfer(tx(), InvestmentTransferRules.XTB_HOLDING_ASSET_ID))

        val holding = assetDao.getById(InvestmentTransferRules.XTB_HOLDING_ASSET_ID)!!
        assertEquals(5_000L, holding.investedCents)
        assertEquals(5_000L, holding.currentValueCents)
        val saved = txDao.getAllSync().single()
        assertEquals("TRANSFER", saved.type)
        assertEquals(holding.id, saved.investmentAssetId)
    }

    @Test
    fun recordTransfer_unknownAsset_returnsFalseAndSavesNothing() = runTest {
        val txDao = FakeTransactionDao()
        val service = InvestmentTransferService(txDao, FakeInvestmentAssetDao()) { 200L }
        assertFalse(service.recordTransfer(tx(), "missing"))
        assertTrue(txDao.getAllSync().isEmpty())
    }

    @Test
    fun deleteTransaction_revertsAssetContribution() = runTest {
        val txDao = FakeTransactionDao()
        val assetDao = FakeInvestmentAssetDao(listOf(optimize))
        val service = InvestmentTransferService(txDao, assetDao) { 200L }
        service.recordTransfer(tx(), "opt")
        val saved = txDao.getAllSync().single()

        service.deleteTransaction(saved)

        val asset = assetDao.getById("opt")!!
        assertEquals(10_000L, asset.investedCents)
        assertEquals(11_000L, asset.currentValueCents)
        assertTrue(txDao.getAllSync().isEmpty())
    }

    @Test
    fun addContribution_recordsTransferAndGrowsAsset_andRejectsNonPositive() = runTest {
        val txDao = FakeTransactionDao()
        val assetDao = FakeInvestmentAssetDao(listOf(optimize))
        val service = InvestmentTransferService(txDao, assetDao) { 200L }

        assertFalse(service.addContribution("opt", 0L))
        assertTrue(service.addContribution("opt", 2_500L))

        val asset = assetDao.getById("opt")!!
        assertEquals(12_500L, asset.investedCents)
        assertEquals(13_500L, asset.currentValueCents)
        val saved = txDao.getAllSync().single()
        assertEquals("TRANSFER", saved.type)
        assertEquals("opt", saved.investmentAssetId)
        assertEquals(InvestmentTransferRules.INVESTMENT_CATEGORY_ID, saved.categoryId)
    }

    @Test
    fun lastAssetByPlatform_returnsMostRecentAssetPerPlatform() = runTest {
        val txDao = FakeTransactionDao()
        val assetDao = FakeInvestmentAssetDao(listOf(optimize))
        val service = InvestmentTransferService(txDao, assetDao) { 200L }
        service.recordTransfer(tx(1_000L).copy(id = 0L, merchant = "Transferência Optimize", epochDay = 10L), "opt")
        service.recordTransfer(tx(1_000L).copy(id = 0L, merchant = "Transferência Optimize", epochDay = 20L), InvestmentTransferRules.XTB_HOLDING_ASSET_ID)

        val last = service.lastAssetByPlatform()

        assertEquals(InvestmentTransferRules.XTB_HOLDING_ASSET_ID, last[com.financas.app.domain.usecase.InvestmentPlatform.OPTIMIZE])
    }

    @Test
    fun allocate_movesFromHoldingToRealAsset() = runTest {
        val assetDao = FakeInvestmentAssetDao(listOf(optimize))
        val service = InvestmentTransferService(FakeTransactionDao(), assetDao) { 200L }
        service.recordTransfer(tx(8_000L), InvestmentTransferRules.XTB_HOLDING_ASSET_ID)

        assertTrue(service.allocate(InvestmentTransferRules.XTB_HOLDING_ASSET_ID, "opt", 3_000L))
        assertEquals(5_000L, assetDao.getById(InvestmentTransferRules.XTB_HOLDING_ASSET_ID)!!.currentValueCents)
        assertEquals(13_000L, assetDao.getById("opt")!!.investedCents)
        assertFalse(service.allocate(InvestmentTransferRules.XTB_HOLDING_ASSET_ID, "opt", 99_000L))
        assertNotNull(assetDao.getById("opt"))
    }
}
