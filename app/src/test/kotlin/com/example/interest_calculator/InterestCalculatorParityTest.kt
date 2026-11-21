package com.example.interest_calculator

import com.example.interest_calculator.domain.InterestCalculator
import com.example.interest_calculator.domain.InterestInput
import com.example.interest_calculator.domain.InterestMode
import com.example.interest_calculator.domain.InterestOutcome
import com.example.interest_calculator.domain.legacyText
import com.example.interest_calculator.ui.CalculatorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Proves the rebuilt calculator returns exactly what v1 returned, by running both the new Kotlin
 * implementation and v1's original Java code ([LegacyInterestLogic]) over millions of inputs and
 * comparing the full output strings (label + `String.valueOf(double)`), not approximations.
 */
class InterestCalculatorParityTest {

    private val modes = listOf(null, InterestMode.Tirageta, InterestMode.Plain)

    private fun legacyId(mode: InterestMode?) = when (mode) {
        null -> LegacyInterestLogic.NONE
        InterestMode.Tirageta -> LegacyInterestLogic.YES
        InterestMode.Plain -> LegacyInterestLogic.NO
    }

    private fun assertParity(
        d1: String, m1: String, y1: String,
        d2: String, m2: String, y2: String,
        amount: Int, rateHundredths: Int, mode: InterestMode?,
    ) {
        // Exactly how the app feeds the calculator (see CalculatorState).
        val rate = CalculatorState.rateFromHundredths(rateHundredths)
        val actual = InterestCalculator.calculate(
            InterestInput(d1, m1, y1, d2, m2, y2, amount, rate, mode),
        ).legacyText
        val expected = LegacyInterestLogic.calculateInterest(
            d1, m1, y1, d2, m2, y2, amount.toFloat(), rate, legacyId(mode),
        )
        assertEquals("dates=$d1/$m1/$y1→$d2/$m2/$y2 amount=$amount rate=$rate mode=$mode", expected, actual)
    }

    @Test
    fun exhaustiveCalendarDatesAcrossFourYears() {
        // Every day-of-month 1..31 x month 1..12 x year 2022..2025, for both dates, sampled amount/rate.
        val rnd = Random(7)
        var checked = 0
        for (y1 in 2022..2025) for (m1 in 1..12) for (d1 in 1..31) {
            for (y2 in y1..2025) for (m2 in 1..12) for (d2 in 1..31) {
                val amount = (rnd.nextInt(1, 1001)) * 1000
                val rate = rnd.nextInt(10, 501)
                val mode = modes[1 + (checked and 1)]
                assertParity("$d1", "$m1", "$y1", "$d2", "$m2", "$y2", amount, rate, mode)
                checked++
            }
        }
        assertTrue(checked > 1_000_000)
    }

    @Test
    fun everyRateAndBoundaryAmountsOnLongPeriods() {
        val periods = listOf(
            arrayOf("1", "1", "2000", "1", "1", "2000"),     // zero length
            arrayOf("31", "12", "1990", "1", "1", "2026"),   // borrows day and month
            arrayOf("15", "6", "2010", "14", "12", "2025"),  // diff_m >= 6 branch
            arrayOf("15", "6", "2023", "20", "9", "2024"),   // 1 year, diff_m < 6 -> "Plain"
            arrayOf("10", "3", "2020", "9", "8", "2023"),    // diff_m < 6 with diff_y >= 2
            arrayOf("1", "1", "1950", "28", "2", "2026"),    // very long compounding
        )
        val amounts = listOf(1_000, 2_000, 99_000, 100_000, 123_000, 999_000, 1_000_000)
        for (p in periods) for (rate in 10..500) for (amount in amounts) for (mode in modes) {
            assertParity(p[0], p[1], p[2], p[3], p[4], p[5], amount, rate, mode)
        }
    }

    @Test
    fun randomizedIncludingOddInputsV1Accepted() {
        // v1 accepted any digits (no calendar validation): 00, 31 in February, month 13, leading
        // zeros, 1-digit years, and "0000". All must behave the same.
        val rnd = Random(2026)
        fun digits(maxLen: Int): String {
            val len = rnd.nextInt(0, maxLen + 1)
            return buildString { repeat(len) { append('0' + rnd.nextInt(10)) } }
        }
        repeat(2_000_000) {
            val emptyChance = rnd.nextInt(20) == 0
            val d1 = if (emptyChance) digits(2) else digits(2).ifEmpty { "1" }
            assertParity(
                d1, digits(2).ifEmpty { "1" }, digits(4).ifEmpty { "2020" },
                digits(2).ifEmpty { "1" }, digits(2).ifEmpty { "1" }, digits(4).ifEmpty { "2021" },
                rnd.nextInt(1, 1001) * 1000, rnd.nextInt(10, 501), modes[rnd.nextInt(3)],
            )
        }
    }

    @Test
    fun typedAmountsUseTheSameFormula() {
        // v2 accepts any typed amount. v1 read the amount as a float, which is exact up to 2^24,
        // so for every amount in that range both implementations must agree to the paisa.
        val rnd = Random(99)
        repeat(1_000_000) {
            val y1 = rnd.nextInt(1990, 2026)
            assertParity(
                "${rnd.nextInt(1, 32)}", "${rnd.nextInt(1, 13)}", "$y1",
                "${rnd.nextInt(1, 32)}", "${rnd.nextInt(1, 13)}", "${rnd.nextInt(y1, 2027)}",
                rnd.nextInt(1, 16_777_217), rnd.nextInt(10, 501), modes[rnd.nextInt(3)],
            )
        }
    }

    @Test
    fun validationOrderMatchesV1() {
        // Missing dates wins over everything, then the radio check, then date order.
        assertParity("", "", "", "", "", "", 100_000, 125, null)
        assertParity("1", "1", "2020", "1", "1", "", 100_000, 125, InterestMode.Plain)
        assertParity("2", "1", "2020", "1", "1", "2020", 100_000, 125, null)
        assertParity("2", "1", "2020", "1", "1", "2020", 100_000, 125, InterestMode.Tirageta)
        assertEquals(
            InterestOutcome.ModeNotSelected,
            InterestCalculator.calculate(InterestInput("2", "1", "2020", "1", "1", "2020", 1000, 1f, null)),
        )
    }

    @Test
    fun knownValues() {
        // 1,00,000 at 1.25%/month for exactly one year, Plain: 1,00,000 * (1 + 0.15) = 1,15,000.
        val plain = InterestCalculator.calculate(
            InterestInput("1", "1", "2024", "1", "1", "2025", 100_000, 1.25f, InterestMode.Plain),
        )
        assertEquals("Plain  115000.0", plain.legacyText)
        // Three years Tirageta, diff_m = 0 (< 6): 1,00,000 * 1.15^2 * 1.15 = 1,52,087.50.
        val tirageta = InterestCalculator.calculate(
            InterestInput("1", "1", "2022", "1", "1", "2025", 100_000, 1.25f, InterestMode.Tirageta),
        )
        assertEquals("Tirageta  152087.5", tirageta.legacyText)
    }
}
