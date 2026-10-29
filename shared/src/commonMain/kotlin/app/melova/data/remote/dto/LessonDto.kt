package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogResponseDto(
    @SerialName("teachers") val teachers: List<TeacherDto>?,
    @SerialName("lessons") val lessons: List<LessonDto>?
)

@Serializable
data class TeacherDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("bio") val bio: String?
)

@Serializable
data class TeacherProfileDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("bio") val bio: String?,
    @SerialName("lessons") val lessons: List<LessonDto>?
)

@Serializable
data class LessonDto(
    @SerialName("id") val id: String?,
    @SerialName("teacher_id") val teacherId: String?,
    @SerialName("teacher") val teacher: String?,
    @SerialName("title") val title: String?,
    @SerialName("subtitle") val subtitle: String?,
    @SerialName("description") val description: String?,
    @SerialName("level") val level: String?,
    @SerialName("published") val published: Boolean?,
    @SerialName("has_intro") val hasIntro: Boolean?,
    @SerialName("intro_version") val introVersion: String?,
    @SerialName("sections") val sections: List<LessonSectionDto>?,
    @SerialName("is_free") val isFree: Boolean? = null,
    @SerialName("price_toman") val priceToman: Long? = null,
    @SerialName("bazaar_sku") val bazaarSku: String? = null,
    @SerialName("included_reviews") val includedReviews: Int? = null,
    @SerialName("is_owned") val isOwned: Boolean? = null
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
