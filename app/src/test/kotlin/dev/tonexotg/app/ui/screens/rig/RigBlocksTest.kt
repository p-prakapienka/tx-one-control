package dev.tonexotg.app.ui.screens.rig

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
}
