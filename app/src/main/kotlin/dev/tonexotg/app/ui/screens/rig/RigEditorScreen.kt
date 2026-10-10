package dev.tonexotg.app.ui.screens.rig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonexotg.app.ui.components.LedToggle
import dev.tonexotg.app.ui.components.ParameterStepper
import dev.tonexotg.app.ui.components.RigSegmented
import dev.tonexotg.app.ui.components.RotaryKnob
import dev.tonexotg.app.ui.screens.parameters.CategoryUiState
import dev.tonexotg.app.ui.screens.parameters.ParameterEditorDialogs
import dev.tonexotg.app.ui.screens.parameters.ParameterEditorViewModel
import dev.tonexotg.app.ui.screens.parameters.ParameterRow
import dev.tonexotg.app.ui.screens.presets.PresetListUiState
import dev.tonexotg.app.ui.screens.presets.PresetListViewModel
import dev.tonexotg.app.ui.theme.RigAmber
import dev.tonexotg.app.ui.theme.RigChassis
import dev.tonexotg.app.ui.theme.RigPanel
import dev.tonexotg.app.ui.theme.RigPanelEdge
import dev.tonexotg.app.ui.theme.TonexOnSurfacePrimary
import dev.tonexotg.app.ui.theme.TonexOnSurfaceTertiary
import dev.tonexotg.protocol.ConnectionState
import dev.tonexotg.protocol.ParameterId
import dev.tonexotg.protocol.PresetIndex

/**
 * The "rig" editor: a hardware-style view of the active preset — LCD readout, a signal-chain
 * strip of blocks, and the selected block's knobs. A different *view* over the same
 * [ParameterEditorViewModel] the classic editor uses; every write goes through its existing,
 * throttled paths, and every dialog is the classic editor's own ([ParameterEditorDialogs]).
 *
 * [presetListViewModel] is used only for the LCD's previous/next buttons and slot badges.
 */
@Composable
fun RigEditorScreen(
    viewModel: ParameterEditorViewModel,
    presetListViewModel: PresetListViewModel,
    onBack: () -> Unit,
    onOpenClassicEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val presetState by presetListViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedBlock by rememberSaveable { mutableStateOf(RigBlock.AMP) }

    val externalMessage = uiState.externalPresetChangeMessage
    LaunchedEffect(externalMessage) {
        if (externalMessage != null) {
            snackbarHostState.showSnackbar(externalMessage, duration = SnackbarDuration.Short)
            viewModel.onExternalPresetChangeMessageShown()
        }
    }

    val live = uiState.connectionState is ConnectionState.Ready
    val blocks = remember(uiState.categories) { chainOrder(buildRigBlocks(uiState.categories)) }
    val selectedState = blocks.firstOrNull { it.block == selectedBlock }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RigChassis),
    ) {
        Column(Modifier.fillMaxSize()) {
            RigTopBar(
                title = "Edit Tone",
                onBack = onBack,
                actions = {
                    TextButton(onClick = onOpenClassicEditor, modifier = Modifier.testTag("rig.classicButton")) {
                        Text("Classic", color = TonexOnSurfaceTertiary)
                    }
                    TextButton(onClick = viewModel::onRevertRequested, enabled = live, modifier = Modifier.testTag("rig.revertButton")) {
                        Text("Revert", color = if (live) RigAmber else TonexOnSurfaceTertiary)
                    }
                },
            )

            PresetLcd(
                number = uiState.presetIndex?.value?.plus(1),
                name = uiState.presetName.ifBlank { if (live) "Loading…" else "No pedal" },
                statusLine = if (live) "Editing · changes save to pedal" else "Connect via USB-OTG",
                slots = presetState.items.firstOrNull { it.isActive }?.assignedSlots.orEmpty(),
                onPrevious = presetStepper(presetState, presetListViewModel, -1),
                onNext = presetStepper(presetState, presetListViewModel, +1),
                large = false,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            SignalChainStrip(
                blocks = blocks,
                selected = selectedBlock,
                onSelect = { selectedBlock = it },
                onToggle = { state -> toggleBlock(state, viewModel) },
                enabled = live,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RigPanel)
                    .border(1.dp, RigPanelEdge, RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("rig.blockPanel"),
            ) {
                if (selectedState?.category == null) {
                    EmptyPanel(live = live)
                } else {
                    BlockPanel(state = selectedState, viewModel = viewModel, enabled = live)
                }
            }

            uiState.masterVolume?.let { row ->
                MasterDock(row = row, viewModel = viewModel, enabled = live)
            } ?: Spacer(Modifier.height(12.dp))
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    ParameterEditorDialogs(uiState = uiState, viewModel = viewModel)
}

/** Previous/next preset handler for the LCD, or `null` (button dimmed) when there's no live pedal. */
internal fun presetStepper(state: PresetListUiState, viewModel: PresetListViewModel, step: Int): (() -> Unit)? {
    if (!state.isLive) return null
    val current = state.items.firstOrNull { it.isActive }?.index?.value ?: return null
    val count = state.items.size
    return { viewModel.selectPreset(PresetIndex(((current + step) % count + count) % count)) }
}

/** Long-press on a chain tile: flip the block's enable switch (or, for the cab, Disabled ↔ Tone Model). */
private fun toggleBlock(state: RigBlockState, viewModel: ParameterEditorViewModel) {
    val enabled = state.enabled ?: return
    if (state.block == RigBlock.CAB) {
        val banked = state.category as? CategoryUiState.Banked ?: return
        val options = banked.selector.options
        val disabledOption = options.firstOrNull { banked.selector.optionLabel(it) == cabDisabledLabel } ?: return
        val target = if (enabled) disabledOption else options.first { it != disabledOption }
        viewModel.onModelSelect(banked.selector.id, target)
        return
    }
    val id = state.block.enableId ?: return
    viewModel.onSwitchToggle(id, !enabled)
}

@Composable
internal fun RigTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("rig.backButton")) {
                Text("‹ Back", color = TonexOnSurfacePrimary)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
            color = TonexOnSurfacePrimary,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
private fun EmptyPanel(live: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (live) "Reading preset…" else "No pedal connected",
            style = MaterialTheme.typography.titleMedium,
            color = TonexOnSurfacePrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (live) "Parameters appear as soon as the pedal reports them." else "Plug in your ToneX One with a USB-OTG cable.",
            style = MaterialTheme.typography.bodyMedium,
            color = TonexOnSurfaceTertiary,
        )
    }
}

/** The selected block: header (name, PRE/POST, ON), model selector, knobs, switches, steppers. */
@Composable
private fun BlockPanel(state: RigBlockState, viewModel: ParameterEditorViewModel, enabled: Boolean) {
    val category = state.category ?: return
    val hiddenSwitchIds: Set<ParameterId> = setOfNotNull(state.block.enableId, state.block.postId)
    val rows = category.allRows()
    val ranges = rows.filterIsInstance<ParameterRow.Range>()
    val switches = rows.filterIsInstance<ParameterRow.Switch>().filter { it.id !in hiddenSwitchIds }
    val steppers = rows.filterIsInstance<ParameterRow.Stepper>()

    // Header.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = state.block.title.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
            color = TonexOnSurfacePrimary,
            modifier = Modifier.weight(1f),
        )
        val enableId = state.block.enableId
        if (enableId != null && state.enabled != null) {
            LedToggle(
                label = if (state.enabled) "On" else "Off",
                checked = state.enabled,
                onCheckedChange = { viewModel.onSwitchToggle(enableId, it) },
                enabled = enabled,
                modifier = Modifier.testTag("rig.enable.${state.block.name}"),
            )
        }
    }

    val postId = state.block.postId
    if (postId != null && state.isPost != null) {
        Spacer(Modifier.height(12.dp))
        RigSegmented(
            options = listOf("Pre amp", "Post amp"),
            selectedIndex = if (state.isPost) 1 else 0,
            onSelect = { viewModel.onSwitchToggle(postId, it == 1) },
            enabled = enabled,
            modifier = Modifier.testTag("rig.position.${state.block.name}"),
        )
    }

    if (category is CategoryUiState.Banked) {
        Spacer(Modifier.height(12.dp))
        val selector = category.selector
        RigSegmented(
            options = selector.options.map(selector.optionLabel),
            selectedIndex = selector.options.indexOf(selector.selectedIndex).coerceAtLeast(0),
            onSelect = { i -> viewModel.onModelSelect(selector.id, selector.options[i]) },
            enabled = enabled,
            modifier = Modifier.testTag("rig.model.${state.block.name}"),
        )
    }

    if (ranges.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        ranges.chunked(3).forEach { rowOfKnobs ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowOfKnobs.forEach { row ->
                    // Same disposal rule as the classic editor (issue #104): a knob leaving
                    // composition mid-drag (model change, block switch) must still end its gesture.
                    DisposableEffect(row.id) {
                        onDispose { viewModel.onRangeDragEnd(row.id) }
                    }
                    RotaryKnob(
                        label = shortParameterLabel(row.id, row.label),
                        value = row.value,
                        valueRange = row.range,
                        valueText = row.valueText,
                        onValueChange = { viewModel.onRangeDrag(row.id, it) },
                        onValueChangeFinished = { viewModel.onRangeDragEnd(row.id) },
                        onValueTextClick = { viewModel.onNumericEntryOpen(row.id) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rig.knob.${row.id.index}"),
                    )
                }
                // Keep a short last row left-aligned under the columns above it.
                repeat(3 - rowOfKnobs.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (switches.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        switches.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { row ->
                    LedToggle(
                        label = shortParameterLabel(row.id, row.label),
                        checked = row.checked,
                        onCheckedChange = { viewModel.onSwitchToggle(row.id, it) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rig.switch.${row.id.index}"),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    steppers.forEach { row ->
        Spacer(Modifier.height(8.dp))
        ParameterStepper(
            label = shortParameterLabel(row.id, row.label),
            value = row.value,
            range = row.range,
            onValueChange = { viewModel.onStepperChange(row.id, it) },
            enabled = enabled,
            modifier = Modifier.testTag("rig.stepper.${row.id.index}"),
        )
    }

    if (ranges.isEmpty() && switches.isEmpty() && steppers.isEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No adjustable controls for this setting.",
            style = MaterialTheme.typography.bodyMedium,
            color = TonexOnSurfaceTertiary,
        )
    }
}

/** Bottom dock: the master volume knob. */
@Composable
private fun MasterDock(row: ParameterRow.Range, viewModel: ParameterEditorViewModel, enabled: Boolean) {
    DisposableEffect(row.id) {
        onDispose { viewModel.onRangeDragEnd(row.id) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "MASTER",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = TonexOnSurfacePrimary,
            )
            Text(
                text = "Output level · all presets",
                style = MaterialTheme.typography.labelSmall,
                color = TonexOnSurfaceTertiary,
            )
        }
        RotaryKnob(
            label = "Volume",
            value = row.value,
            valueRange = row.range,
            valueText = row.valueText,
            onValueChange = { viewModel.onRangeDrag(row.id, it) },
            onValueChangeFinished = { viewModel.onRangeDragEnd(row.id) },
            onValueTextClick = { viewModel.onNumericEntryOpen(row.id) },
            enabled = enabled,
            knobSize = 56.dp,
            modifier = Modifier.testTag("rig.masterKnob"),
        )
    }
}
