package dev.tonexotg.app.ui.screens.rig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonexotg.app.ui.components.RigLed
import dev.tonexotg.app.ui.components.RotaryKnob
import dev.tonexotg.app.ui.screens.connection.ErrorPresentation
import dev.tonexotg.app.ui.screens.presets.PresetListItem
import dev.tonexotg.app.ui.screens.presets.PresetListViewModel
import dev.tonexotg.app.ui.theme.RigAmber
import dev.tonexotg.app.ui.theme.RigChassis
import dev.tonexotg.app.ui.theme.RigPanel
import dev.tonexotg.app.ui.theme.RigPanelEdge
import dev.tonexotg.app.ui.theme.TonexOnAccent
import dev.tonexotg.app.ui.theme.TonexOnSurfacePrimary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceSecondary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceTertiary
import dev.tonexotg.protocol.PresetSlot

/**
 * The rig home screen: a big LCD for the active preset, an "Edit tone" button into
 * [RigEditorScreen], a 4×5 grid of the 20 onboard presets, and the master volume knob.
 *
 * Tap a preset tile to load it; long-press to assign it to footswitch slot A, B or C. Renaming and
 * the six global settings stay on the Presets tab. Navigation between top-level screens is the
 * app-wide bottom tab bar (`RigTabBar`), not buttons on this screen.
 */
@Composable
fun StageScreen(
    viewModel: PresetListViewModel,
    onEditTone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val active = uiState.items.firstOrNull { it.isActive }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RigChassis),
    ) {
        RigTopBar(title = "Tonex-OTG", onBack = null, actions = {})

        PresetLcd(
            number = active?.index?.value?.plus(1),
            name = active?.displayName ?: if (uiState.isLive) "Loading…" else "No pedal",
            statusLine = if (uiState.isLive) "Live" else "Connect via USB-OTG",
            slots = active?.assignedSlots.orEmpty(),
            onPrevious = presetStepper(uiState, viewModel, -1),
            onNext = presetStepper(uiState, viewModel, +1),
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(12.dp))
        EditToneButton(enabled = uiState.isLive, onClick = onEditTone)
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("stage.presetGrid"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.items, key = { it.index.value }) { item ->
                PresetTile(
                    item = item,
                    enabled = uiState.isLive,
                    onSelect = { viewModel.selectPreset(item.index) },
                    onAssign = { slot -> viewModel.assignToSlot(item.index, slot) },
                )
            }
        }

        uiState.masterVolume?.let { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "MASTER",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                        color = TonexOnSurfacePrimary,
                    )
                    Text(
                        text = "Output level",
                        style = MaterialTheme.typography.labelSmall,
                        color = TonexOnSurfaceTertiary,
                    )
                }
                RotaryKnob(
                    label = "Volume",
                    value = row.value,
                    valueRange = row.range,
                    valueText = row.valueText,
                    // PresetListViewModel throttles each drag step itself; there is no separate
                    // "finished" step on this path (same as the classic home-screen dock).
                    onValueChange = viewModel::onMasterVolumeDrag,
                    onValueChangeFinished = {},
                    knobSize = 52.dp,
                    modifier = Modifier.testTag("stage.masterKnob"),
                )
            }
        }
    }

    uiState.selectPresetErrorPresentation?.let { presentation ->
        ErrorAlert(title = "Couldn't load preset", presentation = presentation, onDismiss = viewModel::dismissSelectError)
    }
    uiState.assignSlotErrorPresentation?.let { presentation ->
        ErrorAlert(title = "Couldn't assign slot", presentation = presentation, onDismiss = viewModel::dismissAssignError)
    }
}

@Composable
private fun EditToneButton(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (enabled) Modifier.shadow(10.dp, shape, ambientColor = RigAmber, spotColor = RigAmber) else Modifier)
            .clip(shape)
            .background(RigAmber)
            .combinedClickableCompat(onClick = onClick, onLongClick = null, role = Role.Button, enabled = enabled)
            .testTag("stage.editToneButton"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "EDIT TONE",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 3.sp),
            color = TonexOnAccent,
        )
    }
}

@Composable
private fun PresetTile(
    item: PresetListItem,
    enabled: Boolean,
    onSelect: () -> Unit,
    onAssign: (PresetSlot) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .alpha(if (enabled) 1f else 0.5f)
                .then(if (item.isActive) Modifier.shadow(8.dp, shape, ambientColor = RigAmber, spotColor = RigAmber) else Modifier)
                .clip(shape)
                .background(RigPanel)
                .border(if (item.isActive) 2.dp else 1.dp, if (item.isActive) RigAmber else RigPanelEdge, shape)
                .semantics {
                    contentDescription = "Preset ${item.index.value + 1}, ${item.displayName}"
                    selected = item.isActive
                }
                .combinedClickableCompat(
                    onClick = onSelect,
                    onLongClick = { menuOpen = true },
                    role = Role.Button,
                    enabled = enabled,
                )
                .testTag("stage.preset.${item.index.value}")
                .padding(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (item.index.value + 1).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                    color = if (item.isActive) RigAmber else TonexOnSurfaceTertiary,
                    modifier = Modifier.weight(1f),
                )
                RigLed(lit = item.isActive, size = 7.dp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (item.isActive) TonexOnSurfacePrimary else TonexOnSurfaceSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.assignedSlots.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row {
                    item.assignedSlots.sortedBy { it.ordinal }.forEach { slot ->
                        SlotBadge(slot)
                        Spacer(Modifier.width(3.dp))
                    }
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            PresetSlot.entries.forEach { slot ->
                DropdownMenuItem(
                    text = { Text("Assign to footswitch ${slot.name}") },
                    leadingIcon = { SlotBadge(slot) },
                    onClick = {
                        menuOpen = false
                        onAssign(slot)
                    },
                    modifier = Modifier.testTag("stage.assign.${item.index.value}.${slot.name}"),
                )
            }
        }
    }
}

@Composable
private fun ErrorAlert(title: String, presentation: ErrorPresentation, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text(title) },
        text = { Text("${presentation.title}\n\n${presentation.detail}\n\n${presentation.advice}") },
    )
}
