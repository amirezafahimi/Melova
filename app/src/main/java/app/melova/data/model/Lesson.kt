package app.melova.data.model

/** A teacher as students see them; students follow teachers to get their lessons. */
data class Teacher(
    val id: String,
    val name: String,
    val bio: String
)

/** Everything the student side shows: teachers with lessons, and those lessons. */
data class Catalog(
    val teachers: List<Teacher> = emptyList(),
    val lessons: List<Lesson> = emptyList()
)

/** A signed-in teacher's own view: their profile and every lesson, drafts included. */
data class TeacherProfile(
    val id: String,
    val name: String,
    val bio: String,
    val lessons: List<Lesson>
)

data class Lesson(
    val id: String,
    val teacherId: String,
    /** The teacher's display name. */
    val teacher: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val level: String,
    /** False for a teacher's draft, which only that teacher sees. */
    val published: Boolean,
    val hasIntro: Boolean,
    val introVersion: String,
    val sections: List<LessonSection>,
    /** A free lesson is open to everyone; a paid one to whoever bought it. */
    val isFree: Boolean = true,
    val priceToman: Long = 0L,
    /** Cafe Bazaar product id; empty for a free lesson. */
    val bazaarSku: String = "",
    /** Teacher reviews that come with the lesson. */
    val includedReviews: Int = 0,
    /** Whether this student bought it; null when the server didn't say (no student account yet). */
    val isOwned: Boolean? = null
) {
    val isOpen: Boolean
        get() = isFree || isOwned == true

    fun section(sectionId: String): LessonSection? = sections.firstOrNull { it.id == sectionId }

    val availableSections: Int
        get() = sections.count { it.isAvailable }
}

data class LessonSection(
    val id: String,
    val title: String,
    val description: String,
    val isAvailable: Boolean,
    val durationMs: Long,
    val audioVersion: String
)
