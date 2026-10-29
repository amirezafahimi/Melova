package app.melova.data.repository

import app.melova.data.local.TeacherSession
import app.melova.data.model.TeacherProfile
import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.TeacherApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.TeacherProfileDto
import app.melova.data.remote.mapper.toDomain
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Teacher panel calls; each returns the teacher's profile as the server now has it. */
class TeacherRepository(
    context: PlatformContext,
    private val api: TeacherApi = MelovaNetwork.teacherApi
) {
    private val session = TeacherSession(context)

    val isSignedIn: Boolean
        get() = session.token != null

    /** Checks [code] with the server and keeps it only when it is accepted. */
    suspend fun signIn(code: String): TeacherProfile {
        val profile = call { api.me(code.trim()) }
        session.token = code
        return profile
    }

    fun signOut() {
        session.token = null
    }

    suspend fun profile() = call { api.me(token()) }

    suspend fun updateProfile(name: String, bio: String) =
        call { api.updateMe(token(), mapOf("name" to name, "bio" to bio)) }

    suspend fun createLesson(fields: LessonFields) = call { api.createLesson(token(), fields.toMap()) }

    suspend fun updateLesson(lessonId: String, fields: LessonFields) =
        call { api.updateLesson(token(), lessonId, buildJsonObject { fields.toMap().forEach { (k, v) -> put(k, v) } }) }

    suspend fun setLessonPublished(lessonId: String, published: Boolean) =
        call { api.updateLesson(token(), lessonId, buildJsonObject { put("published", published) }) }

    /** An empty [bazaarSku] makes the lesson free; [includedReviews] come with it either way. */
    suspend fun setLessonSale(lessonId: String, bazaarSku: String, priceToman: Long, includedReviews: Int) =
        call {
            api.updateLesson(
                token(),
                lessonId,
                buildJsonObject {
                    put("bazaar_sku", JsonPrimitive(bazaarSku.trim()))
                    put("price_toman", priceToman)
                    put("included_reviews", includedReviews)
                }
            )
        }

    suspend fun deleteLesson(lessonId: String) = call { api.deleteLesson(token(), lessonId) }

    suspend fun uploadIntro(lessonId: String, audio: LocalFile) = call {
        api.uploadIntro(token(), lessonId, audio.toPart())
    }

    suspend fun addSection(lessonId: String, title: String, description: String, audio: LocalFile) = call {
        api.addSection(token(), lessonId, title, description, audio.toPart())
    }

    suspend fun deleteSection(lessonId: String, sectionId: String) =
        call { api.deleteSection(token(), lessonId, sectionId) }

    private fun token(): String = session.token ?: throw IOException("ابتدا با کد استاد وارد شوید.")

    /** A recorded take or a picked file; its name carries the extension the server checks. */
    private fun LocalFile.toPart(): FilePart {
        if (!exists()) throw IOException("فایل انتخاب‌شده باز نشد.")
        return FilePart("audio", name, mediaTypeFor(name), readBytes())
    }

    /** Runs a call and turns server errors into the Persian message the server sent. */
    private suspend fun call(block: suspend () -> TeacherProfileDto): TeacherProfile =
        apiCall { block() }.toDomain()
}

internal fun mediaTypeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "wav" -> "audio/wav"
    "m4a", "aac" -> "audio/mp4"
    "mp3" -> "audio/mpeg"
    "ogg" -> "audio/ogg"
    "webm" -> "audio/webm"
    else -> "audio/*"
}

data class LessonFields(
    val title: String,
    val subtitle: String,
    val level: String,
    val description: String
) {
    fun toMap() = mapOf(
        "title" to title,
        "subtitle" to subtitle,
        "level" to level,
        "description" to description
    )
}
