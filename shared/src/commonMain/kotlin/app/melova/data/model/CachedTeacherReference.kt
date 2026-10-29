package app.melova.data.model

import app.melova.data.local.TeacherReferenceCache
import app.melova.platform.LocalFile

data class CachedTeacherReference(
    val wavFile: LocalFile,
    val syllablesFile: LocalFile?,
    val sourceLabel: String = TeacherReferenceCache.BACKEND_SOURCE_LABEL
)

class TeacherReferenceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
