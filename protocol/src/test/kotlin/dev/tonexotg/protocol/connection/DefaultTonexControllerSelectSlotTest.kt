@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.tonexotg.protocol.connection

import dev.tonexotg.protocol.PresetIndex
import dev.tonexotg.protocol.PresetSlot
import dev.tonexotg.protocol.TonexError
import dev.tonexotg.protocol.TonexEvent
import dev.tonexotg.protocol.TonexResult
import dev.tonexotg.protocol.message.FirmwareCapabilities
import dev.tonexotg.protocol.state.StateBlobOffsets
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `selectSlot`: switch the active footswitch slot and the pedal's A/B-vs-stomp mode together, as
 * one read-modify-write of exactly two bytes (`END_CURRENT_SLOT`, `START_STOMP_MODE`). The
 * byte values asserted here are upstream's own literals (`usb_tonex_one.c`, pinned commit in
 * [StateBlobOffsets]): stomp mode `1` with slot C, `0` with A or B.
 */
class DefaultTonexControllerSelectSlotTest {

    @Test
    fun `A to C from A-B mode writes slot 2 and stomp mode 1, every other byte unchanged`() = runTest {
        // Same slot presets at connect and at the re-read: otherwise the re-read itself looks like
        // an external preset change and triggers a snapshot read after the write.
        val (controller, fake) = ready(activeSlot = PresetSlot.A, a = 5, b = 10, c = 15)

        val reread = plausibleBlob(activeSlot = PresetSlot.A, a = 5, b = 10, c = 15, stompMode = 0)
        val written = selectSlotAgainst(controller, fake, PresetSlot.C, reread)

        assertOnlyTheseBytesChanged(reread, written, slot = 2, stomp = 1)
    }

    @Test
    fun `C to B from stomp mode writes slot 1 and stomp mode 0`() = runTest {
        val (controller, fake) = ready(activeSlot = PresetSlot.C, a = 5, b = 10, c = 15)

        val reread = plausibleBlob(activeSlot = PresetSlot.C, a = 5, b = 10, c = 15, stompMode = 1)
        val written = selectSlotAgainst(controller, fake, PresetSlot.B, reread)

        assertOnlyTheseBytesChanged(reread, written, slot = 1, stomp = 0)
    }

    @Test
    fun `slot already active in the matching mode is a no-op with nothing written`() = runTest {
        val (controller, fake) = ready(activeSlot = PresetSlot.B)
        val writesBefore = fake.writtenMessages().size

        val deferred = async { controller.selectSlot(PresetSlot.B) }
        testScheduler.runCurrent()
        fake.emitMessage(stateUpdateMessage(plausibleBlob(activeSlot = PresetSlot.B, stompMode = 0)))
        testScheduler.runCurrent()
        assertIs<TonexResult.Success<Unit>>(deferred.await())

        // Exactly one more message: the mandatory state re-read request, and no state write.
        assertEquals(writesBefore + 1, fake.writtenMessages().size)
    }

    @Test
    fun `slot C already active but pedal in A-B mode still writes, to fix the mode`() = runTest {
        val (controller, fake) = ready(activeSlot = PresetSlot.C)

        val reread = plausibleBlob(activeSlot = PresetSlot.C, stompMode = 0)
        val written = selectSlotAgainst(controller, fake, PresetSlot.C, reread)

        assertOnlyTheseBytesChanged(reread, written, slot = 2, stomp = 1)
    }

    @Test
    fun `a stomp-mode byte that is not a 0-1 flag is refused and nothing is written`() = runTest {
        val (controller, fake) = ready(activeSlot = PresetSlot.A)
        val writesBefore = fake.writtenMessages().size

        val deferred = async { controller.selectSlot(PresetSlot.C) }
        testScheduler.runCurrent()
        fake.emitMessage(stateUpdateMessage(plausibleBlob(activeSlot = PresetSlot.A, stompMode = 7)))
        testScheduler.runCurrent()

        val failure = assertIs<TonexResult.Failure>(deferred.await())
        assertIs<TonexError.ImplausibleStateBlobShape>(failure.error)
        assertEquals(writesBefore + 1, fake.writtenMessages().size, "only the state re-read request, no write")
    }

    @Test
    fun `selectSlot before Ready fails with ProtocolStateViolation`() = runTest {
        val controller = DefaultTonexController(scope = backgroundScope, capabilities = FirmwareCapabilities.NONE_CONFIRMED)
        val failure = assertIs<TonexResult.Failure>(controller.selectSlot(PresetSlot.A))
        assertIs<TonexError.ProtocolStateViolation>(failure.error)
    }

    @Test
    fun `activeSlot is seeded at Ready and follows state pushes, null after disconnect`() = runTest {
        val (controller, fake) = ready(activeSlot = PresetSlot.B)
        assertEquals(PresetSlot.B, controller.activeSlot.value)

        fake.emitMessage(stateUpdateMessage(plausibleBlob(activeSlot = PresetSlot.C)))
        testScheduler.runCurrent()
        assertEquals(PresetSlot.C, controller.activeSlot.value)

        controller.disconnect()
        assertNull(controller.activeSlot.value)
    }

    @Test
    fun `own slot switch that moves the active preset is not reported as an external change`() = runTest {
        val (controller, fake) = ready(activeSlot = PresetSlot.A, a = 5, b = 10, c = 15)
        val externalChanges = mutableListOf<PresetIndex>()
        val eventsJob = launch(start = CoroutineStart.UNDISPATCHED) {
            controller.events.collect { if (it is TonexEvent.ExternalPresetChange) externalChanges += it.newIndex }
        }

        selectSlotAgainst(controller, fake, PresetSlot.C, plausibleBlob(activeSlot = PresetSlot.A, a = 5, b = 10, c = 15, stompMode = 0))
        // The pedal's confirming push: slot C (preset 15) now active.
        fake.emitMessage(stateUpdateMessage(plausibleBlob(activeSlot = PresetSlot.C, a = 5, b = 10, c = 15, stompMode = 1)))
        testScheduler.runCurrent()

        assertEquals(PresetIndex(15), controller.activePreset.value)
        assertEquals(PresetSlot.C, controller.activeSlot.value)
        assertTrue(externalChanges.isEmpty(), "the caller's own selectSlot must not surface as an external change")
        eventsJob.cancel()
    }

    // ---- helpers ------------------------------------------------------------------------------

    private suspend fun TestScope.ready(
        activeSlot: PresetSlot,
        a: Int = 0,
        b: Int = 1,
        c: Int = 2,
    ): Pair<DefaultTonexController, FakeTonexTransport> {
        val fake = FakeTonexTransport()
        val controller = DefaultTonexController(scope = backgroundScope, capabilities = FirmwareCapabilities.NONE_CONFIRMED)
        val connectDeferred = async { controller.connect(fake) }
        driveToReady(fake, activeSlot = activeSlot, a = a, b = b, c = c)
        assertIs<TonexResult.Success<Unit>>(connectDeferred.await())
        return controller to fake
    }

    /** Runs `selectSlot([slot])`, answers its re-read with [reread], and returns the written state payload. */
    private suspend fun TestScope.selectSlotAgainst(
        controller: DefaultTonexController,
        fake: FakeTonexTransport,
        slot: PresetSlot,
        reread: ByteArray,
    ): ByteArray {
        val deferred = async { controller.selectSlot(slot) }
        testScheduler.runCurrent()
        fake.emitMessage(stateUpdateMessage(reread))
        testScheduler.runCurrent()
        assertIs<TonexResult.Success<Unit>>(deferred.await())
        val written = fake.writtenMessages().last().payload
        assertEquals(reread.size, written.size)
        return written
    }

    private fun assertOnlyTheseBytesChanged(original: ByteArray, written: ByteArray, slot: Int, stomp: Int) {
        val slotIndex = original.size - StateBlobOffsets.END_CURRENT_SLOT
        val stompIndex = StateBlobOffsets.START_STOMP_MODE
        for (i in original.indices) {
            when (i) {
                slotIndex -> assertEquals(slot.toByte(), written[i], "active-slot byte")
                stompIndex -> assertEquals(stomp.toByte(), written[i], "stomp-mode byte")
                else -> assertEquals(original[i], written[i], "byte $i must be unchanged from the re-read blob")
            }
        }
    }
}
