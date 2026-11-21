package com.example.interest_calculator.ui

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.interest_calculator.domain.InterestCalculator
import com.example.interest_calculator.domain.InterestInput
import com.example.interest_calculator.domain.InterestMode
import com.example.interest_calculator.domain.InterestOutcome

/** One date as three free-text fields, exactly like v1's DD / MM / YYYY boxes. */
@Stable
class DateFields(day: String = "", month: String = "", year: String = "") {
    val day = TextFieldState(day)
    val month = TextFieldState(month)
    val year = TextFieldState(year)

    fun set(day: Int, month: Int, year: Int) {
        this.day.setTextAndPlaceCursorAtEnd(day.toString().padStart(2, '0'))
        this.month.setTextAndPlaceCursorAtEnd(month.toString().padStart(2, '0'))
        this.year.setTextAndPlaceCursorAtEnd(year.toString().padStart(4, '0'))
    }

    fun clear() {
        day.clearText()
        month.clearText()
        year.clearText()
    }

    internal fun values() = listOf(day.text.toString(), month.text.toString(), year.text.toString())
}

/**
 * All screen state. Survives rotation, folding/unfolding and process death via [Saver].
 *
 * The outcome is a [derivedStateOf] so it recomputes only when an input it reads changes, and
 * only the composables that read [outcome] recompose.
 */
@Stable
class CalculatorState(
    amountText: String = DefaultAmount.toString(),
    rateHundredths: Int = DefaultRateHundredths,
    mode: InterestMode? = null,
    startDate: List<String> = listOf("", "", ""),
    endDate: List<String> = listOf("", "", ""),
) {
    val start = DateFields(startDate[0], startDate[1], startDate[2])
    val end = DateFields(endDate[0], endDate[1], endDate[2])

    /**
     * Principal as typed: plain digits, no commas (the lakh/crore grouping is display-only).
     * Any whole amount up to [AmountMax]; v1's slider allowed only 1,000..10,00,000.
     */
    val amountField = TextFieldState(amountText)

    /** The typed principal, or null while the box is empty. */
    val amount: Int? get() = amountField.text.toString().toIntOrNull()?.takeIf { it > 0 }

    /** Rate in hundredths of a percent per month, 10..500 = 0.10%..5.00% (v1 slider range). */
    var rateHundredths by mutableIntStateOf(rateHundredths)
        private set

    var mode by mutableStateOf(mode)

    val rate: Float get() = rateFromHundredths(rateHundredths)

    /** v1's result for the current inputs, or null while the amount box is empty. */
    val outcome: InterestOutcome? by derivedStateOf {
        val principal = amount ?: return@derivedStateOf null
        val (d1, m1, y1) = start.values()
        val (d2, m2, y2) = end.values()
        InterestCalculator.calculate(InterestInput(d1, m1, y1, d2, m2, y2, principal, rate, this.mode))
    }

    fun updateAmount(value: Int) {
        amountField.setTextAndPlaceCursorAtEnd(value.coerceIn(0, AmountMax).takeIf { it > 0 }?.toString().orEmpty())
    }

    fun updateRate(hundredths: Int) {
        rateHundredths = hundredths.coerceIn(RateMin, RateMax)
    }

    fun clearInputs() {
        start.clear()
        end.clear()
        mode = null
    }

    companion object {
        const val AmountMax = 999_999_999 // ₹99,99,99,999
        const val AmountMaxDigits = 9
        const val AmountStep = 1_000
        const val DefaultAmount = 100_000

        const val RateMin = 10
        const val RateMax = 500
        const val DefaultRateHundredths = 125

        /**
         * v1 read the rate as the slider's `float` value; 1.25 is `1.25f`, 1.33 is `1.33f`, etc.
         * Keeping the same float keeps every result identical to v1.
         */
        fun rateFromHundredths(hundredths: Int): Float = hundredths / 100f

        val Saver = listSaver<CalculatorState, String>(
            save = {
                listOf(it.amountField.text.toString(), it.rateHundredths.toString(), it.mode?.name.orEmpty()) +
                    it.start.values() + it.end.values()
            },
            restore = {
                CalculatorState(
                    amountText = it[0],
                    rateHundredths = it[1].toInt(),
                    mode = it[2].takeIf(String::isNotEmpty)?.let(InterestMode::valueOf),
                    startDate = it.subList(3, 6),
                    endDate = it.subList(6, 9),
                )
            },
        )
    }
}

@Composable
fun rememberCalculatorState(): CalculatorState =
    rememberSaveable(saver = CalculatorState.Saver) { CalculatorState() }
