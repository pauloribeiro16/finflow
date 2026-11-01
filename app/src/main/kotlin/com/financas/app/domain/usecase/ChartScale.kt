package com.financas.app.domain.usecase

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

object ChartScale {

    private val NICE_MULTIPLIERS = listOf(1.0, 2.0, 5.0, 10.0)

    fun computeTicks(maxValue: Double, targetTickCount: Int = 3): List<Double> {
        if (maxValue <= 0.0 || targetTickCount <= 0) return emptyList()

        val maxTickCount = targetTickCount + 1
        val base = 10.0.pow(floor(log10(maxValue / targetTickCount)))
        val step = NICE_MULTIPLIERS
            .map { it * base }
            .first { candidate -> ceil(maxValue / candidate).toInt() <= maxTickCount }

        val tickCount = ceil(maxValue / step).toInt().coerceAtLeast(1)
        val magnitude = floor(log10(step)).toInt()
        return (1..tickCount).map { index -> roundTick(index * step, magnitude) }
    }

    fun formatCompactCents(cents: Long): String {
        if (cents == 0L) return "0 €"
        val sign = if (cents < 0L) "-" else ""
        val absCents = abs(cents)
        return when {
            absCents < 100_000L -> "$sign${(absCents / 100.0).roundToLong()} €"
            absCents < 100_000_000L -> "$sign${formatOneDecimal(absCents / 100_000.0)}k €"
            else -> "$sign${formatOneDecimal(absCents / 100_000_000.0)}M €"
        }
    }

    fun formatCompactPercent(value: Double): String {
        val rounded = (value * 10.0).roundToLong() / 10.0
        return if (rounded == rounded.roundToLong().toDouble()) {
            "${rounded.roundToLong()}%"
        } else {
            "${rounded.toString().replace('.', ',')}%"
        }
    }

    private fun roundTick(value: Double, magnitude: Int): Double {
        if (magnitude >= 0) return value
        val factor = 10.0.pow(-magnitude.toDouble())
        return (value * factor).roundToLong() / factor
    }

    private fun formatOneDecimal(value: Double): String {
        val rounded = (value * 10.0).roundToLong() / 10.0
        return rounded.toString().replace('.', ',')
    }
}
