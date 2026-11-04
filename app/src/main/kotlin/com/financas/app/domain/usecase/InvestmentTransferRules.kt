package com.financas.app.domain.usecase

import com.financas.app.data.entity.InvestmentAssetEntity

enum class InvestmentPlatform { XTB, OPTIMIZE }

object InvestmentTransferRules {

    const val TRANSFER_TYPE = "TRANSFER"
    const val XTB_HOLDING_ASSET_ID = "xtb-holding"
    const val XTB_HOLDING_NAME = "XTB – por alocar"

    private val xtbRegex = Regex("""\bxtb\b""", RegexOption.IGNORE_CASE)
    private val optimizeRegex = Regex("""\boptimize\b""", RegexOption.IGNORE_CASE)

    fun detectPlatform(merchant: String): InvestmentPlatform? = when {
        xtbRegex.containsMatchIn(merchant) -> InvestmentPlatform.XTB
        optimizeRegex.containsMatchIn(merchant) -> InvestmentPlatform.OPTIMIZE
        else -> null
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

    fun suggestAssetId(platform: InvestmentPlatform, assets: List<InvestmentAssetEntity>): String? = when (platform) {
        InvestmentPlatform.XTB -> XTB_HOLDING_ASSET_ID
        InvestmentPlatform.OPTIMIZE -> assets.firstOrNull {
            !it.archived &&
                it.name.contains("optimize", ignoreCase = true) &&
                it.name.contains("agress", ignoreCase = true)
        }?.id
    }

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
