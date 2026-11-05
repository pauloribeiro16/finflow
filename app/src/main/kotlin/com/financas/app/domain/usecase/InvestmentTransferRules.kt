package com.financas.app.domain.usecase

import com.financas.app.data.entity.InvestmentAssetEntity

enum class InvestmentPlatform { XTB, OPTIMIZE }

object InvestmentTransferRules {

    const val TRANSFER_TYPE = "TRANSFER"
    const val XTB_HOLDING_ASSET_ID = "xtb-holding"
    const val XTB_HOLDING_NAME = "XTB – por alocar"
    const val XTB_HOLDING_CHOICE_LABEL = "Decidir depois (XTB)"
    const val INVESTMENT_CATEGORY_ID = "exp_financial_savings"

    private val xtbRegex = Regex("""\bxtb\b""", RegexOption.IGNORE_CASE)
    private val optimizeRegex = Regex("""\boptimize\b""", RegexOption.IGNORE_CASE)

    fun detectPlatform(merchant: String): InvestmentPlatform? = when {
        xtbRegex.containsMatchIn(merchant) -> InvestmentPlatform.XTB
        optimizeRegex.containsMatchIn(merchant) -> InvestmentPlatform.OPTIMIZE
        else -> null
    }

    fun isInvestmentCategory(categoryId: String?, categoryName: String?): Boolean =
        categoryId == INVESTMENT_CATEGORY_ID || categoryName?.contains("investiment", ignoreCase = true) == true

    fun isInvestmentTransfer(merchant: String, categoryId: String?, categoryName: String?): Boolean =
        detectPlatform(merchant) != null || isInvestmentCategory(categoryId, categoryName)

    fun matchMonthlyAsset(
        amountCents: Long,
        platform: InvestmentPlatform?,
        assets: List<InvestmentAssetEntity>
    ): String? {
        if (amountCents <= 0L) return null
        val candidates = assets.filter { !it.archived && it.monthlyContributionCents > 0L && it.monthlyContributionCents == amountCents }
        if (candidates.size <= 1 || platform == null) return candidates.singleOrNull()?.id
        val platformName = platform.name
        val narrowed = candidates.filter {
            it.institution.contains(platformName, ignoreCase = true) ||
                it.name.contains(platformName, ignoreCase = true) ||
                (platform == InvestmentPlatform.XTB && isHoldingAsset(it))
        }
        return narrowed.singleOrNull()?.id
    }

    fun isHoldingAsset(asset: InvestmentAssetEntity): Boolean = asset.id == XTB_HOLDING_ASSET_ID

    fun newHoldingAsset(epochDay: Long): InvestmentAssetEntity = InvestmentAssetEntity(
        id = XTB_HOLDING_ASSET_ID,
        name = XTB_HOLDING_NAME,
        type = "OUTRO",
        institution = "XTB",
        investedCents = 0L,
        currentValueCents = 0L,
        notes = "Dinheiro enviado para a XTB que ainda não foi alocado a ativos.",
        lastUpdatedEpochDay = epochDay
    )

    fun contribute(asset: InvestmentAssetEntity, amountCents: Long, epochDay: Long): InvestmentAssetEntity =
        asset.copy(
            investedCents = asset.investedCents + amountCents,
            currentValueCents = asset.currentValueCents + amountCents,
            lastUpdatedEpochDay = epochDay
        )

    fun revert(asset: InvestmentAssetEntity, amountCents: Long, epochDay: Long): InvestmentAssetEntity =
        asset.copy(
            investedCents = (asset.investedCents - amountCents).coerceAtLeast(0L),
            currentValueCents = (asset.currentValueCents - amountCents).coerceAtLeast(0L),
            lastUpdatedEpochDay = epochDay
        )

    fun allocate(
        from: InvestmentAssetEntity,
        to: InvestmentAssetEntity,
        amountCents: Long,
        epochDay: Long
    ): Pair<InvestmentAssetEntity, InvestmentAssetEntity>? {
        if (amountCents <= 0L || from.id == to.id) return null
        if (amountCents > from.currentValueCents || amountCents > from.investedCents) return null
        return revert(from, amountCents, epochDay) to contribute(to, amountCents, epochDay)
    }
}
