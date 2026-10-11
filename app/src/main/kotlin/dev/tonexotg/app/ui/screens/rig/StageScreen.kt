package dev.tonexotg.app.ui.screens.rig

import android.os.SystemClock
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalViewConfiguration
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
import dev.tonexotg.app.ui.components.RigSegmented
import dev.tonexotg.app.ui.components.RotaryKnob
import dev.tonexotg.app.ui.screens.connection.ErrorPresentation
import dev.tonexotg.app.ui.screens.presets.AliasEditDialog
import dev.tonexotg.app.ui.screens.presets.PresetListItem
import dev.tonexotg.app.ui.screens.presets.PresetListViewModel
import dev.tonexotg.app.ui.theme.RigAmber
import dev.tonexotg.app.ui.theme.RigChassis
import dev.tonexotg.app.ui.theme.RigPanel
import dev.tonexotg.app.ui.theme.RigPanelEdge
import dev.tonexotg.app.ui.theme.TonexOnSurfacePrimary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceSecondary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceTertiary
import dev.tonexotg.protocol.PresetSlot

/**
 * The rig home screen: a big LCD for the active preset, an "Edit tone" button into
 * [RigEditorScreen], a 4×5 grid of the 20 onboard presets, and the master volume knob.
 *
 * The A/B/C switch picks the pedal's active footswitch slot (A/B mode for A and B, stomp mode for
 * C); tapping a pad loads that preset into the active slot, like the pedal's own preset knob.
 * Presets held by the other two slots are dimmed and can't be loaded, and the LCD arrows skip
 * them. Double-tap a pad to load it and open [RigEditorScreen]; long-press for a menu: edit,
 * rename (local alias) and assign to footswitch slot A, B or C. The pedal's global settings live
 * on the Settings screen (the gear in the connection bar).
 */
@Composable
fun StageScreen(
    viewModel: PresetListViewModel,
    onEditTone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val active = uiState.items.firstOrNull { it.isActive }
    var renamingIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RigChassis),
    ) {
        RigTopBar(title = "TX One Control", onBack = null, actions = {})

        PresetLcd(
            number = active?.index?.value?.plus(1),
            name = active?.displayName ?: if (uiState.isLive) "Loading…" else "No pedal",
            statusLine = if (uiState.isLive) "Live · double-tap a pad to edit" else "Connect via USB-OTG",
            slots = active?.assignedSlots.orEmpty(),
            onPrevious = presetStepper(uiState, viewModel, -1),
            onNext = presetStepper(uiState, viewModel, +1),
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        SlotSwitchRow(
            activeSlot = uiState.activeSlot,
            enabled = uiState.isLive && uiState.activeSlot != null,
            onSelect = viewModel::selectSlot,
        )
        HorizontalDivider(color = RigPanelEdge)

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("stage.presetGrid"),
            // The grid scrolls when the 20 tiles don't fit. The divider above and this top
            // padding keep a partly scrolled row visibly inside the grid.
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.items, key = { it.index.value }) { item ->
                PresetTile(
                    item = item,
                    live = uiState.isLive,
                    selectable = uiState.isSelectable(item),
                    onSelect = { viewModel.selectPreset(item.index) },
                    onEdit = {
                        // Load first (a no-op if it's already active): the editor always edits the
                        // pedal's *active* preset, never an index captured here.
                        viewModel.selectPreset(item.index)
                        onEditTone()
                    },
                    onRename = { renamingIndex = item.index.value },
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

    val renamingItem = renamingIndex?.let { i -> uiState.items.getOrNull(i) }
    if (renamingItem != null) {
        AliasEditDialog(
            item = renamingItem,
            onConfirm = { newAlias ->
                viewModel.setAlias(renamingItem.index, newAlias)
                renamingIndex = null
            },
            onCancel = { renamingIndex = null },
        )
    }

    uiState.selectPresetErrorPresentation?.let { presentation ->
        ErrorAlert(title = "Couldn't load preset", presentation = presentation, onDismiss = viewModel::dismissSelectError)
    }
    uiState.assignSlotErrorPresentation?.let { presentation ->
        ErrorAlert(title = "Couldn't assign slot", presentation = presentation, onDismiss = viewModel::dismissAssignError)
    }
}

/**
 * The footswitch slot switch: A, B or C, plus the mode it implies. Shows the slot the pedal
 * reports; a tap asks the pedal to switch and the highlight moves when its state push confirms.
 */
@Composable
private fun SlotSwitchRow(activeSlot: PresetSlot?, enabled: Boolean, onSelect: (PresetSlot) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "SLOT",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
            color = TonexOnSurfacePrimary,
        )
        Spacer(Modifier.width(12.dp))
        RigSegmented(
            options = PresetSlot.entries.map { it.name },
            selectedIndex = activeSlot?.ordinal ?: -1,
            onSelect = { onSelect(PresetSlot.entries[it]) },
            enabled = enabled,
            modifier = Modifier.testTag("stage.slotSwitch"),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = when (activeSlot) {
                null -> ""
                PresetSlot.C -> "STOMP MODE"
                else -> "A/B MODE"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
            color = TonexOnSurfaceTertiary,
        )
    }
}

@Composable
private fun PresetTile(
    item: PresetListItem,
    live: Boolean,
    selectable: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onAssign: (PresetSlot) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    // Double-tap without delaying the single tap: the first tap loads the preset immediately (a
    // stock double-click detector would hold it back ~300ms waiting for a second tap), and a
    // second tap inside the system double-tap window opens the editor on the preset just loaded.
    val doubleTapTimeoutMillis = LocalViewConfiguration.current.doubleTapTimeoutMillis
    var lastTapMillis by remember { mutableLongStateOf(0L) }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Wider than tall, so all five rows fit on more phones without scrolling.
                .aspectRatio(1.3f)
                // Held by another footswitch slot: visible (its badge says where) but not loadable.
                .alpha(if (selectable) 1f else if (live) 0.35f else 0.5f)
                .then(if (item.isActive) Modifier.shadow(8.dp, shape, ambientColor = RigAmber, spotColor = RigAmber) else Modifier)
                .clip(shape)
                .background(RigPanel)
                .border(if (item.isActive) 2.dp else 1.dp, if (item.isActive) RigAmber else RigPanelEdge, shape)
                .semantics {
                    contentDescription = "Preset ${item.index.value + 1}, ${item.displayName}"
                    selected = item.isActive
                }
                .combinedClickableCompat(
                    onClick = {
                        if (selectable) {
                            val now = SystemClock.uptimeMillis()
                            if (now - lastTapMillis <= doubleTapTimeoutMillis) {
                                lastTapMillis = 0L
                                onEdit()
                            } else {
                                lastTapMillis = now
                                onSelect()
                            }
                        }
                    },
                    // Renaming works offline (aliases are local), so the menu is always available.
                    onLongClick = { menuOpen = true },
                    role = Role.Button,
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
                // Footswitch badges share the header row; below the name they were clipped
                // whenever the name took two lines. The LED steps aside for them (the active tile
                // still has its amber border and number).
                if (item.assignedSlots.isNotEmpty()) {
                    item.assignedSlots.sortedBy { it.ordinal }.forEach { slot ->
                        Spacer(Modifier.width(2.dp))
                        SlotBadge(slot, size = 14.dp)
                    }
                } else {
                    RigLed(lit = item.isActive, size = 7.dp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (item.isActive) TonexOnSurfacePrimary else TonexOnSurfaceSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (selectable) {
                DropdownMenuItem(
                    text = { Text("Edit tone") },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                    modifier = Modifier.testTag("stage.edit.${item.index.value}"),
                )
            }
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    menuOpen = false
                    onRename()
                },
                modifier = Modifier.testTag("stage.rename.${item.index.value}"),
            )
            // Slot assignments are only known (and writable) while connected.
            if (live) {
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
