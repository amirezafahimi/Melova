package app.melova.audio.io

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlacEncoderTest {
    @Test
    fun aTakeComesBackSampleForSampleAndSmallerThanItsWav() {
        val random = Random(7)
        // A sung note with breath noise, silence, a full-scale click, and a short last block.
        val take = ShortArray(44_100 * 3 + 1_234) { i ->
            when {
                i < 4_000 -> 0
                i == 4_410 -> Short.MAX_VALUE.toInt()
                i == 4_411 -> Short.MIN_VALUE.toInt()
                else -> (sin(2 * PI * 330 * i / 44_100) * 9_000 + random.nextInt(-300, 300)).toInt()
            }.toShort()
        }

        val flac = FlacEncoder.encode(take, 44_100)

        val (samples, sampleRate) = decode(flac)
        assertEquals(44_100, sampleRate)
        assertContentEquals(take, samples)
        // Noise this loud keeps about 10 bits a sample; a real voice comes to about a third.
        assertTrue(flac.size < take.size * 2 * 3 / 4, "FLAC is ${flac.size} bytes")
    }

    @Test
    fun aTakeShorterThanOneBlockStillComesBack() {
        val take = ShortArray(3) { (it * 1_000 - 500).toShort() }
        assertContentEquals(take, decode(FlacEncoder.encode(take, 48_000)).first)
    }

    @Test
    fun aTakeOfMultiplesOfTwelveComesBackAndDropsItsTwoZeroBits() {
        val random = Random(3)
        val take = ShortArray(10_000) { i ->
            (((sin(2 * PI * 220 * i / 44_100) * 500).toInt() + random.nextInt(-20, 20)) * 12).toShort()
        }
        val plain = ShortArray(take.size) { (take[it] + if (it % 2 == 0) 1 else 0).toShort() }

        val flac = FlacEncoder.encode(take, 44_100)

        assertContentEquals(take, decode(flac).first)
        assertTrue(flac.size < FlacEncoder.encode(plain, 44_100).size * 9 / 10)
    }

    /** Reads back what [FlacEncoder] writes: fixed subframes with Rice-coded residuals. */
    private fun decode(flac: ByteArray): Pair<ShortArray, Int> {
        val bits = BitReader(flac)
        assertEquals("fLaC", flac.copyOfRange(0, 4).decodeToString())
        bits.skip(32 + 32 + 16 + 16 + 24 + 24)
        val sampleRate = bits.read(20)
        bits.skip(3 + 5 + 4)
        val total = bits.read(32)
        bits.skip(128)
        val out = ShortArray(total)
        var at = 0
        while (at < total) {
            val frameStart = bits.position / 8
            assertEquals(0x3FFE, bits.read(14))
            bits.skip(2 + 4 + 4 + 4 + 3 + 1)
            val lead = bits.read(8)
            bits.skip(8 * (leadingOnes(lead) - 1).coerceAtLeast(0))
            val count = bits.read(16) + 1
            bits.read(8)
            bits.skip(1)
            val order = bits.read(6) - 0b001000
            var wasted = 0
            if (bits.read(1) == 1) {
                wasted = 1
                while (bits.read(1) == 0) wasted++
            }
            val width = 16 - wasted
            val x = IntArray(count)
            for (i in 0 until order) x[i] = (bits.read(width) shl (32 - width)) shr (32 - width)
            assertEquals(0, bits.read(2))
            val partitionOrder = bits.read(4)
            val size = count shr partitionOrder
            var i = order
            for (partition in 0 until (1 shl partitionOrder)) {
                val parameter = bits.read(4)
                while (i < (partition + 1) * size) {
                    var quotient = 0
                    while (bits.read(1) == 0) quotient++
                    val u = (quotient shl parameter) or bits.read(parameter)
                    val e = (u ushr 1) xor -(u and 1)
                    x[i] = e + when (order) {
                        0 -> 0
                        1 -> x[i - 1]
                        2 -> 2 * x[i - 1] - x[i - 2]
                        3 -> 3 * x[i - 1] - 3 * x[i - 2] + x[i - 3]
                        else -> 4 * x[i - 1] - 6 * x[i - 2] + 4 * x[i - 3] - x[i - 4]
                    }
                    i++
                }
            }
            bits.alignToByte()
            bits.skip(16)
            assertTrue(bits.position / 8 > frameStart)
            for (sample in x) out[at++] = (sample shl wasted).toShort()
        }
        return out to sampleRate
    }

    private fun leadingOnes(byte: Int): Int {
        var count = 0
        while (count < 8 && (byte shl count) and 0x80 != 0) count++
        return count
    }

    private class BitReader(private val bytes: ByteArray) {
        var position = 0
            private set

        fun read(count: Int): Int {
            var value = 0
            repeat(count) {
                val bit = (bytes[position / 8].toInt() shr (7 - position % 8)) and 1
                value = (value shl 1) or bit
                position++
            }
            return value
        }

        fun skip(count: Int) {
            position += count
        }

        fun alignToByte() {
            position = (position + 7) / 8 * 8
        }
    }
}
