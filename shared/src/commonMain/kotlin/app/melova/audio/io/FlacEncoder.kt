package app.melova.audio.io

/**
 * Mono 16-bit PCM as FLAC. Lossless, so the server scores exactly the recorded samples, at about
 * a third of the WAV's size. Only what that needs: fixed predictors, Rice-coded residuals and
 * wasted bits, no MD5 (FLAC allows it to be left blank).
 */
internal object FlacEncoder {
    private const val BLOCK_SIZE = 4096
    private const val MAX_ORDER = 4
    private const val MAX_PARTITION_ORDER = 6
    // 15 is the escape code.
    private const val MAX_RICE_PARAMETER = 14

    fun encode(samples: ShortArray, sampleRate: Int): ByteArray {
        val out = BitWriter(samples.size + 1024)
        "fLaC".encodeToByteArray().forEach { out.bits(it.toInt(), 8) }
        writeStreamInfo(out, samples.size, sampleRate)
        val block = IntArray(BLOCK_SIZE)
        val residual = IntArray(BLOCK_SIZE)
        var start = 0
        var frame = 0
        while (start < samples.size) {
            val end = minOf(start + BLOCK_SIZE, samples.size)
            writeFrame(out, samples, start, end - start, frame++, block, residual)
            start = end
        }
        return out.toByteArray()
    }

    private fun writeStreamInfo(out: BitWriter, sampleCount: Int, sampleRate: Int) {
        out.bits(1, 1) // last metadata block
        out.bits(0, 7) // STREAMINFO
        out.bits(34, 24)
        out.bits(BLOCK_SIZE, 16)
        out.bits(BLOCK_SIZE, 16)
        out.bits(0, 24) // frame sizes unknown
        out.bits(0, 24)
        out.bits(sampleRate, 20)
        out.bits(0, 3) // one channel
        out.bits(15, 5) // 16 bits per sample
        out.bits(0, 4)
        out.bits(sampleCount, 32)
        repeat(16) { out.bits(0, 8) } // no MD5
    }

    private fun writeFrame(
        out: BitWriter,
        samples: ShortArray,
        start: Int,
        count: Int,
        frame: Int,
        block: IntArray,
        residual: IntArray
    ) {
        val frameStart = out.byteCount
        out.bits(0x3FFE, 14)
        out.bits(0, 1)
        out.bits(0, 1) // fixed block size
        out.bits(0b0111, 4) // block size follows the frame number
        out.bits(0, 4) // sample rate as in STREAMINFO
        out.bits(0, 4) // mono
        out.bits(0b100, 3) // 16 bits per sample
        out.bits(0, 1)
        writeUtf8Number(out, frame)
        out.bits(count - 1, 16)
        out.bits(out.crc8(frameStart), 8)

        // A take recorded with a gain can have every sample a multiple of 4 or more; FLAC drops
        // those low zero bits ("wasted bits"), which made one take a quarter smaller.
        var allBits = 0
        for (i in 0 until count) allBits = allBits or samples[start + i].toInt()
        val wasted = if (allBits == 0) 0 else allBits.countTrailingZeroBits()
        for (i in 0 until count) block[i] = samples[start + i].toInt() shr wasted

        val order = bestOrder(block, count, residual)
        out.bits(0, 1)
        out.bits(0b001000 or order, 6) // FIXED subframe
        if (wasted == 0) {
            out.bits(0, 1)
        } else {
            out.bits(1, 1)
            out.rice(wasted - 1, 0)
        }
        for (i in 0 until order) out.bits(block[i], 16 - wasted)
        writeResidual(out, residual, count, order)

        out.alignToByte()
        out.bits(out.crc16(frameStart), 16)
    }

    /** The fixed predictor order whose residual is smallest, with that residual left in [residual]. */
    private fun bestOrder(block: IntArray, count: Int, residual: IntArray): Int {
        var best = 0
        var bestSum = Long.MAX_VALUE
        for (order in 0..minOf(MAX_ORDER, count - 1)) {
            fillResidual(block, count, order, residual)
            var sum = 0L
            for (i in order until count) sum += kotlin.math.abs(residual[i])
            if (sum < bestSum) {
                bestSum = sum
                best = order
            }
        }
        fillResidual(block, count, best, residual)
        return best
    }

    private fun fillResidual(block: IntArray, count: Int, order: Int, residual: IntArray) {
        fun x(i: Int) = block[i]
        for (i in order until count) {
            residual[i] = when (order) {
                0 -> x(i)
                1 -> x(i) - x(i - 1)
                2 -> x(i) - 2 * x(i - 1) + x(i - 2)
                3 -> x(i) - 3 * x(i - 1) + 3 * x(i - 2) - x(i - 3)
                else -> x(i) - 4 * x(i - 1) + 6 * x(i - 2) - 4 * x(i - 3) + x(i - 4)
            }
        }
    }

    private fun writeResidual(out: BitWriter, residual: IntArray, count: Int, order: Int) {
        for (i in order until count) residual[i] = zigzag(residual[i])
        var bestPartitionOrder = 0
        var bestCost = Long.MAX_VALUE
        for (partitionOrder in 0..MAX_PARTITION_ORDER) {
            if (count % (1 shl partitionOrder) != 0 || (count shr partitionOrder) <= order) break
            var cost = 0L
            forEachPartition(count, order, partitionOrder) { from, to -> cost += riceCost(residual, from, to).second }
            if (cost < bestCost) {
                bestCost = cost
                bestPartitionOrder = partitionOrder
            }
        }
        out.bits(0, 2) // Rice coding, 4-bit parameters
        out.bits(bestPartitionOrder, 4)
        forEachPartition(count, order, bestPartitionOrder) { from, to ->
            val parameter = riceCost(residual, from, to).first
            out.bits(parameter, 4)
            for (i in from until to) out.rice(residual[i], parameter)
        }
    }

    /** The first partition is shorter by the warm-up samples, which aren't residual. */
    private inline fun forEachPartition(count: Int, order: Int, partitionOrder: Int, block: (Int, Int) -> Unit) {
        val size = count shr partitionOrder
        for (partition in 0 until (1 shl partitionOrder)) {
            val from = if (partition == 0) order else partition * size
            block(from, (partition + 1) * size)
        }
    }

    /** The Rice parameter for these values and about how many bits it writes them in. */
    private fun riceCost(values: IntArray, from: Int, to: Int): Pair<Int, Long> {
        var sum = 0L
        for (i in from until to) sum += values[i]
        val count = (to - from).toLong()
        var best = 0
        var bestBits = Long.MAX_VALUE
        for (parameter in 0..MAX_RICE_PARAMETER) {
            val bits = count * (parameter + 1) + (sum shr parameter)
            if (bits < bestBits) {
                bestBits = bits
                best = parameter
            }
        }
        return best to bestBits
    }

    private fun zigzag(value: Int) = (value shl 1) xor (value shr 31)

    private fun writeUtf8Number(out: BitWriter, value: Int) {
        when {
            value < 0x80 -> out.bits(value, 8)
            value < 0x800 -> {
                out.bits(0xC0 or (value shr 6), 8)
                out.bits(0x80 or (value and 0x3F), 8)
            }
            value < 0x10000 -> {
                out.bits(0xE0 or (value shr 12), 8)
                out.bits(0x80 or ((value shr 6) and 0x3F), 8)
                out.bits(0x80 or (value and 0x3F), 8)
            }
            else -> {
                out.bits(0xF0 or (value shr 18), 8)
                out.bits(0x80 or ((value shr 12) and 0x3F), 8)
                out.bits(0x80 or ((value shr 6) and 0x3F), 8)
                out.bits(0x80 or (value and 0x3F), 8)
            }
        }
    }
}

private class BitWriter(capacity: Int) {
    private var buffer = ByteArray(capacity)
    var byteCount = 0
        private set
    private var pending = 0
    private var pendingBits = 0

    /** The low [count] bits of [value], highest first. */
    fun bits(value: Int, count: Int) {
        for (shift in count - 1 downTo 0) bit((value ushr shift) and 1)
    }

    fun rice(value: Int, parameter: Int) {
        repeat(value ushr parameter) { bit(0) }
        bit(1)
        bits(value, parameter)
    }

    fun alignToByte() {
        while (pendingBits != 0) bit(0)
    }

    fun crc8(from: Int): Int {
        var crc = 0
        for (i in from until byteCount) {
            crc = crc xor (buffer[i].toInt() and 0xFF)
            repeat(8) { crc = if (crc and 0x80 != 0) ((crc shl 1) xor 0x07) and 0xFF else (crc shl 1) and 0xFF }
        }
        return crc
    }

    fun crc16(from: Int): Int {
        var crc = 0
        for (i in from until byteCount) {
            crc = crc xor ((buffer[i].toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x8005) and 0xFFFF else (crc shl 1) and 0xFFFF }
        }
        return crc
    }

    fun toByteArray(): ByteArray = buffer.copyOf(byteCount)

    private fun bit(value: Int) {
        pending = (pending shl 1) or value
        if (++pendingBits == 8) {
            if (byteCount == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
            buffer[byteCount++] = pending.toByte()
            pending = 0
            pendingBits = 0
        }
    }
}
