package app.melova.data.model

import app.melova.data.local.TeacherReferenceCache
import java.io.File

data class CachedTeacherReference(
    val wavFile: File,
    val syllablesFile: File?,
    val sourceLabel: String = TeacherReferenceCache.BACKEND_SOURCE_LABEL
)

class TeacherReferenceException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
