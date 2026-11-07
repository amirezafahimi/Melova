package app.melova.audio.io

import kotlin.test.Test
import kotlin.test.assertEquals

class Crc32Test {
    @Test
    fun theChecksumIsTheOneTheServerWorksOut() {
        // zlib.crc32 on the server.
        assertEquals(0L, crc32(ByteArray(0)))
        assertEquals(0xCBF43926L, crc32("123456789".encodeToByteArray()))
        assertEquals(0xD202EF8DL, crc32(byteArrayOf(0)))
        // Samples 32767 and -32768: bytes over 127 too.
        assertEquals(0x4D5475A0L, crc32(byteArrayOf(-1, 0x7F, 0, -128)))
    }
}
