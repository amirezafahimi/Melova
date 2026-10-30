package app.melova.platform

/** Files live in memory for the page's lifetime; directories are implied by paths. */
internal actual object FileStore {
    private val files = mutableMapOf<String, ByteArray>()
    private val dirs = mutableSetOf<String>()

    actual fun exists(path: String): Boolean = path in files || path in dirs

    actual fun length(path: String): Long = files[path]?.size?.toLong() ?: 0L

    actual fun delete(path: String): Boolean = files.remove(path) != null || dirs.remove(path)

    actual fun mkdirs(path: String): Boolean = dirs.add(path)

    actual fun readBytes(path: String): ByteArray =
        files[path] ?: throw kotlinx.io.IOException("$path: no such file")

    actual fun writeBytes(path: String, bytes: ByteArray) {
        files[path] = bytes
    }

    actual fun list(path: String): List<String>? {
        if (path !in dirs && files.keys.none { it.startsWith("$path/") }) return null
        val prefix = "$path/"
        return (files.keys + dirs).filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix).substringBefore('/') }
            .distinct()
    }
}
