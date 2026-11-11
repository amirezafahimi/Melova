package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogResponseDto(
    @SerialName("teachers") val teachers: List<TeacherDto>?,
    @SerialName("lessons") val lessons: List<LessonDto>?,
    @SerialName("followed_teachers") val followedTeachers: List<String>? = null
)

@Serializable
data class TeacherDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("bio") val bio: String?,
    @SerialName("photo_version") val photoVersion: Long? = null
)

@Serializable
data class TeacherProfileDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("bio") val bio: String?,
    @SerialName("photo_version") val photoVersion: Long? = null,
    @SerialName("lessons") val lessons: List<LessonDto>?
)

@Serializable
data class LessonDto(
    @SerialName("id") val id: String?,
    @SerialName("teacher_id") val teacherId: String?,
    @SerialName("teacher") val teacher: String?,
    @SerialName("teacher_photo_version") val teacherPhotoVersion: Long? = null,
    @SerialName("title") val title: String?,
    @SerialName("subtitle") val subtitle: String?,
    @SerialName("description") val description: String?,
    @SerialName("level") val level: String?,
    @SerialName("published") val published: Boolean?,
    @SerialName("has_intro") val hasIntro: Boolean?,
    @SerialName("intro_version") val introVersion: String?,
    @SerialName("sections") val sections: List<LessonSectionDto>?,
    @SerialName("is_free") val isFree: Boolean? = null,
    @SerialName("price_cents") val priceCents: Long? = null,
    @SerialName("teacher_price_cents") val teacherPriceCents: Long? = null,
    @SerialName("set_price_cents") val setPriceCents: Long? = null,
    @SerialName("set_teacher_price_cents") val setTeacherPriceCents: Long? = null,
    @SerialName("is_owned") val isOwned: Boolean? = null,
    @SerialName("offer") val offer: LessonOfferDto? = null
)

/** A lesson's selling side for one student; see the server's ``lesson_offer``. */
@Serializable
data class LessonOfferDto(
    @SerialName("owned_tier") val ownedTier: String? = null,
    @SerialName("messages_left") val messagesLeft: Int? = null,
    @SerialName("messages_total") val messagesTotal: Int? = null,
    @SerialName("messages_until") val messagesUntil: Double? = null,
    @SerialName("lesson_price_now_cents") val lessonPriceNowCents: Long? = null,
    @SerialName("teacher_price_now_cents") val teacherPriceNowCents: Long? = null,
    @SerialName("lesson_messages") val lessonMessages: Int? = null,
    @SerialName("teacher_messages") val teacherMessages: Int? = null,
    @SerialName("message_days") val messageDays: Int? = null,
    @SerialName("answer_days") val answerDays: Int? = null
)

@Serializable
data class LessonSectionDto(
    @SerialName("id") val id: String?,
    @SerialName("title") val title: String?,
    @SerialName("description") val description: String?,
    @SerialName("is_available") val isAvailable: Boolean?,
    @SerialName("duration_ms") val durationMs: Long?,
    @SerialName("audio_version") val audioVersion: String?
)
