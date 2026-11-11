package com.financas.app.data.repository

import kotlin.math.ln

enum class PasswordStrength { WEAK, FAIR, STRONG }

object BackupPasswordPolicy {

    const val MIN_LENGTH = 10
    private const val FAIR_BITS = 50.0
    private const val STRONG_BITS = 75.0
    private val COMMON_FRAGMENTS = listOf(
        "password", "palavra", "passe", "senha", "qwerty", "azerty", "asdfgh", "finflow", "financas",
        "benfica", "sporting", "porto", "portugal", "iloveyou", "admin", "welcome", "letmein", "dragon"
    )

    fun strength(password: String): PasswordStrength {
        if (password.length < MIN_LENGTH) return PasswordStrength.WEAK
        val bits = estimateBits(password)
        return when {
            bits < FAIR_BITS -> PasswordStrength.WEAK
            bits < STRONG_BITS -> PasswordStrength.FAIR
            else -> PasswordStrength.STRONG
        }
    }

    fun isAcceptable(password: String): Boolean = strength(password) != PasswordStrength.WEAK

    internal fun estimateBits(password: String): Double {
        var pool = 0
        if (password.any { it in 'a'..'z' }) pool += 26
        if (password.any { it in 'A'..'Z' }) pool += 26
        if (password.any { it.isDigit() }) pool += 10
        if (password.any { !it.isLetterOrDigit() }) pool += 32
        if (password.any { it.isLetter() && it !in 'a'..'z' && it !in 'A'..'Z' }) pool += 32
        if (pool == 0) return 0.0

        val lower = password.lowercase()
        var effectiveLength = minOf(password.length, password.toSet().size * 2).toDouble()
        effectiveLength -= sequentialRunChars(lower) * 0.75
        COMMON_FRAGMENTS.filter { lower.contains(it) }.forEach { effectiveLength -= it.length * 0.75 }
        return effectiveLength.coerceAtLeast(0.0) * (ln(pool.toDouble()) / ln(2.0))
    }

    private fun sequentialRunChars(text: String): Int {
        var total = 0
        var run = 1
        for (i in 1 until text.length) {
            val step = text[i] - text[i - 1]
            if (step == 1 || step == -1 || step == 0) {
                run++
            } else {
                if (run >= 3) total += run
                run = 1
            }
        }
        if (run >= 3) total += run
        return total
    }
}
