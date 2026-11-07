package app.melova.data.repository

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest

class TakeStreamTest {
    private class Sent(val offset: Int, val samples: ShortArray, val ended: Boolean)

    @Test
    fun theTakeGoesASecondAtATimeAndAllOfItArrives() = runTest {
        val sent = mutableListOf<Sent>()
        val stream = TakeStream("take", backgroundScope, send = { offset, samples, rate, ended ->
            assertEquals(1_000, rate)
            sent += Sent(offset, samples, ended)
            true
        }, drop = {})
        val take = ShortArray(3_700) { (it % 300).toShort() }
        // As the recorder hands it over: in pieces of any size.
        var at = 0
        for (size in listOf(400, 900, 1, 1_299, 1_100)) {
            stream.add(take.copyOfRange(at, at + size), 1_000)
            at += size
        }
        stream.end()

        val streamed = stream.streamed()
        assertEquals("take", streamed?.id)
        assertEquals(true, streamed?.whole)
        assertEquals(listOf(0, 1_000, 2_000, 3_000), sent.map { it.offset })
        assertEquals(listOf(1_000, 1_000, 1_000, 700), sent.map { it.samples.size })
        assertEquals(listOf(false, false, false, true), sent.map { it.ended })
        assertContentEquals(take, sent.flatMap { it.samples.toList() }.toShortArray())
    }

    @Test
    fun oncePieceIsTurnedAwayNoMoreGoAndTheTakeIsSentWithoutIt() = runTest {
        val offsets = mutableListOf<Int>()
        val stream = TakeStream("take", backgroundScope, send = { offset, _, _, _ ->
            offsets += offset
            offset == 0
        }, drop = {})
        stream.add(ShortArray(3_500), 1_000)
        stream.end()

        assertNull(stream.streamed())
        assertEquals(listOf(0, 1_000), offsets)
    }

    @Test
    fun aPieceThatCannotBeSentCountsAsTurnedAway() = runTest {
        val stream = TakeStream("take", backgroundScope, send = { _, _, _, _ -> error("offline") }, drop = {})
        stream.add(ShortArray(1_200), 1_000)
        stream.end()

        assertNull(stream.streamed())
    }

    @Test
    fun aSlowLineIsWaitedForWhileItsPiecesKeepGoing() = runTest {
        // Each second of the take takes ten to send: slow, but moving.
        val stream = TakeStream("take", backgroundScope, send = { _, _, _, _ -> delay(10_000); true }, drop = {})
        stream.add(ShortArray(3_500), 1_000)
        stream.end()

        assertEquals(true, stream.streamed()?.whole)
        assertEquals(40_000, currentTime)
    }

    @Test
    fun piecesThatStopMovingAreLeftAndTheTakeGoesItself() = runTest {
        val stream = TakeStream("take", backgroundScope, send = { offset, _, _, _ ->
            if (offset > 0) awaitCancellation()
            true
        }, drop = {})
        stream.add(ShortArray(3_000), 1_000)
        stream.end()

        val streamed = stream.streamed()
        assertEquals("take", streamed?.id)
        assertEquals(false, streamed?.whole)
        assertEquals(15_000, currentTime)
    }

    @Test
    fun aTakeThrownAwayIsDroppedOnTheServer() = runTest {
        var dropped = false
        // Its own scope: the drop runs after the sending stops.
        val stream = TakeStream("take", this, send = { _, _, _, _ -> true }, drop = { dropped = true })
        stream.add(ShortArray(500), 1_000)
        stream.discard()
        testScheduler.advanceUntilIdle()

        assertTrue(dropped)
    }

    @Test
    fun anIdIsLongEnoughForTheServer() {
        val id = TakeStream.newId()
        assertTrue(Regex("^[0-9a-f]{32}$").matches(id), id)
    }
}
