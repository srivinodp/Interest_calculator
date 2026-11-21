package com.example.interest_calculator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.interest_calculator.R
import com.example.interest_calculator.domain.InterestMode

/**
 * v1's Tirageta / Plain radio group as an Expressive connected button group: the selected
 * button morphs its corners, a check mark grows in, and the description below cross-slides.
 * Starts with nothing selected, exactly like v1.
 */
@Composable
fun ModeSelector(
    selected: InterestMode?,
    onSelect: (InterestMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val motion = MaterialTheme.motionScheme

    SectionCard(modifier) {
        Text(
            text = stringResource(R.string.interest_type),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            InterestMode.entries.forEachIndexed { index, mode ->
                val checked = selected == mode
                ToggleButton(
                    checked = checked,
                    onCheckedChange = {
                        if (!checked) haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        onSelect(mode)
                    },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .semantics { role = Role.RadioButton }
                        .testTag("mode_${mode.name}"),
                ) {
                    AnimatedVisibility(
                        visible = checked,
                        enter = expandHorizontally(motion.fastSpatialSpec(), expandFrom = Alignment.Start) +
                            scaleIn(motion.fastSpatialSpec(), initialScale = 0.4f) +
                            fadeIn(motion.fastEffectsSpec()),
                        exit = shrinkHorizontally(motion.fastSpatialSpec(), shrinkTowards = Alignment.Start) +
                            scaleOut(motion.fastSpatialSpec(), targetScale = 0.4f) +
                            fadeOut(motion.fastEffectsSpec()),
                    ) {
                        Row {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                            )
                            Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                        }
                    }
                    Text(
                        text = stringResource(mode.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn(motion.defaultEffectsSpec()) + slideInVertically(motion.defaultSpatialSpec()) { it / 2 })
                    .togetherWith(fadeOut(motion.fastEffectsSpec()) + slideOutVertically(motion.fastSpatialSpec()) { -it / 2 })
            },
            contentAlignment = Alignment.CenterStart,
            label = "modeDescription",
        ) { mode ->
            Text(
                text = stringResource(
                    when (mode) {
                        InterestMode.Tirageta -> R.string.tirageta_description
                        InterestMode.Plain -> R.string.plain_description
                        null -> R.string.mode_description_none
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 4.dp),
            )
        }
    }
}

val InterestMode.titleRes: Int
    get() = when (this) {
        InterestMode.Tirageta -> R.string.tirageta
        InterestMode.Plain -> R.string.plain
    }
