package app.melova.data.remote.mapper

import app.melova.audio.playback.ExpectedSegment
import app.melova.data.remote.dto.ExpectedSegmentDto
import app.melova.data.remote.MelovaNetwork
import kotlinx.serialization.builtins.ListSerializer

fun List<ExpectedSegment>?.toRequestJson(): String? {
    if (isNullOrEmpty()) return null
    return MelovaNetwork.json.encodeToString(ListSerializer(ExpectedSegmentDto.serializer()), map { it.toDto() })
}

fun List<ExpectedSegmentDto>.toDomain(): List<ExpectedSegment> =
    map { dto ->
        ExpectedSegment(
            startMs = dto.startMs,
            endMs = dto.endMs,
            frequencyHz = dto.frequencyHz,
            label = dto.label
        )
    }
