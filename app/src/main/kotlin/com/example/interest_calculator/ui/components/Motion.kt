package com.example.interest_calculator.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Odometer-style number: each character sits in its own slot and digits roll vertically when
 * they change, up when the value grows and down when it shrinks.
 *
 * Slots are keyed by their position from the right, so units stay units when the number gains a
 * digit, and the row width glides via [animateContentSize]. While [LocalNumberMotion] is
 * instant (the user is dragging a slider or holding a stepper) characters swap instantly, so the
 * number tracks the finger with no lag.
 *
 * @param value numeric value of [text]; only used to pick the roll direction.
 */
@Composable
fun RollingText(
    text: String,
    value: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val numberMotion = LocalNumberMotion.current
    val motion = MaterialTheme.motionScheme
    val slide = motion.fastSpatialSpec<IntOffset>()
    val fade = motion.fastEffectsSpec<Float>()
    val size = motion.fastSpatialSpec<IntSize>()

    // Plain (non-snapshot) holder: direction is decided once per change, during composition.
    val previous = remember { longArrayOf(value) }
    val rollUp = value >= previous[0]
    previous[0] = value

    val resolvedColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }

    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = text }
            .animateContentSize(size),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        // Consulted only when a digit actually changes, never observed: starting or
                        // ending a drag recomposes nothing.
                        if (numberMotion.instant || !initialState.isDigit() || !targetState.isDigit()) {
                            (EnterTransition.None togetherWith ExitTransition.None)
                                .using(SizeTransform(clip = false) { _, _ -> snap() })
                        } else {
                            val dir = if (rollUp) 1 else -1
                            (slideInVertically(slide) { h -> dir * h } + fadeIn(fade))
                                .togetherWith(slideOutVertically(slide) { h -> -dir * h } + fadeOut(fade))
                                .using(SizeTransform(clip = false))
                        }
                    },
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clipToBounds(),
                    label = "digit",
                ) { c ->
                    Text(text = c.toString(), style = style, color = resolvedColor, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/**
 * Scales its content down (never up) so it always fits the available width, e.g. a crore-sized
 * total on a narrow phone. Done in the layout phase with a layer: no re-measuring text sizes.
 */
fun Modifier.scaleToFitWidth(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val maxWidth = constraints.maxWidth
    if (!constraints.hasBoundedWidth || placeable.width <= maxWidth) {
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        val scale = maxWidth.toFloat() / placeable.width
        layout(maxWidth, (placeable.height * scale).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

/**
 * Staggered entrance played once per screen instance (not again on rotation): content rises
 * and fades in on the expressive slow spatial spring. Runs in the draw layer only, so it costs
 * no recomposition or relayout per frame.
 */
@Composable
fun EnterStagger(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val played = rememberSaveable { booleanArrayOf(false) }
    val progress = remember { Animatable(if (played[0]) 1f else 0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            awaitFrameTime(millis = 40L + index * 55L)
            progress.animateTo(1f, spec)
        }
        played[0] = true
    }
    Box(
        modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * 40.dp.toPx()
            val s = 0.96f + 0.04f * p
            scaleX = s
            scaleY = s
        },
    ) { content() }
}

/**
 * Waits on the frame clock rather than with `delay()`: the stagger stays locked to vsync with
 * the animation that follows it, and the UI never looks idle while an entrance is pending.
 */
/**
 * Tells [RollingText] to swap digits instantly while any value is being scrubbed. A plain object
 * rather than snapshot state on purpose: it is read at the moment a digit changes, so toggling it
 * at the start and end of every drag costs zero recompositions.
 */
class NumberMotion {
    private val scrubbing = mutableSetOf<Any>()

    val instant: Boolean get() = scrubbing.isNotEmpty()

    fun setScrubbing(source: Any, active: Boolean) {
        if (active) scrubbing += source else scrubbing -= source
    }
}

val LocalNumberMotion = staticCompositionLocalOf { NumberMotion() }

private suspend fun awaitFrameTime(millis: Long) {
    val start = withFrameNanos { it }
    val target = start + millis * 1_000_000L
    while (withFrameNanos { it } < target) Unit
}

/** Damped horizontal shake used to flag an invalid date range. Returns an offset in dp. */
class Shake {
    val offset = Animatable(0f)

    suspend fun play() {
        offset.snapTo(0f)
        offset.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.22f, stiffness = 1400f),
            initialVelocity = -600f,
        )
    }
}
