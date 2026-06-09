package app.melova.data.remote.mapper

import app.melova.audio.playback.ExpectedSegment
import app.melova.data.remote.dto.ExpectedSegmentDto
import com.google.gson.Gson

private val gson = Gson()

fun List<ExpectedSegment>?.toRequestJson(): String? {
    if (isNullOrEmpty()) return null
    return gson.toJson(map { it.toDto() })
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
