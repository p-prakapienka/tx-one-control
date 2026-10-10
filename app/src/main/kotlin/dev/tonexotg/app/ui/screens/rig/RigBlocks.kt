package dev.tonexotg.app.ui.screens.rig

import dev.tonexotg.app.ui.screens.parameters.CategoryUiState
import dev.tonexotg.app.ui.screens.parameters.ParameterRow
import dev.tonexotg.protocol.ParameterId
import dev.tonexotg.protocol.params.ParameterRegistry
import java.util.Locale

/**
 * The eight processing blocks shown in the rig screen's signal-chain strip, each mapped onto one
 * of [dev.tonexotg.app.ui.screens.parameters.ParameterCatalog.categories] by title.
 *
 * This is a *view* over the existing catalog — no new parameters, no new write paths. Every
 * toggle and knob on the rig screen goes through the same [ParameterEditorViewModel] calls the
 * classic editor uses.
 *
 * @property chipLabel the short label printed on the block's tile.
 * @property title the heading shown above the block's knobs.
 * @property categoryTitle the [CategoryUiState.title] this block renders.
 * @property enableId the block's on/off `SWITCH`, or `null` when it has none (EQ; the cabinet is
 *   switched off through its model selector instead — see [cabDisabledLabel]).
 * @property postId the block's pre/post-amp `SWITCH`, or `null` for the amp and cabinet.
 */
enum class RigBlock(
    val chipLabel: String,
    val title: String,
    val categoryTitle: String,
    val enableId: ParameterId?,
    val postId: ParameterId?,
) {
    GATE("GATE", "Noise Gate", "Noise Gate", ParameterId(1), ParameterId(0)),
    COMP("COMP", "Compressor", "Compressor", ParameterId(6), ParameterId(5)),
    EQ("EQ", "Equalizer", "EQ", null, ParameterId(10)),
    AMP("AMP", "Amp / Tone Model", "Tone Model / Amp", ParameterId(18), null),
    CAB("CAB", "Cabinet", "Cabinet", null, null),
    MOD("MOD", "Modulation", "Modulation", ParameterId(64), ParameterId(63)),
    DELAY("DLY", "Delay", "Delay", ParameterId(95), ParameterId(94)),
    REVERB("VERB", "Reverb", "Reverb", ParameterId(37), ParameterId(36)),
}

/** The cabinet bank's option label that means "cab sim off" (`CABINET_TYPE` = 2). */
internal const val cabDisabledLabel = "Disabled"

/**
 * One block's live state, derived from the editor's [CategoryUiState]s.
 *
 * @property category the block's category, or `null` when the editor has no state yet
 *   (disconnected / still loading).
 * @property enabled whether the block is on, or `null` when unknown or not switchable.
 * @property isPost whether the block sits after the amp, or `null` when unknown or fixed.
 */
data class RigBlockState(
    val block: RigBlock,
    val category: CategoryUiState?,
    val enabled: Boolean?,
    val isPost: Boolean?,
)

/** Every row a category renders, in display order (the model selector excluded). */
internal fun CategoryUiState.allRows(): List<ParameterRow> = when (this) {
    is CategoryUiState.Flat -> rows
    is CategoryUiState.Banked -> alwaysOnRows + modelRows
}

/** Builds one [RigBlockState] per [RigBlock], in [RigBlock] declaration order. */
internal fun buildRigBlocks(categories: List<CategoryUiState>): List<RigBlockState> {
    val byTitle = categories.associateBy { it.title }
    return RigBlock.entries.map { block ->
        val category = byTitle[block.categoryTitle]
        val switches = category?.allRows().orEmpty().filterIsInstance<ParameterRow.Switch>()
        fun switchValue(id: ParameterId?): Boolean? = id?.let { wanted -> switches.firstOrNull { it.id == wanted }?.checked }

        val enabled = if (block == RigBlock.CAB) {
            (category as? CategoryUiState.Banked)?.let { banked ->
                banked.selector.optionLabel(banked.selector.selectedIndex) != cabDisabledLabel
            }
        } else {
            switchValue(block.enableId)
        }
        RigBlockState(block = block, category = category, enabled = enabled, isPost = switchValue(block.postId))
    }
}

/**
 * Orders blocks the way signal actually flows: pre-amp blocks, then amp → cab, then post-amp
 * blocks — each group keeping [RigBlock] order. Display only; nothing is written.
 *
 * If any movable block's position is still unknown (no live state yet), falls back to plain
 * [RigBlock] order rather than guessing where it sits.
 */
internal fun chainOrder(states: List<RigBlockState>): List<RigBlockState> {
    val movable = states.filter { it.block.postId != null }
    if (movable.any { it.isPost == null }) return states
    val core = states.filter { it.block.postId == null }
    return movable.filter { it.isPost == false } + core + movable.filter { it.isPost == true }
}

/** Leading enum-name words that only repeat the block or model name, dropped from knob labels. */
private val redundantPrefixWords = setOf(
    "NOISE", "GATE", "COMP", "EQ", "MODEL", "MODEX", "VIR", "CABINET",
    "REVERB", "SPRING1", "SPRING2", "SPRING3", "SPRING4", "ROOM", "PLATE",
    "MODULATION", "CHORUS", "TREMOLO", "PHASER", "FLANGER", "ROTARY",
    "DELAY", "DIGITAL", "TAPE",
)

private val wordOverrides = mapOf(
    "MIDQ" to "Mid Q",
    "FREQ" to "Freq",
    "TS" to "Division",
    "THRESH" to "Threshold",
    "RESO" to "Resonance",
)

/**
 * A short knob label: `NOISE_GATE_THRESHOLD` → "Threshold", `REVERB_SPRING1_PREDELAY` →
 * "Predelay", `EQ_BASS_FREQ` → "Bass Freq". Falls back to [fallback] if stripping leaves nothing.
 */
internal fun shortParameterLabel(id: ParameterId, fallback: String): String {
    val enumName = ParameterRegistry.byIndex(id.index)?.enumName ?: return fallback
    val words = enumName.split('_').dropWhile { it in redundantPrefixWords }
    // `CABINET_UNKNOWN` (idx 23) would strip to a bare "Unknown"; keep the full name there.
    if (words.isEmpty() || words == listOf("UNKNOWN")) return fallback
    return words.joinToString(" ") { word ->
        wordOverrides[word] ?: word.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
    }
}
