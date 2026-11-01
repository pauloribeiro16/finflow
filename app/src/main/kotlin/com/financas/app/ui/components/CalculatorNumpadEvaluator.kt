package com.financas.app.ui.components

import com.financas.app.domain.model.MoneyCents

class CalculatorNumpadEvaluator(initialInput: String = "0") {

    var rawInput: String = initialInput
        private set

    fun appendDigit(digit: Char) {
        require(digit in '0'..'9') { "Only digits 0..9 are supported" }
        if (rawInput == "0") {
            rawInput = digit.toString()
        } else {
            rawInput += digit
        }
    }

    fun appendDoubleZero() {
        if (rawInput != "0") {
            val lastPart = rawInput.split('+').last()
            if (lastPart.isNotEmpty() && !lastPart.contains(',')) {
                rawInput += "00"
            } else if (lastPart.isNotEmpty() && lastPart.contains(',')) {
                val decimals = lastPart.substringAfter(',')
                if (decimals.isEmpty()) {
                    rawInput += "00"
                } else if (decimals.length == 1) {
                    rawInput += "0"
                }
            }
        }
    }

    fun appendComma() {
        val lastPart = rawInput.split('+').last()
        if (!lastPart.contains(',')) {
            if (lastPart.isEmpty()) {
                rawInput += "0,"
            } else {
                rawInput += ","
            }
        }
    }

    fun appendPlus() {
        if (rawInput.isNotEmpty() && rawInput.last() != '+' && rawInput.last() != ',') {
            rawInput += "+"
        }
    }

    fun backspace() {
        if (rawInput.length <= 1) {
            rawInput = "0"
        } else {
            rawInput = rawInput.dropLast(1)
        }
    }

    fun clear() {
        rawInput = "0"
    }

    fun setRaw(text: String) {
        val sanitized = text.replace('.', ',').filter { it in '0'..'9' || it == ',' || it == '+' }
        rawInput = if (sanitized.isEmpty()) "0" else sanitized
    }

    fun evaluateToCents(): Long {
        if (rawInput.isBlank()) return 0L

        val parts = rawInput.split('+')
        var totalCents = 0L

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            totalCents += parsePartToCents(trimmed)
        }

        return totalCents
    }

    fun formattedResult(): String {
        return MoneyCents.formatCents(evaluateToCents())
    }

    companion object {
        fun parsePartToCents(operand: String): Long {
            val clean = operand.replace(" ", "").replace('.', ',')
            if (clean.isEmpty()) return 0L

            val commaIndex = clean.indexOf(',')
            return if (commaIndex == -1) {
                val euros = clean.toLongOrNull() ?: 0L
                euros * 100L
            } else {
                val integerStr = clean.substring(0, commaIndex)
                val fractionalStr = clean.substring(commaIndex + 1)

                val euros = if (integerStr.isEmpty()) 0L else integerStr.toLongOrNull() ?: 0L
                val cents = when {
                    fractionalStr.isEmpty() -> 0L
                    fractionalStr.length == 1 -> (fractionalStr.toLongOrNull() ?: 0L) * 10L
                    fractionalStr.length == 2 -> fractionalStr.toLongOrNull() ?: 0L
                    else -> fractionalStr.take(2).toLongOrNull() ?: 0L
                }

                euros * 100L + cents
            }
        }
    }
}
