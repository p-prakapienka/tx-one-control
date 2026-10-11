package dev.tonexotg.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Line icons drawn on a [Canvas], in the same spirit as the preset list's hand-drawn glyphs:
 * `material-icons-extended` costs ~4MB of release APK for three shapes (see `PresetListScreen`'s
 * "Glyphs" note), so these are a few strokes each instead.
 */
enum class RigIcon { BACK, RECONNECT, SETTINGS }

/** A 48dp circular tap target holding one [RigIcon]. [description] is what TalkBack reads. */
@Composable
fun RigIconButton(
    icon: RigIcon,
    description: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(iconSize)) {
            val color = if (enabled) tint else tint.copy(alpha = 0.4f)
            when (icon) {
                RigIcon.BACK -> drawBack(color)
                RigIcon.RECONNECT -> drawReconnect(color)
                RigIcon.SETTINGS -> drawSettings(color)
            }
        }
    }
}

private fun DrawScope.stroke() = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** ← : a shaft with a chevron head. */
private fun DrawScope.drawBack(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.85f, h * 0.5f)
        lineTo(w * 0.15f, h * 0.5f)
        moveTo(w * 0.45f, h * 0.2f)
        lineTo(w * 0.15f, h * 0.5f)
        lineTo(w * 0.45f, h * 0.8f)
    }
    drawPath(path, color, style = stroke())
}

/** ⟳ : a 300° arc with an arrowhead at its open end. */
private fun DrawScope.drawReconnect(color: Color) {
    val s = stroke()
    val inset = s.width / 2f + size.minDimension * 0.08f
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    val startAngle = -60f
    val sweep = 300f
    drawArc(color, startAngle, sweep, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = s)

    // Arrowhead at the arc's start (top right), pointing along the direction of travel.
    val r = arcSize.width / 2f
    val c = center
    val a = startAngle * (PI / 180f).toFloat()
    val tip = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
    val head = size.minDimension * 0.26f
    val path = Path().apply {
        moveTo(tip.x - head, tip.y - head * 0.15f)
        lineTo(tip.x, tip.y)
        lineTo(tip.x - head * 0.15f, tip.y + head)
    }
    drawPath(path, color, style = s)
}

/** ⚙ : a ring with eight teeth around a hub. */
private fun DrawScope.drawSettings(color: Color) {
    val s = stroke()
    val c = center
    val outer = size.minDimension * 0.47f
    val ring = size.minDimension * 0.32f
    val hub = size.minDimension * 0.12f
    drawCircle(color, radius = ring, center = c, style = s)
    drawCircle(color, radius = hub, center = c, style = s)
    val teeth = 8
    for (i in 0 until teeth) {
        val a = (i * 360f / teeth) * (PI / 180f).toFloat()
        drawLine(
            color = color,
            start = Offset(c.x + cos(a) * ring, c.y + sin(a) * ring),
            end = Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
            strokeWidth = s.width * 1.6f,
            cap = StrokeCap.Round,
        )
    }
}
