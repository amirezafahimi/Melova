package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Whether the server took a piece of a take being sung. */
@Serializable
data class TakePieceAnswerDto(
    @SerialName("accepted") val accepted: Boolean = false
)
