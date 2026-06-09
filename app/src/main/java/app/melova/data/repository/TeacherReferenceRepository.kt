package app.melova.data.repository

import android.content.Context
import app.melova.data.model.CachedTeacherReference

interface TeacherReferenceRepository {
    suspend fun ensureCached(context: Context): CachedTeacherReference
}
