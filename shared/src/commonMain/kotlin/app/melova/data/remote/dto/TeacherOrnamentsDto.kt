package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeacherOrnamentsResponseDto(
    @SerialName("ornaments") val ornaments: List<TeacherOrnamentDto> = emptyList(),
    @SerialName("failure_reason") val failureReason: String? = null
)

@Serializable
data class TeacherOrnamentDto(
    @SerialName("kind") val kind: String = "",
    @SerialName("start_ms") val startMs: Long = 0L,
    @SerialName("end_ms") val endMs: Long = 0L
)

/** A take saved before it is confirmed, with the ornaments read from it. */
@Serializable
data class UploadedTakeDto(
    @SerialName("take_id") val takeId: String = "",
    @SerialName("ornaments") val ornaments: List<TeacherOrnamentDto> = emptyList(),
    @SerialName("failure_reason") val failureReason: String? = null
)
