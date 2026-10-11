package dev.tonexotg.app.ui.screens.rig

import dev.tonexotg.app.ui.screens.presets.PresetListItem
import dev.tonexotg.app.ui.screens.presets.PresetListUiState
import dev.tonexotg.protocol.PresetIndex
import dev.tonexotg.protocol.PresetSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which presets the Stage lets you load, and where the LCD arrows land: the pedal's own rule. */
class SlotSelectionTest {

    /** 20 presets; A holds 3, B holds 7, C holds 12; [active] is the active preset. */
    private fun state(
        activeSlot: PresetSlot?,
        active: Int,
        isLive: Boolean = true,
        slots: Map<PresetSlot, Int> = mapOf(PresetSlot.A to 3, PresetSlot.B to 7, PresetSlot.C to 12),
    ) = PresetListUiState(
        items = (0..19).map { i ->
            PresetListItem(
                index = PresetIndex(i),
                pedalName = "P$i",
                localAlias = null,
                displayName = "P$i",
                isActive = i == active,
                assignedSlots = slots.filterValues { it == i }.keys,
            )
        },
        isLive = isLive,
        activeSlot = activeSlot,
    )

    @Test
    fun `presets on the other two slots are not selectable, 18 of 20 are`() {
        val s = state(activeSlot = PresetSlot.C, active = 12)
        assertFalse(s.isSelectable(s.items[3])) // on A
        assertFalse(s.isSelectable(s.items[7])) // on B
        assertTrue(s.isSelectable(s.items[12])) // on C, the active slot
        assertTrue(s.isSelectable(s.items[0])) // on no slot
        assertEquals(18, s.items.count { s.isSelectable(it) })
    }

    @Test
    fun `nothing is selectable without a live pedal or a known active slot`() {
        val offline = state(activeSlot = PresetSlot.A, active = 3, isLive = false)
        assertFalse(offline.isSelectable(offline.items[0]))
        val unknownSlot = state(activeSlot = null, active = 3)
        assertFalse(unknownSlot.isSelectable(unknownSlot.items[0]))
    }

    @Test
    fun `arrows skip presets held by the other slots`() {
        // Active slot A on preset 3. Going up from 6 must skip 7 (on B); down from 13 skips 12 (on C).
        val fromSix = state(activeSlot = PresetSlot.A, active = 6, slots = mapOf(PresetSlot.A to 6, PresetSlot.B to 7, PresetSlot.C to 12))
        assertEquals(PresetIndex(8), nextSelectablePreset(fromSix, +1))
        val fromThirteen = state(activeSlot = PresetSlot.A, active = 13, slots = mapOf(PresetSlot.A to 13, PresetSlot.B to 7, PresetSlot.C to 12))
        assertEquals(PresetIndex(11), nextSelectablePreset(fromThirteen, -1))
    }

    @Test
    fun `arrows wrap around and skip held presets at the ends`() {
        // Active on 19 in slot B; preset 0 is on A, so +1 wraps past it to 1.
        val s = state(activeSlot = PresetSlot.B, active = 19, slots = mapOf(PresetSlot.A to 0, PresetSlot.B to 19, PresetSlot.C to 12))
        assertEquals(PresetIndex(1), nextSelectablePreset(s, +1))
    }

    @Test
    fun `no arrow target offline`() {
        assertNull(nextSelectablePreset(state(activeSlot = PresetSlot.A, active = 3, isLive = false), +1))
    }
}
