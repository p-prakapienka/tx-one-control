package dev.tonexotg.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonexotg.app.ui.theme.RigAmber
import dev.tonexotg.app.ui.theme.RigChassis
import dev.tonexotg.app.ui.theme.RigPanel
import dev.tonexotg.app.ui.theme.RigPanelEdge
import dev.tonexotg.app.ui.theme.RigTrack
import dev.tonexotg.app.ui.theme.TonexOnAccent
import dev.tonexotg.app.ui.theme.TonexOnSurfaceSecondary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceTertiary
import dev.tonexotg.app.ui.theme.TonexTheme

/** A small status LED: amber and glowing when [lit], a dark dimple otherwise. */
@Composable
fun RigLed(lit: Boolean, modifier: Modifier = Modifier, size: Dp = 8.dp, color: Color = RigAmber) {
    Box(
        modifier = modifier
            .size(size)
            .then(if (lit) Modifier.shadow(elevation = 6.dp, shape = CircleShape, ambientColor = color, spotColor = color) else Modifier)
            .clip(CircleShape)
            .background(if (lit) color else RigTrack),
    )
}

/** A pill-shaped on/off button with an LED, for `SWITCH` parameters on the rig screens. */
@Composable
fun LedToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(RigPanel)
            .border(1.dp, if (checked) RigAmber.copy(alpha = 0.6f) else RigPanelEdge, RoundedCornerShape(20.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RigLed(lit = checked)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
            color = if (checked) TonexOnSurfaceSecondary else TonexOnSurfaceTertiary,
            maxLines = 1,
        )
    }
}

/**
 * A row of mutually exclusive options (model selectors, PRE/POST). Scrolls horizontally when the
 * options don't fit, so a six-model reverb bank never wraps or truncates.
 */
@Composable
fun RigSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(10.dp))
            .background(RigChassis)
            .border(1.dp, RigPanelEdge, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, option ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) RigAmber else Color.Transparent)
                    .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold),
                    color = if (selected) TonexOnAccent else TonexOnSurfaceTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(name = "Rig controls", backgroundColor = 0xFF1F2023, showBackground = true)
@Composable
private fun RigControlsPreview() {
    TonexTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LedToggle(label = "On", checked = true, onCheckedChange = {})
            RigSegmented(options = listOf("Pre", "Post"), selectedIndex = 1, onSelect = {})
        }
    }
}
