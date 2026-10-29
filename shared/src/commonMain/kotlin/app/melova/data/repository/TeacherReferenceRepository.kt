package app.melova.data.repository

import app.melova.data.model.CachedTeacherReference
import app.melova.platform.PlatformContext

interface TeacherReferenceRepository {
    suspend fun ensureCached(context: PlatformContext): CachedTeacherReference
}
