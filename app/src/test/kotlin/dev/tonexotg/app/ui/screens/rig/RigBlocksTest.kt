package dev.tonexotg.app.ui.screens.rig

import dev.tonexotg.app.ui.components.KnobTaper
import dev.tonexotg.app.ui.screens.parameters.CategoryUiState
import dev.tonexotg.app.ui.screens.parameters.ModelSelectorUiState
import dev.tonexotg.app.ui.screens.parameters.ParameterCatalog
import dev.tonexotg.app.ui.screens.parameters.ParameterRow
import dev.tonexotg.protocol.ParameterId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RigBlocksTest {

    private fun switch(index: Int, checked: Boolean) =
        ParameterRow.Switch(id = ParameterId(index), label = "s$index", abbreviation = "", checked = checked)

    private fun flat(title: String, vararg rows: ParameterRow) = CategoryUiState.Flat(title, rows.toList())

    private fun cab(selectedIndex: Int): CategoryUiState.Banked {
        val bank = ParameterCatalog.cabinetBank
        return CategoryUiState.Banked(
            title = "Cabinet",
            alwaysOnRows = emptyList(),
            selector = ModelSelectorUiState(
                id = bank.selectorId,
                label = "Cabinet Type",
                options = bank.models.map { it.index },
                selectedIndex = selectedIndex,
                optionLabel = { bank.modelFor(it).label },
            ),
            modelRows = emptyList(),
        )
    }

    @Test
    fun `every block maps onto a real catalog category`() {
        val titles = ParameterCatalog.categories.map { it.title }.toSet()
        RigBlock.entries.forEach { block ->
            assertTrue("${block.name} -> ${block.categoryTitle} not in catalog", block.categoryTitle in titles)
        }
    }

    @Test
    fun `enable and position come from the block's own switches`() {
        val blocks = buildRigBlocks(listOf(flat("Noise Gate", switch(0, true), switch(1, false))))
        val gate = blocks.first { it.block == RigBlock.GATE }
        assertEquals(false, gate.enabled)
        assertEquals(true, gate.isPost)
    }

    @Test
    fun `cabinet is enabled unless its model is Disabled`() {
        assertEquals(true, buildRigBlocks(listOf(cab(0))).first { it.block == RigBlock.CAB }.enabled)
        assertEquals(false, buildRigBlocks(listOf(cab(2))).first { it.block == RigBlock.CAB }.enabled)
    }

    @Test
    fun `missing categories give unknown state`() {
        val amp = buildRigBlocks(emptyList()).first { it.block == RigBlock.AMP }
        assertNull(amp.category)
        assertNull(amp.enabled)
    }

    @Test
    fun `chain order puts pre-amp blocks before amp and cab, post-amp after`() {
        val states = RigBlock.entries.map { block ->
            RigBlockState(block, null, enabled = true, isPost = block.postId?.let { block == RigBlock.EQ || block == RigBlock.REVERB || block == RigBlock.DELAY })
        }
        assertEquals(
            listOf(RigBlock.GATE, RigBlock.COMP, RigBlock.MOD, RigBlock.AMP, RigBlock.CAB, RigBlock.EQ, RigBlock.DELAY, RigBlock.REVERB),
            chainOrder(states).map { it.block },
        )
    }

    @Test
    fun `chain order falls back to declaration order when a position is unknown`() {
        val states = RigBlock.entries.map { RigBlockState(it, null, enabled = null, isPost = null) }
        assertEquals(RigBlock.entries.toList(), chainOrder(states).map { it.block })
    }

    @Test
    fun `short labels drop the block and model prefixes`() {
        assertEquals("Threshold", shortParameterLabel(ParameterId(2), "fallback"))
        assertEquals("Predelay", shortParameterLabel(ParameterId(40), "fallback"))
        assertEquals("Bass Freq", shortParameterLabel(ParameterId(12), "fallback"))
        assertEquals("Gain", shortParameterLabel(ParameterId(20), "fallback"))
        assertEquals("Cabinet Unknown", shortParameterLabel(ParameterId(23), "Cabinet Unknown"))
    }

    private fun rangeRow(index: Int, lo: Float, hi: Float, unit: String) =
        ParameterRow.Range(ParameterId(index), "r$index", "R$index", lo, lo..hi, "", 1, unit)

    @Test
    fun `log taper puts the geometric mean at 12 o'clock`() {
        val rate = 0.1f..10f
        assertEquals(1f, KnobTaper.LOGARITHMIC.valueAt(0.5f, rate), 1e-4f)
        assertEquals(0.5f, KnobTaper.LOGARITHMIC.fractionOf(1f, rate), 1e-4f)
        assertEquals(0.1f, KnobTaper.LOGARITHMIC.valueAt(0f, rate), 1e-6f)
        assertEquals(10f, KnobTaper.LOGARITHMIC.valueAt(1f, rate), 1e-4f)
        // Round trip across the travel.
        for (f in listOf(0.1f, 0.25f, 0.8f)) {
            assertEquals(f, KnobTaper.LOGARITHMIC.fractionOf(KnobTaper.LOGARITHMIC.valueAt(f, rate), rate), 1e-4f)
        }
    }

    @Test
    fun `linear taper is a straight map and clamps out-of-range values`() {
        assertEquals(5f, KnobTaper.LINEAR.valueAt(0.5f, 0f..10f), 1e-6f)
        assertEquals(1f, KnobTaper.LINEAR.fractionOf(42f, 0f..10f), 1e-6f)
        assertEquals(0f, KnobTaper.LINEAR.fractionOf(-1f, 0f..10f), 1e-6f)
    }

    @Test
    fun `log taper only for wide frequency ranges`() {
        assertEquals(KnobTaper.LOGARITHMIC, knobTaperFor(rangeRow(79, 0.1f, 10f, "Hz"))) // phaser rate
        assertEquals(KnobTaper.LOGARITHMIC, knobTaperFor(rangeRow(15, 150f, 5000f, "Hz"))) // EQ mid freq
        assertEquals(KnobTaper.LINEAR, knobTaperFor(rangeRow(17, 1000f, 4000f, "Hz"))) // EQ treble freq
        assertEquals(KnobTaper.LINEAR, knobTaperFor(rangeRow(20, 0f, 10f, ""))) // gain
        assertEquals(KnobTaper.LINEAR, knobTaperFor(rangeRow(99, 0f, 1000f, "ms"))) // delay time
    }
}
