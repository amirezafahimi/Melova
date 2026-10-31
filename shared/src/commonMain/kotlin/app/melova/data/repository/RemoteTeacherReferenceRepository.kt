package app.melova.data.repository

import app.melova.data.local.TeacherReferenceCache
import app.melova.data.model.CachedTeacherReference
import app.melova.data.model.TeacherReferenceException
import app.melova.data.remote.datasource.MelovaRemoteDataSource
import app.melova.data.remote.datasource.MelovaRemoteDataSourceImpl
import app.melova.data.remote.writeAtomically
import kotlinx.coroutines.withContext
import app.melova.data.remote.ApiException
import app.melova.data.remote.apiCall
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.ioDispatcher

class RemoteTeacherReferenceRepository(
    private val remoteDataSource: MelovaRemoteDataSource = MelovaRemoteDataSourceImpl()
) : TeacherReferenceRepository {

    override suspend fun ensureCached(context: PlatformContext): CachedTeacherReference =
        withContext(ioDispatcher) {
            val wavFile = TeacherReferenceCache.wavFile(context)
            val syllablesFile = TeacherReferenceCache.syllablesFile(context)

            if (TeacherReferenceCache.isReady(context)) {
                TeacherReferenceCache.normalizeMetaIfNeeded(context)
                return@withContext toCachedReference(wavFile, syllablesFile)
            }

            try {
                remoteDataSource.downloadTeacherWav().writeAtomically(wavFile, minBytes = 45L)
                require(TeacherReferenceCache.isValidWav(wavFile)) { "invalid teacher wav" }
                TeacherReferenceCache.normalizePlaybackWav(context)
                TeacherReferenceCache.metaFile(context).writeText(TeacherReferenceCache.BACKEND_META)

                val sidecar = downloadSyllablesSidecar(context)
                toCachedReference(wavFile, sidecar ?: syllablesFile.takeIf { it.exists() })
            } catch (error: Exception) {
                wavFile.delete()
                TeacherReferenceCache.metaFile(context).delete()
                syllablesFile.delete()
                throw TeacherReferenceException(
                    "دریافت نمونه استاد ممکن نشد؛ دوباره تلاش کنید.",
                    error
                )
            }
        }

    private fun toCachedReference(wavFile: LocalFile, syllablesFile: LocalFile?): CachedTeacherReference {
        val sidecar = syllablesFile?.takeIf { it.exists() && it.length() > 0L }
        return CachedTeacherReference(wavFile = wavFile, syllablesFile = sidecar)
    }

    private suspend fun downloadSyllablesSidecar(context: PlatformContext): LocalFile? {
        val sidecar = TeacherReferenceCache.syllablesFile(context)
        return try {
            apiCall { remoteDataSource.downloadTeacherSyllables() }.writeAtomically(sidecar)
            sidecar
        } catch (error: ApiException) {
            if (error.code == 404) {
                sidecar.delete()
                null
            } else {
                throw error
            }
        }
    }
}
