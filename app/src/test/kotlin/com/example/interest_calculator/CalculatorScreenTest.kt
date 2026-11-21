package com.example.interest_calculator

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.interest_calculator.domain.InterestCalculator
import com.example.interest_calculator.domain.InterestInput
import com.example.interest_calculator.domain.InterestMode
import com.example.interest_calculator.domain.InterestOutcome
import com.example.interest_calculator.ui.CalculatorApp
import com.example.interest_calculator.ui.CalculatorState
import com.example.interest_calculator.ui.formatRupeesFromCents
import com.example.interest_calculator.ui.theme.InterestTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val Phone = "w360dp-h780dp-xxxhdpi" // Galaxy S24 Ultra at its default display size
private const val Tablet = "w1280dp-h800dp-land-xhdpi"
private const val Out = "build/screenshots"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = Phone)
class CalculatorScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setApp(state: CalculatorState = CalculatorState(), dark: Boolean = false) {
        compose.setContent {
            InterestTheme(darkTheme = dark, dynamicColor = false) { CalculatorApp(state) }
        }
        compose.waitForIdle()
    }

    private fun typeDates(d1: String, m1: String, y1: String, d2: String, m2: String, y2: String) {
        compose.onNodeWithTag("start_day").performTextInput(d1)
        compose.onNodeWithTag("start_month").performTextInput(m1)
        compose.onNodeWithTag("start_year").performTextInput(y1)
        compose.onNodeWithTag("end_day").performTextInput(d2)
        compose.onNodeWithTag("end_month").performTextInput(m2)
        compose.onNodeWithTag("end_year").performTextInput(y2)
        compose.waitForIdle()
    }

    private fun expectedTotal(d1: String, m1: String, y1: String, d2: String, m2: String, y2: String, mode: InterestMode): String {
        val outcome = InterestCalculator.calculate(InterestInput(d1, m1, y1, d2, m2, y2, 100_000, 1.25f, mode))
        return formatRupeesFromCents((outcome as InterestOutcome.Calculated).totalCents)
    }

    @Test
    fun emptyState() {
        setApp()
        compose.onNodeWithTag("result_message").assertTextEquals("Enter start and end dates")
        compose.onNodeWithTag("reset").assertIsNotEnabled()
        compose.onRoot().captureRoboImage("$Out/01_phone_empty.png")
    }

    @Test
    fun typingDatesAndPickingModeShowsV1Result() {
        setApp()
        typeDates("15", "06", "2023", "21", "11", "2026")
        compose.onNodeWithTag("result_message").assertTextEquals("Choose Tirageta or Plain")
        compose.onRoot().captureRoboImage("$Out/02_phone_choose_mode.png")

        compose.onNodeWithTag("mode_Tirageta").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("result_total")
            .assertContentDescriptionEquals(expectedTotal("15", "06", "2023", "21", "11", "2026", InterestMode.Tirageta))
        compose.onNodeWithTag("result_formula").assertTextEquals("Tirageta")
        compose.onRoot().captureRoboImage("$Out/03_phone_tirageta.png")

        compose.onNodeWithTag("mode_Plain").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("result_total")
            .assertContentDescriptionEquals(expectedTotal("15", "06", "2023", "21", "11", "2026", InterestMode.Plain))
        compose.onNodeWithTag("result_formula").assertTextEquals("Plain")
    }

    @Test
    fun boxesAutoAdvanceLikeAPinEntry() {
        setApp()
        compose.onNodeWithTag("start_day").performTextInput("07")
        compose.waitForIdle()
        compose.onNodeWithTag("start_month").assertIsFocused()
        compose.onNodeWithTag("start_month").performTextInput("1a2") // non-digits are dropped
        compose.waitForIdle()
        compose.onNodeWithTag("start_year").assertIsFocused()
        compose.onNodeWithTag("start_month").assertTextEquals("MM", "12")
        compose.onNodeWithTag("start_year").performTextInput("2024")
        compose.waitForIdle()
        compose.onNodeWithTag("end_day").assertIsFocused()
    }

    @Test
    fun invertedRangeShowsError() {
        setApp(CalculatorState(mode = InterestMode.Plain))
        typeDates("10", "05", "2025", "01", "01", "2024")
        compose.onNodeWithTag("result_message").assertTextEquals("Check the dates")
        compose.onRoot().captureRoboImage("$Out/04_phone_error.png")
    }

    @Test
    fun steppersAndResetAnimateBackToDefaults() {
        val state = CalculatorState()
        setApp(state)
        repeat(3) { compose.onNodeWithTag("rate_inc").performClick() }
        compose.onNodeWithTag("amount_dec").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("rate_value").assertContentDescriptionEquals("1.28")
        assertEquals(99_000, state.amount)
        compose.onNodeWithTag("reset").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("rate_value").assertContentDescriptionEquals("1.25")
        assertEquals(100_000, state.amount)
        compose.onNodeWithTag("reset").assertIsNotEnabled()
    }

    @Test
    fun typingAnExactAmount() {
        val state = CalculatorState(
            mode = InterestMode.Plain,
            startDate = listOf("01", "04", "2024"),
            endDate = listOf("15", "09", "2025"),
        )
        setApp(state)
        compose.onNodeWithTag("amount_field").performTextReplacement("235500")
        compose.waitForIdle()
        assertEquals(235_500, state.amount)
        val outcome = InterestCalculator.calculate(
            InterestInput("01", "04", "2024", "15", "09", "2025", 235_500, 1.25f, InterestMode.Plain),
        ) as InterestOutcome.Calculated
        compose.onNodeWithTag("result_total").assertContentDescriptionEquals(formatRupeesFromCents(outcome.totalCents))

        // Leading zeros and non-digits are dropped; more than 9 digits is refused.
        compose.onNodeWithTag("amount_field").performTextReplacement("00a12")
        compose.waitForIdle()
        assertEquals(12, state.amount)

        compose.onNodeWithTag("amount_field").performTextReplacement("")
        compose.waitForIdle()
        compose.onNodeWithTag("result_message").assertTextEquals("Enter the principal amount")
    }

    @Test
    @Config(qualifiers = "+night")
    fun darkTheme() {
        setApp(
            CalculatorState(
                mode = InterestMode.Tirageta,
                startDate = listOf("12", "03", "2021"),
                endDate = listOf("21", "11", "2026"),
            ),
            dark = true,
        )
        compose.onNodeWithTag("result_formula").assertTextEquals("Tirageta")
        compose.onRoot().captureRoboImage("$Out/06_phone_dark.png")
    }

    @Test
    @Config(fontScale = 1.5f)
    fun largestFontStillFits() {
        setApp(
            CalculatorState(
                amountText = "1000000",
                rateHundredths = 500,
                mode = InterestMode.Tirageta,
                startDate = listOf("01", "01", "2000"),
                endDate = listOf("21", "11", "2026"),
            ),
        )
        compose.onRoot().captureRoboImage("$Out/07_phone_font150_huge_total.png")
    }

    @Test
    @Config(qualifiers = Tablet)
    fun tabletTwoPane() {
        setApp(
            CalculatorState(
                mode = InterestMode.Tirageta,
                startDate = listOf("12", "03", "2021"),
                endDate = listOf("21", "11", "2026"),
            ),
        )
        compose.onRoot().captureRoboImage("$Out/08_tablet_two_pane.png")
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-night-xhdpi")
    fun tabletTwoPaneDarkEmpty() {
        setApp(dark = true)
        compose.onRoot().captureRoboImage("$Out/09_tablet_dark_empty.png")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = Phone)
class MainActivitySmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesWithSplashAndEdgeToEdge() {
        compose.waitForIdle()
        compose.onNodeWithTag("result_panel").assertExists()
        assertTrue(compose.activity.window.decorView.isAttachedToWindow)
    }
}
