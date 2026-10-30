package app.melova.data.model

/** A teacher as students see them. Lessons are bought one by one; following only keeps a teacher close. */
data class Teacher(
    val id: String,
    val name: String,
    val bio: String
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
    /** Where students pay for this teacher's lessons; empty until they set it. */
    val cardNumber: String,
    val cardHolder: String,
    val lessons: List<Lesson>
)

/** A card payment the student reported for a lesson, until the teacher confirms it. */
enum class LessonPurchaseStatus {
    PENDING,

    /** The teacher couldn't find the payment; the student can try again. */
    REJECTED;

    companion object {
        fun fromRaw(raw: String?): LessonPurchaseStatus? = when (raw) {
            "pending" -> PENDING
            "rejected" -> REJECTED
            else -> null
        }
    }
}

/** The teacher's card, where a lesson is paid for. */
data class LessonPayment(val cardNumber: String, val cardHolder: String)

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
    /** Whether this student has it; null when the server didn't say (no student account yet). */
    val isOwned: Boolean? = null,
    val purchaseStatus: LessonPurchaseStatus? = null,
    /** Null while the teacher has no card, and for a lesson that needs no paying. */
    val payment: LessonPayment? = null
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
