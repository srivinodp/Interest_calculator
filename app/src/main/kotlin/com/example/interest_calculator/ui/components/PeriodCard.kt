package com.example.interest_calculator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.interest_calculator.R
import com.example.interest_calculator.ui.DateFields
import com.example.interest_calculator.ui.theme.tabularNumbers
import kotlinx.coroutines.flow.drop
import java.util.Calendar
import java.util.TimeZone

/**
 * Start / end date entry. Same free-form DD / MM / YYYY digits as v1 (so every input v1
 * accepted is still accepted), plus: auto-advance between boxes, a calendar picker, a
 * "Today" shortcut, and an animated error state with a shake for an inverted range.
 */
@Composable
fun PeriodCard(
    start: DateFields,
    end: DateFields,
    isError: Boolean,
    shake: Shake,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    val requesters = remember { List(6) { FocusRequester() } }
    var pickerTarget by rememberSaveable { mutableStateOf<Int?>(null) } // 0 = start, 1 = end

    LaunchedEffect(isError) {
        if (isError) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            shake.play()
        }
    }

    SectionCard(modifier) {
        Text(
            text = stringResource(R.string.period),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        DateHeader(stringResource(R.string.start_date))
        DateRow(
            fields = start,
            requesters = requesters.subList(0, 3),
            onRowComplete = { requesters[3].requestFocus() },
            isError = false,
            onPick = { pickerTarget = 0 },
            pickDescription = stringResource(R.string.pick_start_date),
            tagPrefix = "start",
            lastRow = false,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
        DateHeader(stringResource(R.string.end_date)) {
            TextButton(
                onClick = {
                    val now = Calendar.getInstance()
                    end.set(now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR))
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 36.dp).testTag("end_today"),
            ) { Text(stringResource(R.string.today)) }
        }
        DateRow(
            fields = end,
            requesters = requesters.subList(3, 6),
            onRowComplete = { focusManager.clearFocus() },
            isError = isError,
            onPick = { pickerTarget = 1 },
            pickDescription = stringResource(R.string.pick_end_date),
            tagPrefix = "end",
            lastRow = true,
            modifier = Modifier.graphicsLayer { translationX = shake.offset.value.dp.toPx() },
        )
        AnimatedVisibility(
            visible = isError,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Text(
                text = stringResource(R.string.error_end_before_start),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp),
            )
        }
    }

    pickerTarget?.let { target ->
        val fields = if (target == 0) start else end
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fields.toUtcMillisOrNull())
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(fields::setFromUtcMillis)
                        pickerTarget = null
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateHeader(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
private fun DateRow(
    fields: DateFields,
    requesters: List<FocusRequester>,
    onRowComplete: () -> Unit,
    isError: Boolean,
    onPick: () -> Unit,
    pickDescription: String,
    tagPrefix: String,
    lastRow: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        DigitField(
            state = fields.day, maxLength = 2, label = stringResource(R.string.dd),
            focusRequester = requesters[0], onFilled = { requesters[1].requestFocus() },
            imeAction = ImeAction.Next, isError = isError,
            modifier = Modifier.weight(1f).testTag("${tagPrefix}_day"),
        )
        DigitField(
            state = fields.month, maxLength = 2, label = stringResource(R.string.mm),
            focusRequester = requesters[1], onFilled = { requesters[2].requestFocus() },
            imeAction = ImeAction.Next, isError = isError,
            modifier = Modifier.weight(1f).testTag("${tagPrefix}_month"),
        )
        DigitField(
            state = fields.year, maxLength = 4, label = stringResource(R.string.yyyy),
            focusRequester = requesters[2], onFilled = onRowComplete,
            imeAction = if (lastRow) ImeAction.Done else ImeAction.Next, isError = isError,
            modifier = Modifier.weight(1.45f).testTag("${tagPrefix}_year"),
        )
        FilledTonalIconButton(
            onClick = onPick,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier
                .padding(bottom = 4.dp)
                .size(48.dp)
                .testTag("${tagPrefix}_pick"),
        ) {
            Icon(painterResource(R.drawable.ic_calendar), contentDescription = pickDescription)
        }
    }
}

@Composable
private fun DigitField(
    state: TextFieldState,
    maxLength: Int,
    label: String,
    focusRequester: FocusRequester,
    onFilled: () -> Unit,
    imeAction: ImeAction,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val focused = remember { booleanArrayOf(false) }
    val currentOnFilled by rememberUpdatedState(onFilled)
    val transformation = remember(maxLength) { DigitsOnly(maxLength) }

    // Typing the last digit of a box jumps to the next one, like a PIN entry.
    LaunchedEffect(state) {
        snapshotFlow { state.text.length }
            .drop(1)
            .collect { length -> if (length == maxLength && focused[0]) currentOnFilled() }
    }

    val errorTint by animateColorAsState(
        targetValue = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "digitColor",
    )

    OutlinedTextField(
        state = state,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focused[0] = it.isFocused },
        label = { Text(label, maxLines = 1, softWrap = false) },
        inputTransformation = transformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        onKeyboardAction = { currentOnFilled() },
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.titleMedium.tabularNumbers().copy(
            textAlign = TextAlign.Center,
            color = errorTint,
        ),
        isError = isError,
        shape = MaterialTheme.shapes.large,
        // Narrow boxes on 360dp phones: trim the default 16dp side padding so "MM" never clips.
        contentPadding = OutlinedTextFieldDefaults.contentPaddingWithLabel(start = 6.dp, end = 6.dp),
    )
}

/** v1 used `inputType="number"` + `maxLength`: ASCII digits only, capped length. */
private class DigitsOnly(private val maxLength: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val current = asCharSequence()
        if (current.length <= maxLength && current.all { it in '0'..'9' }) return
        val filtered = current.filter { it in '0'..'9' }.take(maxLength)
        replace(0, length, filtered)
    }
}

private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

/** The typed date as UTC midnight millis for the picker, or null if it is not a real date. */
private fun DateFields.toUtcMillisOrNull(): Long? {
    val d = day.text.toString().toIntOrNull() ?: return null
    val m = month.text.toString().toIntOrNull() ?: return null
    val y = year.text.toString().toIntOrNull() ?: return null
    if (y !in 1900..2100) return null
    return runCatching {
        Calendar.getInstance(Utc).apply {
            clear()
            isLenient = false
            set(y, m - 1, d)
        }.timeInMillis
    }.getOrNull()
}

private fun DateFields.setFromUtcMillis(millis: Long) {
    val calendar = Calendar.getInstance(Utc).apply { timeInMillis = millis }
    set(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
}
