package app.melova.data.repository

import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.apiCall
import app.melova.data.remote.writeAtomically
import app.melova.platform.LocalFile
import app.melova.platform.appContext
import app.melova.platform.filesDir
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Teachers' photos, fetched once per version and kept on disk, so a list of lesson cards
 * asks the server once per teacher rather than once per card.
 */
object TeacherPhotos {
    private val memory = mutableMapOf<String, ByteArray>()
    private val lock = Mutex()

    /** Null when the teacher has no photo, or it can't be had right now. */
    suspend fun photo(teacherId: String, version: Long?): ByteArray? {
        if (teacherId.isBlank() || version == null) return null
        val key = "$teacherId-$version"
        return lock.withLock {
            memory[key] ?: runCatching { fromDiskOrServer(teacherId, key) }.getOrNull()?.also { memory[key] = it }
        }
    }

    private suspend fun fromDiskOrServer(teacherId: String, key: String): ByteArray {
        val dir = LocalFile(filesDir(appContext), "teacher_photos").apply { mkdirs() }
        val target = LocalFile(dir, "$key.jpg")
        if (!target.exists() || target.length() == 0L) {
            // An older version of the same teacher's photo is no use any more.
            dir.listFiles()?.filter { it.name.startsWith("$teacherId-") }?.forEach { it.delete() }
            apiCall { MelovaNetwork.teacherApi.photo(teacherId) }.writeAtomically(target)
        }
        return target.readBytes()
    }
}
