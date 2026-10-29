package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TeacherOrnamentsResponseDto(
    @SerializedName("ornaments") val ornaments: List<TeacherOrnamentDto> = emptyList(),
    @SerializedName("failure_reason") val failureReason: String? = null
)

data class TeacherOrnamentDto(
    @SerializedName("kind") val kind: String = "",
    @SerializedName("start_ms") val startMs: Long = 0L,
    @SerializedName("end_ms") val endMs: Long = 0L
)
