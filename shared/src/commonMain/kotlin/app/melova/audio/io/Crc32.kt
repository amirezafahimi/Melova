package app.melova.audio.io

private val table = LongArray(256) { index ->
    var crc = index.toLong()
    repeat(8) { crc = if (crc and 1L != 0L) (crc ushr 1) xor 0xEDB88320L else crc ushr 1 }
    crc
}

/** The CRC-32 zlib gives (unsigned), so the server can tell a take it built is this one. */
fun crc32(bytes: ByteArray): Long {
    var crc = 0xFFFFFFFFL
    for (byte in bytes) crc = table[((crc xor byte.toLong()) and 0xFF).toInt()] xor (crc ushr 8)
    return crc xor 0xFFFFFFFFL
}
