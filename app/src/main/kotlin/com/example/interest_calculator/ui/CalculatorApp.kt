package com.example.interest_calculator.ui

import androidx.activity.compose.ReportDrawn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.interest_calculator.R
import com.example.interest_calculator.domain.InterestMode
import com.example.interest_calculator.domain.InterestOutcome
import com.example.interest_calculator.ui.CalculatorState.Companion.AmountMax
import com.example.interest_calculator.ui.CalculatorState.Companion.AmountStep
import com.example.interest_calculator.ui.CalculatorState.Companion.DefaultAmount
import com.example.interest_calculator.ui.CalculatorState.Companion.DefaultRateHundredths
import com.example.interest_calculator.ui.CalculatorState.Companion.RateMax
import com.example.interest_calculator.ui.CalculatorState.Companion.RateMin
import com.example.interest_calculator.ui.components.AmountCard
import com.example.interest_calculator.ui.components.EnterStagger
import com.example.interest_calculator.ui.components.LocalNumberMotion
import com.example.interest_calculator.ui.components.NumberMotion
import com.example.interest_calculator.ui.components.ModeSelector
import com.example.interest_calculator.ui.components.PeriodCard
import com.example.interest_calculator.ui.components.ResultPanel
import com.example.interest_calculator.ui.components.Shake
import com.example.interest_calculator.ui.components.ValueSliderCard
import com.example.interest_calculator.ui.theme.InterestTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Width at which the layout switches to inputs + result side by side. */
private val TwoPaneMinWidth = 720.dp

@Composable
fun CalculatorApp(state: CalculatorState = rememberCalculatorState()) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val motion = MaterialTheme.motionScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val shake = remember { Shake() }

    // While a value is being scrubbed (slider drag, stepper hold, reset count-back) numbers update
    // instantly so they track the finger; discrete changes get the rolling-digit animation.
    val numberMotion = remember { NumberMotion() }

    val canReset by remember {
        derivedStateOf {
            state.amount != DefaultAmount || state.rateHundredths != DefaultRateHundredths ||
                state.mode != null || state.start.hasText() || state.end.hasText()
        }
    }
    val resetRotation = remember { Animatable(0f) }

    fun reset() {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        focusManager.clearFocus()
        state.clearInputs()
        scope.launch { resetRotation.animateTo(resetRotation.value + 360f, motion.slowSpatialSpec()) }
        scope.launch {
            numberMotion.setScrubbing("reset", true)
            val amountJob = launch {
                val from = (state.amount ?: 0).toFloat()
                animate(from, DefaultAmount.toFloat(), animationSpec = motion.slowEffectsSpec()) { v, _ ->
                    state.updateAmount((v / AmountStep).roundToInt() * AmountStep)
                }
            }
            val rateJob = launch {
                animate(state.rateHundredths.toFloat(), DefaultRateHundredths.toFloat(), animationSpec = motion.slowEffectsSpec()) { v, _ ->
                    state.updateRate(v.roundToInt())
                }
            }
            amountJob.join()
            rateJob.join()
            state.updateAmount(DefaultAmount)
            state.updateRate(DefaultRateHundredths)
            numberMotion.setScrubbing("reset", false)
        }
    }

    ReportDrawn()

    CompositionLocalProvider(LocalNumberMotion provides numberMotion) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                // Ask for the panel's top refresh rate (e.g. 120 Hz) whenever something here animates;
                // when idle nothing is invalidated, so the display is free to drop to save battery.
                .preferredFrameRate(FrameRateCategory.High),
        ) {
            val twoPane = maxWidth >= TwoPaneMinWidth
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    LargeFlexibleTopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            IconButton(
                                onClick = ::reset,
                                enabled = canReset,
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.testTag("reset"),
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_reset),
                                    contentDescription = stringResource(R.string.reset),
                                    modifier = Modifier.graphicsLayer { rotationZ = resetRotation.value },
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                },
                bottomBar = {
                    if (!twoPane) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                // Rides on top of the keyboard, frame-synced with the IME animation.
                                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            EnterStagger(index = 4) {
                                ResultPanel(
                                    outcome = state.outcome,
                                    hero = false,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                if (twoPane) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    ) {
                        Inputs(
                            state = state,
                            shake = shake,
                            numberMotion = numberMotion,
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 560.dp),
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .widthIn(max = 520.dp)
                                .padding(top = 8.dp, bottom = 24.dp),
                        ) {
                            EnterStagger(index = 1) {
                                ResultPanel(
                                    outcome = state.outcome,
                                    hero = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                } else {
                    Inputs(
                        state = state,
                        shake = shake,
                        numberMotion = numberMotion,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun Inputs(
    state: CalculatorState,
    shake: Shake,
    numberMotion: NumberMotion,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isError by remember { derivedStateOf { state.outcome == InterestOutcome.IncorrectDates } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        EnterStagger(index = 0) {
            val amount = state.amount ?: 0
            AmountCard(
                field = state.amountField,
                amount = state.amount,
                canDecrement = amount > AmountStep,
                canIncrement = amount <= AmountMax - AmountStep,
                onDecrement = { state.updateAmount((state.amount ?: 0) - AmountStep) },
                onIncrement = { state.updateAmount((state.amount ?: 0) + AmountStep) },
                onHoldChange = { numberMotion.setScrubbing("amount", it) },
            )
        }
        EnterStagger(index = 1) {
            ValueSliderCard(
                title = stringResource(R.string.rate_of_interest),
                valueText = formatRate(state.rateHundredths),
                numericValue = state.rateHundredths.toLong(),
                unit = stringResource(R.string.rate_unit),
                sliderValue = state.rateHundredths / 100f,
                valueRange = RateMin / 100f..RateMax / 100f,
                onSliderChange = { v ->
                    val before = state.rateHundredths
                    state.updateRate((v * 100f).roundToInt())
                    if (crossedTick(before, state.rateHundredths, tick = 10, min = RateMin, max = RateMax)) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    }
                },
                minLabel = formatRate(RateMin) + "%",
                maxLabel = formatRate(RateMax) + "%",
                canDecrement = state.rateHundredths > RateMin,
                canIncrement = state.rateHundredths < RateMax,
                onDecrement = { state.updateRate(state.rateHundredths - 1) },
                onIncrement = { state.updateRate(state.rateHundredths + 1) },
                onScrubbingChange = { numberMotion.setScrubbing("rate", it) },
                decrementDescription = stringResource(R.string.decrease_rate),
                incrementDescription = stringResource(R.string.increase_rate),
                testTagPrefix = "rate",
            )
        }
        EnterStagger(index = 2) {
            PeriodCard(start = state.start, end = state.end, isError = isError, shake = shake)
        }
        EnterStagger(index = 3) {
            ModeSelector(selected = state.mode, onSelect = { state.mode = it })
        }
    }
}

/** True when a slider move crosses a tick boundary or lands on an end stop. */
private fun crossedTick(before: Int, after: Int, tick: Int, min: Int, max: Int): Boolean =
    before != after && (before / tick != after / tick || after == min || after == max)

private fun DateFields.hasText() = day.text.isNotEmpty() || month.text.isNotEmpty() || year.text.isNotEmpty()

@Preview(name = "Phone", showSystemUi = true)
@Composable
private fun PhonePreview() {
    InterestTheme(dynamicColor = false) {
        CalculatorApp(
            CalculatorState(
                mode = InterestMode.Tirageta,
                startDate = listOf("12", "03", "2021"),
                endDate = listOf("21", "11", "2026"),
            ),
        )
    }
}

@Preview(name = "Tablet", device = Devices.TABLET, showSystemUi = true)
@Composable
private fun TabletPreview() {
    InterestTheme(dynamicColor = false, darkTheme = true) {
        CalculatorApp(
            CalculatorState(
                mode = InterestMode.Plain,
                startDate = listOf("01", "01", "2024"),
                endDate = listOf("15", "08", "2025"),
            ),
        )
    }
}
