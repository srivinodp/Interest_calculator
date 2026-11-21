package com.example.interest_calculator.domain

/** The two choices of v1's radio group (`R.id.Yes` = Tirageta, `R.id.No` = Plain). */
enum class InterestMode { Tirageta, Plain }

/** The formula that actually produced the total. v1 printed this as the result prefix. */
enum class AppliedFormula(val legacyLabel: String) {
    Plain("Plain"),
    Tirageta("Tirageta"),
}

/**
 * Raw inputs, in exactly the shape v1 read them from its views.
 *
 * @property amount principal. v1: `Math.round(sliderAmount.getValue())`.
 * @property rate interest in % per month. v1: `sliderInterest.getValue()`, a `float` that was then
 *   widened to `double`. It is kept as a [Float] on purpose so the arithmetic matches v1 bit for bit.
 * @property mode `null` when neither option is selected.
 */
data class InterestInput(
    val startDay: String,
    val startMonth: String,
    val startYear: String,
    val endDay: String,
    val endMonth: String,
    val endYear: String,
    val amount: Int,
    val rate: Float,
    val mode: InterestMode?,
)

sealed interface InterestOutcome {
    /** v1: "Missing dates!" */
    data object MissingDates : InterestOutcome

    /** v1: "Tirageta / No ???" */
    data object ModeNotSelected : InterestOutcome

    /** v1: "Incorrect dates???" */
    data object IncorrectDates : InterestOutcome

    /**
     * A computed total.
     *
     * @property total the unrounded total.
     * @property totalCents `Math.round(total * 100.0)`, i.e. v1's displayed value times 100.
     * @property years @property months @property days the period as v1 computed it
     *   (30-day months, after borrowing).
     */
    data class Calculated(
        val formula: AppliedFormula,
        val principal: Int,
        val total: Double,
        val totalCents: Long,
        val years: Int,
        val months: Int,
        val days: Int,
    ) : InterestOutcome {
        /** v1's displayed rounded total: `Math.round(total * 100.0) / 100.0`. */
        val roundedTotal: Double get() = totalCents / 100.0
        val interestCents: Long get() = totalCents - principal * 100L
    }
}

/** The exact output string v1 would have shown in its result `TextView`. */
val InterestOutcome.legacyText: String
    get() = when (this) {
        InterestOutcome.MissingDates -> "Missing dates!"
        InterestOutcome.ModeNotSelected -> "Tirageta / No ???"
        InterestOutcome.IncorrectDates -> "Incorrect dates???"
        is InterestOutcome.Calculated -> formula.legacyLabel + "  " + roundedTotal.toString()
    }

/**
 * Line-for-line port of v1's `MainActivity.calculateInterest()`.
 *
 * Do not "clean up" the arithmetic: operand order, the int/double mixing, the float rate and
 * `Math.round` are all kept identical so results match the original app exactly. This is
 * enforced by `InterestCalculatorParityTest`, which runs this against the original Java code.
 */
object InterestCalculator {

    fun calculate(input: InterestInput): InterestOutcome = calculateFrom(input)

    private fun calculateFrom(input: InterestInput): InterestOutcome {
        val startDay = input.startDay
        val startMonth = input.startMonth
        val startYear = input.startYear
        val endDay = input.endDay
        val endMonth = input.endMonth
        val endYear = input.endYear
        val amount = input.amount
        val rate = input.rate
        val mode = input.mode


        if (startDay.isEmpty() || startMonth.isEmpty() || startYear.isEmpty() ||
            endDay.isEmpty() || endMonth.isEmpty() || endYear.isEmpty()
        ) {
            return InterestOutcome.MissingDates
        }

        // starting and ending dates
        val dd1 = startDay.toInt()
        val dd2 = endDay.toInt()
        val mm1 = startMonth.toInt()
        var mm2 = endMonth.toInt()
        val yy1 = startYear.toInt()
        var yy2 = endYear.toInt()

        if (mode == null) return InterestOutcome.ModeNotSelected

        if (yy1 > yy2 || yy1 == yy2 && mm1 > mm2 || yy1 == yy2 && mm1 == mm2 && dd1 > dd2) {
            return InterestOutcome.IncorrectDates
        }

        val aamount: Int = amount
        val inttVal: Double = rate.toDouble()

        val diffD: Int
        val diffM: Int
        if (dd1 <= dd2) {
            diffD = dd2 - dd1
        } else {
            mm2 -= 1
            diffD = 30 + dd2 - dd1
        }
        if (mm2 >= mm1) {
            diffM = mm2 - mm1
        } else {
            yy2 -= 1
            diffM = 12 + mm2 - mm1
        }
        val diffY = yy2 - yy1

        fun result(formula: AppliedFormula, total: Double) = InterestOutcome.Calculated(
            formula = formula,
            principal = aamount,
            total = total,
            totalCents = Math.round(total * 100.0),
            years = diffY,
            months = diffM,
            days = diffD,
        )

        return when (mode) {
            InterestMode.Tirageta -> {
                if (diffY < 1) {
                    val total = 1.0 * aamount * (1 + inttVal * 0.01 * (12 * diffY + diffM + diffD * 1.0 / 30))
                    return result(AppliedFormula.Plain, total)
                }
                if (diffM < 6) {
                    val total = 1.0 * aamount * Math.pow((1 + (0.12 * inttVal)), (diffY - 1).toDouble()) *
                        (1 + inttVal * 0.01 * (12 + diffM + diffD * 1.0 / 30))
                    return result(if (diffY < 2) AppliedFormula.Plain else AppliedFormula.Tirageta, total)
                }
                val total = 1.0 * aamount * Math.pow((1 + (0.12 * inttVal)), diffY.toDouble()) *
                    (1 + inttVal * 0.01 * (diffM + diffD * 1.0 / 30))
                result(AppliedFormula.Tirageta, total)
            }

            InterestMode.Plain -> {
                val total = 1.0 * aamount * (1 + inttVal * 0.01 * (12 * diffY + diffM + diffD * 1.0 / 30))
                result(AppliedFormula.Plain, total)
            }
        }
    }
}
