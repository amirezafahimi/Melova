package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoreMetricDto(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String,
    @SerialName("value") val value: Int,
    @SerialName("hint") val hint: String? = null
)
