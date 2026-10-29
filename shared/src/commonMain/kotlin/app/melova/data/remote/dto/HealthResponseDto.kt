package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponseDto(
    @SerialName("status") val status: String,
    @SerialName("rmvpe_ready") val rmvpeReady: Boolean = false,
    @SerialName("vosk_ready") val voskReady: Boolean = false,
    @SerialName("teacher_wav_ready") val teacherWavReady: Boolean = false,
    @SerialName("teacher_syllables_ready") val teacherSyllablesReady: Boolean = false
)
