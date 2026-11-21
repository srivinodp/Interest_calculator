package com.example.interest_calculator.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.interest_calculator.R
import com.example.interest_calculator.domain.AppliedFormula
import com.example.interest_calculator.domain.InterestOutcome
import com.example.interest_calculator.ui.formatPeriod
import com.example.interest_calculator.ui.formatRupees
import com.example.interest_calculator.ui.formatRupeesFromCents
import com.example.interest_calculator.ui.theme.tabularNumbers

private enum class ResultKind { MissingAmount, MissingDates, ModeNotSelected, IncorrectDates, Result }

private val InterestOutcome?.kind: ResultKind
    get() = when (this) {
        null -> ResultKind.MissingAmount
        InterestOutcome.MissingDates -> ResultKind.MissingDates
        InterestOutcome.ModeNotSelected -> ResultKind.ModeNotSelected
        InterestOutcome.IncorrectDates -> ResultKind.IncorrectDates
        is InterestOutcome.Calculated -> ResultKind.Result
    }

/**
 * The live result. [outcome] is null while the amount box is empty. [hero] = large two-pane
 * variant for tablets, foldables and landscape.
 *
 * Only a change of *kind* (hint → result → error) runs the container transform; value changes
 * inside a result just roll the digits, so typing and sliding never re-animate the whole card.
 */
@Composable
fun ResultPanel(
    outcome: InterestOutcome?,
    hero: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val kind = outcome.kind
    val formula = (outcome as? InterestOutcome.Calculated)?.formula

    val (containerTarget, contentTarget) = when {
        kind == ResultKind.IncorrectDates -> scheme.errorContainer to scheme.onErrorContainer
        formula == AppliedFormula.Tirageta -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        formula == AppliedFormula.Plain -> scheme.primaryContainer to scheme.onPrimaryContainer
        else -> scheme.surfaceContainerHighest to scheme.onSurface
    }
    val container = animateColorAsState(containerTarget, motion.slowEffectsSpec(), label = "resultContainer")
    val content by animateColorAsState(contentTarget, motion.slowEffectsSpec(), label = "resultContent")

    // Keeps showing the last value while the result content animates out.
    val lastResult = remember { arrayOfNulls<InterestOutcome.Calculated>(1) }
    if (outcome is InterestOutcome.Calculated) lastResult[0] = outcome

    val shape = RoundedCornerShape(if (hero) 40.dp else 32.dp)
    Box(
        modifier = modifier
            .then(if (hero) Modifier else Modifier.shadow(10.dp, shape, clip = false))
            .clip(shape)
            // Color is read in the draw phase: animating it never recomposes the panel.
            .drawBehind { drawRect(container.value) }
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("result_panel"),
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            AnimatedContent(
                targetState = kind,
                transitionSpec = {
                    (
                        fadeIn(motion.defaultEffectsSpec()) +
                            scaleIn(motion.defaultSpatialSpec(), initialScale = 0.92f) +
                            slideInVertically(motion.defaultSpatialSpec()) { it / 5 }
                        ).togetherWith(
                        fadeOut(motion.fastEffectsSpec()) +
                            scaleOut(motion.fastSpatialSpec(), targetScale = 0.96f) +
                            slideOutVertically(motion.fastSpatialSpec()) { -it / 8 },
                    ).using(SizeTransform(clip = false) { _, _ -> motion.defaultSpatialSpec() })
                },
                contentAlignment = Alignment.TopStart,
                label = "resultKind",
            ) { target ->
                val padding = if (hero) Modifier.padding(28.dp) else Modifier.padding(horizontal = 22.dp, vertical = 18.dp)
                when (target) {
                    ResultKind.Result -> {
                        val result = (outcome as? InterestOutcome.Calculated) ?: lastResult[0]
                        if (result != null) {
                            ResultContent(result, hero, padding)
                        }
                    }
                    ResultKind.MissingAmount -> MessageContent(
                        R.drawable.ic_rupee, R.string.hint_missing_amount, R.string.hint_missing_amount_sub, hero, padding,
                    )
                    ResultKind.MissingDates -> MessageContent(
                        R.drawable.ic_event, R.string.hint_missing_dates, R.string.hint_missing_dates_sub, hero, padding,
                    )
                    ResultKind.ModeNotSelected -> MessageContent(
                        R.drawable.ic_tune, R.string.hint_choose_mode, R.string.hint_choose_mode_sub, hero, padding,
                    )
                    ResultKind.IncorrectDates -> MessageContent(
                        R.drawable.ic_error, R.string.hint_incorrect_dates, R.string.hint_incorrect_dates_sub, hero, padding,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    result: InterestOutcome.Calculated,
    hero: Boolean,
    modifier: Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        // Pill and period share a line, and the period wraps below at large font sizes.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FormulaPill(result.formula)
            Text(
                text = formatPeriod(result.years, result.months, result.days),
                style = MaterialTheme.typography.labelLarge.tabularNumbers(),
                color = LocalContentColor.current.copy(alpha = 0.8f),
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .testTag("result_period"),
            )
        }
        Spacer(Modifier.height(if (hero) 20.dp else 8.dp))
        Text(
            text = stringResource(R.string.total_amount),
            style = MaterialTheme.typography.labelLarge,
            color = LocalContentColor.current.copy(alpha = 0.75f),
        )
        RollingText(
            text = formatRupeesFromCents(result.totalCents),
            value = result.totalCents,
            style = (if (hero) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium)
                .tabularNumbers(),
            modifier = Modifier
                .scaleToFitWidth()
                .testTag("result_total"),
        )
        if (hero) {
            Spacer(Modifier.height(24.dp))
            BreakdownRow(stringResource(R.string.principal), formatRupees(result.principal))
            BreakdownDivider()
            BreakdownRow(stringResource(R.string.interest), formatRupeesFromCents(result.interestCents))
            BreakdownDivider()
            BreakdownRow(stringResource(R.string.period), formatPeriod(result.years, result.months, result.days))
        } else {
            Text(
                text = stringResource(
                    R.string.interest_on_principal,
                    formatRupeesFromCents(result.interestCents),
                    formatRupees(result.principal),
                ),
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                color = LocalContentColor.current.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .scaleToFitWidth()
                    .testTag("result_interest"),
            )
        }
    }
}

@Composable
private fun FormulaPill(formula: AppliedFormula) {
    val motion = MaterialTheme.motionScheme
    val color = LocalContentColor.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = formula,
            transitionSpec = {
                (fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f))
                    .togetherWith(fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.6f))
                    .using(SizeTransform(clip = false) { _, _ -> motion.fastSpatialSpec() })
            },
            contentAlignment = Alignment.CenterStart,
            label = "formula",
        ) { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (f == AppliedFormula.Tirageta) R.drawable.ic_trending_up else R.drawable.ic_trending_flat),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(if (f == AppliedFormula.Tirageta) R.string.tirageta else R.string.plain),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("result_formula"),
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = LocalContentColor.current.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.titleMedium.tabularNumbers())
    }
}

@Composable
private fun BreakdownDivider() {
    HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.14f))
}

@Composable
private fun MessageContent(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    hero: Boolean,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (hero) 56.dp else 44.dp)
                .clip(CircleShape)
                .background(LocalContentColor.current.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(if (hero) 28.dp else 24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(title),
                style = if (hero) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("result_message"),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.75f),
            )
        }
    }
}
