package app.melova.data.model

/** A teacher as students see them. Lessons are bought one by one; following only keeps a teacher close. */
data class Teacher(
    val id: String,
    val name: String,
    val bio: String,
    /** Null while the teacher has no photo; changes whenever it does. */
    val photoVersion: Long? = null
)

/** Everything the student side shows: teachers with lessons, and those lessons. */
data class Catalog(
    val teachers: List<Teacher> = emptyList(),
    val lessons: List<Lesson> = emptyList(),
    /** Null when the catalog came without an account, as it can't say then. */
    val followedTeacherIds: Set<String>? = null
)

/** A signed-in teacher's own view: their profile and every lesson, drafts included. */
data class TeacherProfile(
    val id: String,
    val name: String,
    val bio: String,
    val lessons: List<Lesson>,
    val photoVersion: Long? = null
)

data class Lesson(
    val id: String,
    val teacherId: String,
    /** The teacher's display name. */
    val teacher: String,
    val teacherPhotoVersion: Long? = null,
    val title: String,
    val subtitle: String,
    val description: String,
    val level: String,
    /** False for a teacher's draft, which only that teacher sees. */
    val published: Boolean,
    val hasIntro: Boolean,
    val introVersion: String,
    val sections: List<LessonSection>,
    /** A free lesson is open to everyone; a paid one to whoever bought it. The first section is always open. */
    val isFree: Boolean = true,
    /** «درس», in euro cents; 0 for a free lesson. */
    val priceCents: Long = 0L,
    /** «درس با استاد», in euro cents; 0 when the teacher doesn't sell it. */
    val teacherPriceCents: Long = 0L,
    /** What the teacher entered, even while every lesson is made free for now. */
    val setPriceCents: Long = 0L,
    val setTeacherPriceCents: Long = 0L,
    /** Whether this student has it; null when the server didn't say (no student account yet). */
    val isOwned: Boolean? = null,
    /** This student's version, messages and prices now; null without an account. */
    val offer: LessonOffer? = null
) {
    val isOpen: Boolean
        get() = isFree || isOwned == true

    /** Sections past the first wait for the lesson to be bought. */
    fun isSectionOpen(sectionId: String): Boolean = isOpen || sections.firstOrNull()?.id == sectionId

    /** Sells «درس با استاد» too. */
    val sellsWithTeacher: Boolean
        get() = teacherPriceCents > 0

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
