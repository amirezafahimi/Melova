package app.melova.data.repository

import android.content.Context
import app.melova.BuildConfig
import app.melova.data.local.TeacherReferenceCache
import app.melova.data.model.CachedTeacherReference
import app.melova.data.model.TeacherReferenceException
import app.melova.data.remote.datasource.MelovaRemoteDataSource
import app.melova.data.remote.datasource.MelovaRemoteDataSourceImpl
import app.melova.data.remote.writeAtomically
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RemoteTeacherReferenceRepository(
    private val remoteDataSource: MelovaRemoteDataSource = MelovaRemoteDataSourceImpl()
) : TeacherReferenceRepository {

    override suspend fun ensureCached(context: Context): CachedTeacherReference =
        withContext(Dispatchers.IO) {
            val wavFile = TeacherReferenceCache.wavFile(context)
            val syllablesFile = TeacherReferenceCache.syllablesFile(context)

            if (TeacherReferenceCache.isReady(context)) {
                TeacherReferenceCache.normalizeMetaIfNeeded(context)
                return@withContext toCachedReference(wavFile, syllablesFile)
            }

            try {
                remoteDataSource.downloadTeacherWav().use { body ->
                    body.writeAtomically(wavFile, context.cacheDir, minBytes = 45L)
                }
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
                    "دریافت نمونه استاد از سرور ممکن نشد. سرور را اجرا کنید (${BuildConfig.SCORING_API_BASE_URL}).",
                    error
                )
            }
        }

    private fun toCachedReference(wavFile: File, syllablesFile: File?): CachedTeacherReference {
        val sidecar = syllablesFile?.takeIf { it.exists() && it.length() > 0L }
        return CachedTeacherReference(wavFile = wavFile, syllablesFile = sidecar)
    }

    private suspend fun downloadSyllablesSidecar(context: Context): File? {
        val sidecar = TeacherReferenceCache.syllablesFile(context)
        return try {
            remoteDataSource.downloadTeacherSyllables().use { body ->
                body.writeAtomically(sidecar, context.cacheDir)
            }
            sidecar
        } catch (error: HttpException) {
            if (error.code() == 404) {
                sidecar.delete()
                null
            } else {
                throw error
            }
        }
    }
}
