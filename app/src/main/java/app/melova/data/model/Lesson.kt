package app.melova.data.model

data class Lesson(
    val id: String,
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
