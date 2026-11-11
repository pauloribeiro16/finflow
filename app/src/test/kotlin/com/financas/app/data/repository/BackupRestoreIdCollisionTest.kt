package com.financas.app.data.repository

import com.financas.app.data.entity.TransactionEntity
import com.financas.app.ui.fakes.FakeCategoryDao
import com.financas.app.ui.fakes.FakeTransactionDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreIdCollisionTest {

    private val serializer = BackupSerializer()

    private fun tx(
        id: Long,
        merchant: String,
        amountCents: Long,
        epochDay: Long,
        type: String = "EXPENSE",
        linked: Long? = null
    ) = TransactionEntity(
        id = id,
        type = type,
        amountCents = amountCents,
        yearMonth = 202611,
        epochDay = epochDay,
        categoryId = "sub_groceries",
        contextTag = "COMERCIO",
        merchant = merchant,
        paymentMethod = "MB WAY",
        linkedTransactionId = linked
    )

    @Test
    fun backupFromAnotherPhone_keepsLocalRowsAndRemapsRefundLinks() = runTest {
        val local = tx(id = 1, merchant = "Continente", amountCents = 4_200L, epochDay = 20_700L)
        val transactionDao = FakeTransactionDao(listOf(local))
        val incomingPurchase = tx(id = 1, merchant = "Worten", amountCents = 59_900L, epochDay = 20_760L)
        val incomingRefund = tx(id = 2, merchant = "Worten reembolso", amountCents = 10_000L, epochDay = 20_765L, type = "INCOME", linked = 1)

        val result = serializer.restoreBackup(
            BackupData(version = 3, categories = emptyList(), transactions = listOf(incomingRefund, incomingPurchase)),
            FakeCategoryDao(),
            transactionDao
        )

        val all = transactionDao.getAllSync()
        assertEquals(2, result.transactionsImported)
        assertEquals(0, result.transactionsSkipped)
        assertEquals(3, all.size)
        assertTrue(all.contains(local))
        val purchase = all.single { it.merchant == "Worten" }
        val refund = all.single { it.merchant == "Worten reembolso" }
        assertNotEquals(1L, purchase.id)
        assertEquals(purchase.id, refund.linkedTransactionId)
    }

    @Test
    fun sameRecordEditedLocally_isStillSkipped() = runTest {
        val local = tx(id = 1, merchant = "Continente", amountCents = 4_500L, epochDay = 20_700L)
        val transactionDao = FakeTransactionDao(listOf(local))
        val older = local.copy(amountCents = 4_200L)

        val result = serializer.restoreBackup(
            BackupData(version = 3, categories = emptyList(), transactions = listOf(older)),
            FakeCategoryDao(),
            transactionDao
        )

        assertEquals(0, result.transactionsImported)
        assertEquals(1, result.transactionsSkipped)
        assertEquals(listOf(local), transactionDao.getAllSync())
    }
}
