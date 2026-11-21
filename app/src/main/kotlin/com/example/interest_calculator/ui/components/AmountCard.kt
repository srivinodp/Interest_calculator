package com.example.interest_calculator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.interest_calculator.R
import com.example.interest_calculator.ui.CalculatorState.Companion.AmountMaxDigits
import com.example.interest_calculator.ui.groupIndian
import com.example.interest_calculator.ui.theme.tabularNumbers

/**
 * Principal entry: a full-width box you type the exact amount into (number keypad, live
 * lakh/crore commas), with -/+ ₹1,000 steppers in the header.
 *
 * While the box is not focused the number is drawn by [RollingText], so stepper taps and reset
 * roll the digits; on focus the real editable text takes over in exactly the same place.
 */
@Composable
fun AmountCard(
    field: TextFieldState,
    amount: Int?,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onHoldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.principal_amount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StepperButton(
                icon = R.drawable.ic_remove,
                contentDescription = stringResource(R.string.decrease_amount),
                enabled = canDecrement,
                onStep = onDecrement,
                onHoldChange = onHoldChange,
                modifier = Modifier.testTag("amount_dec"),
            )
            Spacer(Modifier.width(8.dp))
            StepperButton(
                icon = R.drawable.ic_add,
                contentDescription = stringResource(R.string.increase_amount),
                enabled = canIncrement,
                onStep = onIncrement,
                onHoldChange = onHoldChange,
                modifier = Modifier.testTag("amount_inc"),
            )
        }
        AmountField(
            field = field,
            amount = amount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}

@Composable
private fun AmountField(
    field: TextFieldState,
    amount: Int?,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    val borderWidth by animateDpAsState(if (focused) 2.dp else 1.dp, motion.fastSpatialSpec(), label = "amountBorder")
    val borderColor by animateColorAsState(
        if (focused) scheme.primary else scheme.outline,
        motion.fastEffectsSpec(),
        label = "amountBorderColor",
    )
    val style = MaterialTheme.typography.displaySmall.tabularNumbers().copy(color = scheme.onSurface)
    val shape = MaterialTheme.shapes.large
    val showRolling = !focused && amount != null

    BasicTextField(
        state = field,
        modifier = modifier.testTag("amount_field"),
        textStyle = style,
        inputTransformation = AmountInput,
        outputTransformation = IndianGrouping,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        onKeyboardAction = { focusManager.clearFocus() },
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(scheme.primary),
        decorator = { innerTextField ->
            Row(
                modifier = Modifier
                    .border(borderWidth, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("₹", style = style, color = scheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    // The real text stays laid out (cursor, selection, IME) and is only hidden
                    // while the rolling copy is drawn on top of it.
                    Box(Modifier.graphicsLayer { alpha = if (showRolling) 0f else 1f }) { innerTextField() }
                    if (showRolling) {
                        RollingText(
                            text = groupIndian(amount.toLong()),
                            value = amount.toLong(),
                            style = style,
                            modifier = Modifier.scaleToFitWidth(),
                        )
                    }
                    if (field.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.amount_placeholder),
                            style = style,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        },
    )
}

/** Digits only, no leading zeros, at most [AmountMaxDigits] digits (₹99,99,99,999). */
private object AmountInput : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val current = asCharSequence().toString()
        val digits = current.filter { it in '0'..'9' }.trimStart('0')
        if (digits.length > AmountMaxDigits) {
            revertAllChanges()
            return
        }
        if (digits != current) replace(0, length, digits)
    }
}

/** Display-only Indian grouping: "235000" shows as "2,35,000"; the stored text stays digits. */
private object IndianGrouping : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        var position = length - 3
        while (position > 0) {
            insert(position, ",")
            position -= 2
        }
    }
}
