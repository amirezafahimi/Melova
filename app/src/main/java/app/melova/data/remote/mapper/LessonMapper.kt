package app.melova.data.remote.mapper

import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.remote.dto.LessonDto
import app.melova.data.remote.dto.LessonsResponseDto

fun LessonsResponseDto.toDomain(): List<Lesson> =
    lessons.orEmpty().mapNotNull { it.toDomain() }

private fun LessonDto.toDomain(): Lesson? {
    val lessonId = id?.takeIf { it.isNotBlank() } ?: return null
    return Lesson(
        id = lessonId,
        title = title.orEmpty(),
        subtitle = subtitle.orEmpty(),
        description = description.orEmpty(),
        teacher = teacher.orEmpty(),
        level = level.orEmpty(),
        hasIntro = hasIntro == true,
        introVersion = introVersion.orEmpty(),
        sections = sections.orEmpty().mapNotNull { dto ->
            val sectionId = dto.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            LessonSection(
                id = sectionId,
                title = dto.title.orEmpty(),
                description = dto.description.orEmpty(),
                isAvailable = dto.isAvailable == true,
                durationMs = dto.durationMs ?: 0L,
                audioVersion = dto.audioVersion.orEmpty()
            )
        }
    )
}
