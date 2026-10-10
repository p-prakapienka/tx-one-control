package dev.tonexotg.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonexotg.app.ui.theme.RigAmber
import dev.tonexotg.app.ui.theme.RigKnobDark
import dev.tonexotg.app.ui.theme.RigKnobLight
import dev.tonexotg.app.ui.theme.RigTrack
import dev.tonexotg.app.ui.theme.TonexOnSurfaceSecondary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceTertiary
import dev.tonexotg.app.ui.theme.TonexTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The knob travels 270° — from 7:30 (135° in Compose's clockwise-from-3-o'clock convention) to 4:30. */
private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

/**
 * A hardware-style rotary knob for a `RANGE` parameter, used by the rig screens.
 *
 * Interaction:
 * - **Drag** up or right to increase, down or left to decrease. A full sweep of the range takes
 *   about [dragTravel] of finger travel, so small moves give fine control.
 * - **Double-tap** the knob (or tap its value readout) to open precise numeric entry via
 *   [onValueTextClick], if supplied.
 *
 * Same write contract as [ParameterControl]: [onValueChange] fires on every drag step,
 * [onValueChangeFinished] exactly once when the gesture ends (or is cancelled). Callers must
 * still end the gesture on disposal (issue #104) — see the rig screen's `DisposableEffect`.
 *
 * While a drag is in progress the knob draws its own local value rather than [value], so the
 * pointer never stutters back while the pedal echo is in flight.
 */
@Composable
fun RotaryKnob(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    knobSize: Dp = 68.dp,
    enabled: Boolean = true,
    bipolar: Boolean = valueRange.start < 0f && valueRange.endInclusive > 0f,
    dragTravel: Dp = 240.dp,
    onValueTextClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val travelPx = with(LocalDensity.current) { dragTravel.toPx() }
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f

    var dragging by remember { mutableStateOf(false) }
    var localValue by remember { mutableFloatStateOf(value) }
    val shownValue = if (dragging) localValue else value

    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestOnTextClick by rememberUpdatedState(onValueTextClick)
    // Read through updated state so a mid-drag bounds change (e.g. a self-widening VIR parameter)
    // doesn't restart the gesture detector and strand the drag without an onDragEnd.
    val latestRange by rememberUpdatedState(valueRange)
    val latestSpan by rememberUpdatedState(span)

    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
            color = TonexOnSurfaceTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(knobSize)
                .semantics {
                    contentDescription = label
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
                    if (!enabled) disabled()
                    if (enabled) {
                        setProgress { target ->
                            latestOnChange(target.coerceIn(valueRange))
                            latestOnFinished()
                            true
                        }
                    }
                }
                .pointerInput(enabled, travelPx) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            localValue = latestValue
                            dragging = true
                        },
                        onDragEnd = {
                            dragging = false
                            latestOnFinished()
                        },
                        onDragCancel = {
                            dragging = false
                            latestOnFinished()
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        // Right and up both increase; screen y grows downward, hence the minus.
                        val range = latestRange
                        val delta = (dragAmount.x - dragAmount.y) / travelPx * latestSpan
                        val previous = localValue
                        val next = (previous + delta).coerceIn(range)
                        if (next != previous) {
                            localValue = next
                            latestOnChange(next)
                            val hitEdge = next == range.start || next == range.endInclusive
                            if (hitEdge) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(onDoubleTap = { latestOnTextClick?.invoke() })
                },
            contentAlignment = Alignment.Center,
        ) {
            KnobFace(
                fraction = ((shownValue - valueRange.start) / span).coerceIn(0f, 1f),
                bipolar = bipolar,
                active = dragging,
                modifier = Modifier.size(knobSize),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = if (dragging) RigAmber else TonexOnSurfaceSecondary,
            maxLines = 1,
            modifier = if (onValueTextClick != null && enabled) {
                Modifier.clickable { onValueTextClick() }.padding(horizontal = 6.dp, vertical = 2.dp)
            } else {
                Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            },
        )
    }
}

/** The drawn part of the knob: track arc, value arc, knurled body, pointer. */
@Composable
private fun KnobFace(fraction: Float, bipolar: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.06f
        val inset = stroke / 2f + 1f
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val arcTopLeft = Offset(inset, inset)

        // Track.
        drawArc(
            color = RigTrack,
            startAngle = START_ANGLE,
            sweepAngle = SWEEP_ANGLE,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        // Value arc: from the minimum, or from 12 o'clock for a bipolar (e.g. ±dB) parameter.
        val valueAngle = START_ANGLE + SWEEP_ANGLE * fraction
        val (arcStart, arcSweep) = if (bipolar) {
            val center = START_ANGLE + SWEEP_ANGLE / 2f
            if (valueAngle >= center) center to (valueAngle - center) else valueAngle to (center - valueAngle)
        } else {
            START_ANGLE to (valueAngle - START_ANGLE)
        }
        if (arcSweep > 0.5f) {
            drawArc(
                color = RigAmber,
                startAngle = arcStart,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        val c = center
        val bodyRadius = size.minDimension * 0.36f

        // Drop shadow, then the brushed body lit from the top-left.
        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = bodyRadius * 1.06f, center = c + Offset(0f, bodyRadius * 0.08f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RigKnobLight, RigKnobDark),
                center = c + Offset(-bodyRadius * 0.35f, -bodyRadius * 0.45f),
                radius = bodyRadius * 1.6f,
            ),
            radius = bodyRadius,
            center = c,
        )

        // Knurling around the rim.
        val ticks = 28
        for (i in 0 until ticks) {
            val a = (i * 360f / ticks) * (PI / 180f).toFloat()
            val outer = Offset(c.x + cos(a) * bodyRadius, c.y + sin(a) * bodyRadius)
            val inner = Offset(c.x + cos(a) * bodyRadius * 0.88f, c.y + sin(a) * bodyRadius * 0.88f)
            drawLine(color = Color.Black.copy(alpha = 0.35f), start = inner, end = outer, strokeWidth = 1.2f)
        }

        // Cap.
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(RigKnobLight.copy(alpha = 0.9f), RigKnobDark),
                start = c - Offset(bodyRadius, bodyRadius),
                end = c + Offset(bodyRadius, bodyRadius),
            ),
            radius = bodyRadius * 0.72f,
            center = c,
        )

        // Pointer.
        val rad = valueAngle * (PI / 180f).toFloat()
        val pStart = Offset(c.x + cos(rad) * bodyRadius * 0.22f, c.y + sin(rad) * bodyRadius * 0.22f)
        val pEnd = Offset(c.x + cos(rad) * bodyRadius * 0.86f, c.y + sin(rad) * bodyRadius * 0.86f)
        drawLine(
            color = if (active) RigAmber else Color.White.copy(alpha = 0.92f),
            start = pStart,
            end = pEnd,
            strokeWidth = stroke * 0.7f,
            cap = StrokeCap.Round,
        )
    }
}

@Preview(name = "Rotary knob", backgroundColor = 0xFF151517, showBackground = true)
@Composable
private fun RotaryKnobPreview() {
    TonexTheme {
        Box(Modifier.padding(16.dp)) {
            RotaryKnob(
                label = "Gain",
                value = 6.4f,
                valueRange = 0f..10f,
                valueText = "6.4",
                onValueChange = {},
                onValueChangeFinished = {},
            )
        }
    }
}

@Preview(name = "Rotary knob — bipolar", backgroundColor = 0xFF151517, showBackground = true)
@Composable
private fun RotaryKnobBipolarPreview() {
    TonexTheme {
        Box(Modifier.padding(16.dp)) {
            RotaryKnob(
                label = "Bass",
                value = -4f,
                valueRange = -15f..15f,
                valueText = "-4.0 dB",
                onValueChange = {},
                onValueChangeFinished = {},
            )
        }
    }
}
