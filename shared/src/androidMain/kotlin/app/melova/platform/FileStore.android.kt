package app.melova.platform

import java.io.File

internal actual object FileStore {
    actual fun exists(path: String): Boolean = File(path).exists()
    actual fun length(path: String): Long = File(path).length()
    actual fun delete(path: String): Boolean = File(path).delete()
    actual fun mkdirs(path: String): Boolean = File(path).mkdirs()
    actual fun readBytes(path: String): ByteArray = File(path).readBytes()
    actual fun writeBytes(path: String, bytes: ByteArray) = File(path).writeBytes(bytes)
    actual fun list(path: String): List<String>? = File(path).list()?.toList()
}
