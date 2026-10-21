package app.melova.data.model

data class Course(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val level: String,
    /** False for a teacher's draft, which only that teacher sees. */
    val published: Boolean,
    val teacherId: String,
    val teacherName: String,
    val lessons: List<Lesson>
) {
    val sectionCount: Int
        get() = lessons.sumOf { lesson -> lesson.sections.size }
}

data class TeacherProfile(
    val id: String,
    val name: String,
    val bio: String,
    val courses: List<Course>
)

data class Lesson(
    val id: String,
    val courseId: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val teacher: String,
    val level: String,
    val hasIntro: Boolean,
    val introVersion: String,
    val sections: List<LessonSection>
) {
    fun section(sectionId: String): LessonSection? = sections.firstOrNull { it.id == sectionId }
}

data class LessonSection(
    val id: String,
    val title: String,
    val description: String,
    val isAvailable: Boolean,
    val durationMs: Long,
    val audioVersion: String
)
