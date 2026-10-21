package app.melova.data.remote.mapper

import app.melova.data.model.Course
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.model.TeacherProfile
import app.melova.data.remote.dto.CourseDto
import app.melova.data.remote.dto.CoursesResponseDto
import app.melova.data.remote.dto.LessonDto
import app.melova.data.remote.dto.TeacherProfileDto

fun CoursesResponseDto.toDomain(): List<Course> =
    courses.orEmpty().mapNotNull { it.toDomain() }

fun TeacherProfileDto.toDomain(): TeacherProfile = TeacherProfile(
    id = id.orEmpty(),
    name = name.orEmpty(),
    bio = bio.orEmpty(),
    courses = courses.orEmpty().mapNotNull { it.toDomain() }
)

private fun CourseDto.toDomain(): Course? {
    val courseId = id?.takeIf { it.isNotBlank() } ?: return null
    return Course(
        id = courseId,
        title = title.orEmpty(),
        subtitle = subtitle.orEmpty(),
        description = description.orEmpty(),
        level = level.orEmpty(),
        published = published == true,
        teacherId = teacherId.orEmpty(),
        teacherName = teacherName.orEmpty(),
        lessons = lessons.orEmpty().mapNotNull { it.toDomain(courseId) }
    )
}

private fun LessonDto.toDomain(fallbackCourseId: String): Lesson? {
    val lessonId = id?.takeIf { it.isNotBlank() } ?: return null
    return Lesson(
        id = lessonId,
        courseId = courseId ?: fallbackCourseId,
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
