package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HealthResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("rmvpe_ready") val rmvpeReady: Boolean = false,
    @SerializedName("vosk_ready") val voskReady: Boolean = false,
    @SerializedName("teacher_wav_ready") val teacherWavReady: Boolean = false,
    @SerializedName("teacher_syllables_ready") val teacherSyllablesReady: Boolean = false
)
