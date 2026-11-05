package com.financas.app.ui.screens.addsheet

import com.financas.app.data.entity.CategoryEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentDestinationTest {

    private val categories = listOf(
        CategoryEntity("exp_food", null, "Alimentação", "restaurant", 0xFFFF9800, "EXPENSE", false, 1),
        CategoryEntity("exp_financial_savings", "exp_financial", "Poupança / Investimento", "trending_up", 0xFF607D8B, "EXPENSE", true, 2)
    )

    @Test
    fun investmentCategoryOnNewExpense_requiresExplicitDestination() {
        val state = AddTransactionUiState(allCategories = categories, selectedCategoryId = "exp_financial_savings", type = "EXPENSE")
        assertTrue(state.requiresInvestmentDestination())
        assertTrue(state.investmentAssetId == null)
    }

    @Test
    fun effectiveAsset_isNullUntilChosen_unlessAMonthlyContributionMatchesTheAmount() {
        val asset = com.financas.app.data.entity.InvestmentAssetEntity(
            id = "x", name = "XTB", type = "OUTRO", institution = "XTB", investedCents = 0L, currentValueCents = 0L,
            monthlyContributionCents = 20_000L, lastUpdatedEpochDay = 1L
        )
        val base = AddTransactionUiState(allCategories = categories, selectedCategoryId = "exp_financial_savings", investmentAssets = listOf(asset))
        assertTrue(base.effectiveInvestmentAssetId() == null)
        assertTrue(base.copy(amountCents = 20_000L).effectiveInvestmentAssetId() == "x")
        assertTrue(base.copy(amountCents = 20_000L, investmentAssetId = "other").effectiveInvestmentAssetId() == "other")
        assertTrue(base.copy(amountCents = 5_000L).effectiveInvestmentAssetId() == null)
    }

    @Test
    fun otherCategoriesAndEditsAndIncomeDoNotRequireDestination() {
        assertFalse(AddTransactionUiState(allCategories = categories, selectedCategoryId = "exp_food").requiresInvestmentDestination())
        assertFalse(
            AddTransactionUiState(allCategories = categories, selectedCategoryId = "exp_financial_savings", editingTransactionId = 5L)
                .requiresInvestmentDestination()
        )
        assertFalse(
            AddTransactionUiState(allCategories = categories, selectedCategoryId = "exp_financial_savings", type = "INCOME")
                .requiresInvestmentDestination()
        )
    }
}
