package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ScoreMetricDto(
    @SerializedName("id") val id: String,
    @SerializedName("label") val label: String,
    @SerializedName("value") val value: Int,
    @SerializedName("hint") val hint: String? = null
)
