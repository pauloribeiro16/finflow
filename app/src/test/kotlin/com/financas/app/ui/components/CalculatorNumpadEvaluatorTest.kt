package com.financas.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculatorNumpadEvaluatorTest {

    private lateinit var evaluator: CalculatorNumpadEvaluator

    @Before
    fun setUp() {
        evaluator = CalculatorNumpadEvaluator()
    }

    @Test
    fun initialValueIsZero() {
        assertEquals("0", evaluator.rawInput)
        assertEquals(0L, evaluator.evaluateToCents())
        assertEquals("0,00 €", evaluator.formattedResult())
    }

    @Test
    fun appendDigits_replacesInitialZero() {
        evaluator.appendDigit('1')
        evaluator.appendDigit('2')
        evaluator.appendDigit('5')
        assertEquals("125", evaluator.rawInput)
        assertEquals(12500L, evaluator.evaluateToCents())
    }

    @Test
    fun appendCommaAndDecimals() {
        evaluator.appendDigit('1')
        evaluator.appendDigit('2')
        evaluator.appendComma()
        evaluator.appendDigit('5')
        evaluator.appendDigit('0')

        assertEquals("12,50", evaluator.rawInput)
        assertEquals(1250L, evaluator.evaluateToCents())
        assertEquals("12,50 €", evaluator.formattedResult())
    }

    @Test
    fun preventsMultipleCommasInSameOperand() {
        evaluator.appendDigit('5')
        evaluator.appendComma()
        evaluator.appendDigit('5')
        evaluator.appendComma() // Should be ignored
        evaluator.appendDigit('5')

        assertEquals("5,55", evaluator.rawInput)
        assertEquals(555L, evaluator.evaluateToCents())
    }

    @Test
    fun inlineAddition_criterion1_contractMustRequirement() {
        // "12,50+3,50" -> 1600L
        evaluator.setRaw("12,50+3,50")
        assertEquals(1600L, evaluator.evaluateToCents())
        assertEquals("16,00 €", evaluator.formattedResult())
    }

    @Test
    fun multipleInlineAdditions() {
        evaluator.setRaw("10+5,25+2")
        // 1000 + 525 + 200 = 1725L
        assertEquals(1725L, evaluator.evaluateToCents())
        assertEquals("17,25 €", evaluator.formattedResult())
    }

    @Test
    fun appendDoubleZeroBehavior() {
        evaluator.appendDigit('2')
        evaluator.appendDoubleZero()
        assertEquals("200", evaluator.rawInput)
        assertEquals(20000L, evaluator.evaluateToCents())

        evaluator.appendComma()
        evaluator.appendDoubleZero()
        assertEquals("200,00", evaluator.rawInput)
        assertEquals(20000L, evaluator.evaluateToCents())
    }

    @Test
    fun backspaceBehavior() {
        evaluator.setRaw("123")
        evaluator.backspace()
        assertEquals("12", evaluator.rawInput)

        evaluator.backspace()
        assertEquals("1", evaluator.rawInput)

        evaluator.backspace()
        assertEquals("0", evaluator.rawInput)

        evaluator.backspace()
        assertEquals("0", evaluator.rawInput)
    }

    @Test
    fun clearResetsToZero() {
        evaluator.setRaw("999,99+50")
        evaluator.clear()
        assertEquals("0", evaluator.rawInput)
        assertEquals(0L, evaluator.evaluateToCents())
    }

    @Test
    fun danglingPlusSignHandledGracefully() {
        evaluator.setRaw("15+")
        assertEquals(1500L, evaluator.evaluateToCents())
    }

    @Test
    fun appendDoubleZero_integerOneDecimalAndTwoDecimalPositions() {
        evaluator.setRaw("0")
        evaluator.appendDoubleZero()
        assertEquals("0", evaluator.rawInput)

        evaluator.setRaw("5")
        evaluator.appendDoubleZero()
        assertEquals("500", evaluator.rawInput)
        assertEquals(50000L, evaluator.evaluateToCents())

        evaluator.setRaw("5,")
        evaluator.appendDoubleZero()
        assertEquals("5,00", evaluator.rawInput)
        assertEquals(500L, evaluator.evaluateToCents())

        evaluator.setRaw("5,5")
        evaluator.appendDoubleZero()
        assertEquals("5,50", evaluator.rawInput)
        assertEquals(550L, evaluator.evaluateToCents())

        evaluator.setRaw("5,55")
        evaluator.appendDoubleZero()
        assertEquals("5,55", evaluator.rawInput)
        assertEquals(555L, evaluator.evaluateToCents())
    }

    @Test
    fun commaAfterPlus_prependsZeroDecimal() {
        evaluator.setRaw("5+")
        evaluator.appendComma()
        assertEquals("5+0,", evaluator.rawInput)
        assertEquals(500L, evaluator.evaluateToCents())

        evaluator.appendDigit('7')
        assertEquals("5+0,7", evaluator.rawInput)
        assertEquals(570L, evaluator.evaluateToCents())
    }

    @Test
    fun clearMidExpression_andBackspaceFromSingleChar() {
        evaluator.setRaw("12,5+3")
        assertEquals(1550L, evaluator.evaluateToCents())
        evaluator.clear()
        assertEquals("0", evaluator.rawInput)
        assertEquals(0L, evaluator.evaluateToCents())

        evaluator.setRaw("0")
        evaluator.appendDigit('9')
        assertEquals("9", evaluator.rawInput)
        assertEquals(900L, evaluator.evaluateToCents())
        evaluator.backspace()
        assertEquals("0", evaluator.rawInput)
        evaluator.backspace()
        assertEquals("0", evaluator.rawInput)
    }

    @Test
    fun trailingCommaExcessDecimalsAndLargeSum() {
        evaluator.setRaw("12,")
        assertEquals(1200L, evaluator.evaluateToCents())

        evaluator.setRaw("1,234")
        assertEquals(123L, evaluator.evaluateToCents())

        evaluator.setRaw("0,999")
        assertEquals(99L, evaluator.evaluateToCents())

        evaluator.setRaw("999,99+999,99")
        assertEquals(199998L, evaluator.evaluateToCents())
        assertEquals("1.999,98 €", evaluator.formattedResult())
    }
}
