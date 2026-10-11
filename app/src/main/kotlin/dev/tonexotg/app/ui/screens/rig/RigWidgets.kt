package dev.tonexotg.app.ui.screens.rig

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonexotg.app.ui.components.RigLed
import dev.tonexotg.app.ui.theme.RigAmber
import dev.tonexotg.app.ui.theme.RigLcd
import dev.tonexotg.app.ui.theme.RigLcdText
import dev.tonexotg.app.ui.theme.RigLcdTextDim
import dev.tonexotg.app.ui.theme.RigPanel
import dev.tonexotg.app.ui.theme.RigPanelEdge
import dev.tonexotg.app.ui.theme.RigSlotA
import dev.tonexotg.app.ui.theme.RigSlotB
import dev.tonexotg.app.ui.theme.RigSlotC
import dev.tonexotg.app.ui.theme.RigTrack
import dev.tonexotg.app.ui.theme.TonexOnSurfacePrimary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceTertiary
import dev.tonexotg.app.ui.theme.TonexTheme
import dev.tonexotg.protocol.PresetSlot

/** Badge color for a footswitch slot. */
internal fun PresetSlot.badgeColor(): Color = when (this) {
    PresetSlot.A -> RigSlotA
    PresetSlot.B -> RigSlotB
    PresetSlot.C -> RigSlotC
}

/** A small rounded "A"/"B"/"C" badge. */
@Composable
internal fun SlotBadge(slot: PresetSlot, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(5.dp))
            .background(slot.badgeColor()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = slot.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = if (size < 18.dp) 9.sp else 11.sp),
            color = Color.Black,
        )
    }
}

/**
 * The LCD-style preset readout: big two-digit preset number, preset name, a status line, and
 * previous/next buttons either side. [number] is 1-based; `null` shows dashes (no live pedal).
 */
@Composable
internal fun PresetLcd(
    number: Int?,
    name: String,
    statusLine: String,
    slots: Set<PresetSlot>,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    large: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RigPanel)
            .border(1.dp, RigPanelEdge, RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(glyph = "‹", contentDescription = "Previous preset", onClick = onPrevious)
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(listOf(RigLcd, RigLcd.copy(alpha = 0.85f))),
                )
                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = if (large) 12.dp else 8.dp)
                .testTag("rig.presetLcd"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number?.toString()?.padStart(2, '0') ?: "--",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (large) 44.sp else 30.sp,
                ),
                color = RigLcdText,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = RigLcdText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    slots.sortedBy { it.ordinal }.forEach { slot ->
                        SlotBadge(slot)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = statusLine.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                        color = RigLcdTextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        StepButton(glyph = "›", contentDescription = "Next preset", onClick = onNext)
    }
}

@Composable
private fun StepButton(glyph: String, contentDescription: String, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 56.dp)
            .alpha(if (onClick != null) 1f else 0.35f)
            .clip(RoundedCornerShape(10.dp))
            .background(RigTrack)
            .semantics { this.contentDescription = contentDescription }
            .then(
                if (onClick != null) {
                    Modifier.combinedClickableCompat(onClick = onClick, onLongClick = null, role = Role.Button)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, style = MaterialTheme.typography.headlineMedium, color = TonexOnSurfacePrimary)
    }
}

/**
 * The signal-chain strip: one tile per block, in signal-flow order, joined by short "cables".
 * Tap a tile to edit that block; long-press to switch it on or off.
 */
@Composable
internal fun SignalChainStrip(
    blocks: List<RigBlockState>,
    selected: RigBlock,
    onSelect: (RigBlock) -> Unit,
    onToggle: (RigBlockState) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        blocks.forEachIndexed { i, state ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(10.dp)
                        .height(3.dp)
                        .background(if (state.enabled == false) RigTrack else RigAmber.copy(alpha = 0.5f)),
                )
            }
            BlockTile(
                state = state,
                selected = state.block == selected,
                enabled = enabled,
                onClick = { onSelect(state.block) },
                onLongClick = { onToggle(state) },
            )
        }
    }
}

@Composable
private fun BlockTile(
    state: RigBlockState,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val on = state.enabled != false
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .size(width = 64.dp, height = 64.dp)
            .then(if (selected) Modifier.shadow(8.dp, shape, ambientColor = RigAmber, spotColor = RigAmber) else Modifier)
            .clip(shape)
            .background(RigPanel)
            .border(if (selected) 2.dp else 1.dp, if (selected) RigAmber else RigPanelEdge, shape)
            .semantics {
                contentDescription = state.block.title
                stateDescription = when (state.enabled) {
                    true -> "On"
                    false -> "Off"
                    null -> ""
                }
            }
            .combinedClickableCompat(
                onClick = onClick,
                onLongClick = if (enabled && state.enabled != null) onLongClick else null,
                role = Role.Tab,
            )
            .testTag("rig.block.${state.block.name}")
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            RigLed(lit = state.enabled == true, size = 7.dp)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = state.block.chipLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
            color = if (on) TonexOnSurfacePrimary else TonexOnSurfaceTertiary,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(width = 20.dp, height = 3.dp)
                .clip(CircleShape)
                .background(if (selected) RigAmber else Color.Transparent),
        )
    }
}

/** `combinedClickable` behind one opt-in, so callers don't each need the annotation. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.combinedClickableCompat(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    role: Role? = null,
    enabled: Boolean = true,
): Modifier = this.combinedClickable(enabled = enabled, role = role, onLongClick = onLongClick, onClick = onClick)

@Preview(name = "Preset LCD + chain", backgroundColor = 0xFF151517, showBackground = true, widthDp = 380)
@Composable
private fun RigWidgetsPreview() {
    TonexTheme {
        Column(Modifier.padding(12.dp)) {
            PresetLcd(
                number = 7,
                name = "Plexi Crunch",
                statusLine = "Live",
                slots = setOf(PresetSlot.A),
                onPrevious = {},
                onNext = {},
            )
            SignalChainStrip(
                blocks = RigBlock.entries.map { RigBlockState(it, null, enabled = it != RigBlock.MOD, isPost = null) },
                selected = RigBlock.AMP,
                onSelect = {},
                onToggle = {},
            )
        }
    }
}
