package com.example.interest_calculator.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.interest_calculator.R
import com.example.interest_calculator.ui.theme.tabularNumbers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Rounded container shared by every input section. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), content = content)
    }
}

/**
 * A big rolling value, -/+ steppers (tap = one step, hold = accelerating repeat) and a slider.
 *
 * @param onScrubbingChange true while the user is dragging the slider or holding a stepper,
 *   so the screen can switch numbers to instant updates for zero-lag tracking.
 */
@Composable
fun ValueSliderCard(
    title: String,
    valueText: String,
    numericValue: Long,
    unit: String?,
    sliderValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onSliderChange: (Float) -> Unit,
    minLabel: String,
    maxLabel: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onScrubbingChange: (Boolean) -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    modifier: Modifier = Modifier,
    testTagPrefix: String = title,
) {
    val sliderInteraction = remember { MutableInteractionSource() }
    val dragged by sliderInteraction.collectIsDraggedAsState()
    val pressed by sliderInteraction.collectIsPressedAsState()
    val currentOnScrubbingChange by rememberUpdatedState(onScrubbingChange)
    LaunchedEffect(dragged || pressed) { currentOnScrubbingChange(dragged || pressed) }

    // The thumb follows the finger continuously while dragging (the value snaps to whole steps);
    // otherwise it follows the app state, e.g. after a stepper tap or a reset.
    val sliderState = remember(valueRange) { SliderState(sliderValue, 0, valueRange) }
    SideEffect {
        if (!sliderState.isDragging && sliderState.value != sliderValue) sliderState.value = sliderValue
    }

    SectionCard(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
            ) {
                RollingText(
                    text = valueText,
                    value = numericValue,
                    style = MaterialTheme.typography.displaySmall.tabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .scaleToFitWidth()
                        .testTag("${testTagPrefix}_value"),
                )
                if (unit != null) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, bottom = 7.dp),
                    )
                }
            }
            StepperButton(
                icon = R.drawable.ic_remove,
                contentDescription = decrementDescription,
                enabled = canDecrement,
                onStep = onDecrement,
                onHoldChange = onScrubbingChange,
                modifier = Modifier.testTag("${testTagPrefix}_dec"),
            )
            Spacer(Modifier.width(8.dp))
            StepperButton(
                icon = R.drawable.ic_add,
                contentDescription = incrementDescription,
                enabled = canIncrement,
                onStep = onIncrement,
                onHoldChange = onScrubbingChange,
                modifier = Modifier.testTag("${testTagPrefix}_inc"),
            )
        }
        Slider(
            state = sliderState,
            onValueChange = { v ->
                sliderState.value = v
                onSliderChange(v)
            },
            interactionSource = sliderInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .semantics { contentDescription = title }
                .testTag("${testTagPrefix}_slider"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val labelStyle = MaterialTheme.typography.labelMedium.tabularNumbers()
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(minLabel, style = labelStyle, color = labelColor)
            Text(maxLabel, style = labelStyle, color = labelColor)
        }
    }
}

/**
 * Tonal icon button whose corners morph on press (Material 3 Expressive shapes).
 * A tap steps once; holding repeats with acceleration, ticking the haptics on every step.
 */
@Composable
internal fun StepperButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    enabled: Boolean,
    onStep: () -> Unit,
    onHoldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val currentOnStep by rememberUpdatedState(onStep)
    val currentOnHoldChange by rememberUpdatedState(onHoldChange)
    val currentEnabled by rememberUpdatedState(enabled)
    // Set once a hold has produced repeats, so the click that ends the hold does not add one more.
    val holdFired = remember { booleanArrayOf(false) }

    LaunchedEffect(interactionSource) {
        var repeatJob: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    repeatJob?.cancel()
                    repeatJob = launch {
                        delay(380)
                        var interval = 110L
                        currentOnHoldChange(true)
                        while (isActive && currentEnabled) {
                            holdFired[0] = true
                            currentOnStep()
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            delay(interval)
                            interval = (interval * 0.86f).toLong().coerceAtLeast(26L)
                        }
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    repeatJob?.cancel()
                    repeatJob = null
                    holdFired[0] = false
                    currentOnHoldChange(false)
                }
            }
        }
    }

    FilledTonalIconButton(
        onClick = {
            if (holdFired[0]) {
                holdFired[0] = false
            } else {
                onStep()
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        },
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
        interactionSource = interactionSource,
        modifier = modifier.size(48.dp),
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription)
    }
}
