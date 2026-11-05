package app.melova.data.repository

import app.melova.data.local.TeacherSession
import app.melova.data.model.TeacherProfile
import app.melova.data.model.UploadedTake
import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.TeacherApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.TeacherProfileDto
import app.melova.data.remote.writeAtomically
import app.melova.data.remote.mapper.toDomain
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.cacheDir
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Teacher panel calls; each returns the teacher's profile as the server now has it. */
class TeacherRepository(
    private val context: PlatformContext,
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

    suspend fun updateProfile(name: String, bio: String, cardNumber: String, cardHolder: String) = call {
        api.updateMe(
            token(),
            mapOf("name" to name, "bio" to bio, "card_number" to cardNumber, "card_holder" to cardHolder)
        )
    }

    /** The server crops it to a square; read now, as the picked file is only temporary. */
    suspend fun setPhoto(photo: LocalFile) = call {
        api.setPhoto(token(), FilePart("photo", "photo", "application/octet-stream", photo.readBytes()))
    }

    suspend fun removePhoto() = call { api.deletePhoto(token()) }

    suspend fun createLesson(fields: LessonFields) = call { api.createLesson(token(), fields.toMap()) }

    suspend fun updateLesson(lessonId: String, fields: LessonFields) =
        call { api.updateLesson(token(), lessonId, buildJsonObject { fields.toMap().forEach { (k, v) -> put(k, v) } }) }

    suspend fun setLessonPublished(lessonId: String, published: Boolean) =
        call { api.updateLesson(token(), lessonId, buildJsonObject { put("published", published) }) }

    /** With no [bazaarSku] and no [priceToman], the lesson is free. */
    suspend fun setLessonSale(lessonId: String, bazaarSku: String, priceToman: Long) =
        call {
            api.updateLesson(
                token(),
                lessonId,
                buildJsonObject {
                    put("bazaar_sku", JsonPrimitive(bazaarSku.trim()))
                    put("price_toman", priceToman)
                }
            )
        }

    suspend fun deleteLesson(lessonId: String) = call { api.deleteLesson(token(), lessonId) }

    suspend fun uploadIntro(lessonId: String, audio: LocalFile, onProgress: (Float) -> Unit) = call {
        api.uploadIntro(token(), lessonId, audio.toPart(), onProgress)
    }

    /** Sends a take to be read before it becomes a section; [sectionId] for a new take of one. */
    suspend fun uploadTake(lessonId: String, sectionId: String?, audio: LocalFile, onProgress: (Float) -> Unit): UploadedTake =
        apiCall { api.uploadTake(token(), lessonId, sectionId, audio.toPart(), onProgress) }.toDomain()

    /** The take as the server stored it, for a picked file this device can't read. */
    suspend fun takeAudio(lessonId: String, takeId: String): LocalFile = apiCall {
        val dir = LocalFile(cacheDir(context), "take_preview").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        LocalFile(dir, "$takeId.wav").also { api.takeAudio(token(), lessonId, takeId).writeAtomically(it) }
    }

    suspend fun discardTake(lessonId: String, takeId: String) {
        apiCall { api.discardTake(token(), lessonId, takeId) }
    }

    suspend fun addSection(lessonId: String, title: String, description: String, takeId: String) = call {
        api.addSection(token(), lessonId, title, description, takeId)
    }

    suspend fun updateSection(lessonId: String, sectionId: String, title: String, description: String, takeId: String?) =
        call { api.updateSection(token(), lessonId, sectionId, title, description, takeId) }

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
