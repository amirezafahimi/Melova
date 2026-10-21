package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CatalogResponseDto(
    @SerializedName("teachers") val teachers: List<TeacherDto>?,
    @SerializedName("lessons") val lessons: List<LessonDto>?
)

data class TeacherDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("bio") val bio: String?
)

data class TeacherProfileDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("lessons") val lessons: List<LessonDto>?
)

data class LessonDto(
    @SerializedName("id") val id: String?,
    @SerializedName("teacher_id") val teacherId: String?,
    @SerializedName("teacher") val teacher: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("level") val level: String?,
    @SerializedName("published") val published: Boolean?,
    @SerializedName("has_intro") val hasIntro: Boolean?,
    @SerializedName("intro_version") val introVersion: String?,
    @SerializedName("sections") val sections: List<LessonSectionDto>?
)

data class LessonSectionDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("is_available") val isAvailable: Boolean?,
    @SerializedName("duration_ms") val durationMs: Long?,
    @SerializedName("audio_version") val audioVersion: String?
)
