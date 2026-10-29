package app.melova.platform

/**
 * A file in the app's own storage. On Android it is a real file; in the browser it lives in
 * memory for the session, which is enough for downloads and takes that are re-fetched later.
 */
class LocalFile(val path: String) {
    constructor(parent: LocalFile, name: String) : this(parent.path.trimEnd('/') + "/" + name)

    val name: String
        get() = path.substringAfterLast('/')

    val nameWithoutExtension: String
        get() = name.substringBeforeLast('.')

    val extension: String
        get() = name.substringAfterLast('.', "")

    val parentFile: LocalFile?
        get() = path.substringBeforeLast('/', "").takeIf { it.isNotEmpty() }?.let(::LocalFile)

    fun exists(): Boolean = FileStore.exists(path)

    fun length(): Long = FileStore.length(path)

    fun delete(): Boolean = FileStore.delete(path)

    fun mkdirs(): Boolean = FileStore.mkdirs(path)

    fun readBytes(): ByteArray = FileStore.readBytes(path)

    fun writeBytes(bytes: ByteArray) = FileStore.writeBytes(path, bytes)

    fun readText(): String = readBytes().decodeToString()

    fun writeText(text: String) = writeBytes(text.encodeToByteArray())

    fun copyTo(target: LocalFile, overwrite: Boolean = false): LocalFile {
        check(overwrite || !target.exists()) { "${target.path} already exists" }
        target.writeBytes(readBytes())
        return target
    }

    /** Files directly inside this directory, or null when it is not one. */
    fun listFiles(filter: ((LocalFile) -> Boolean)? = null): List<LocalFile>? =
        FileStore.list(path)?.map { LocalFile(this, it) }?.let { files ->
            if (filter == null) files else files.filter(filter)
        }

    override fun equals(other: Any?): Boolean = other is LocalFile && other.path == path

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String = path
}

internal expect object FileStore {
    fun exists(path: String): Boolean
    fun length(path: String): Long
    fun delete(path: String): Boolean
    fun mkdirs(path: String): Boolean
    fun readBytes(path: String): ByteArray
    fun writeBytes(path: String, bytes: ByteArray)
    fun list(path: String): List<String>?
}
