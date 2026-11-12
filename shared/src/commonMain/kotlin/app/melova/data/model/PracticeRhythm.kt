package app.melova.data.model

/** This week's practice, Saturday first, against the number of days the student aims for. */
data class PracticeWeek(
    val goal: Int = DEFAULT_GOAL,
    /** Seven days, Saturday first: whether each had a counted take. */
    val days: List<Boolean> = List(7) { false },
    /** Today's place in [days]. */
    val today: Int = 0,
    val practised: Int = 0,
    /** Weeks that reached the goal, in all: a missed week never takes one away. */
    val fullWeeks: Int = 0,
    val reminders: Boolean = false
) {
    val isGoalReached: Boolean get() = practised >= goal

    companion object {
        const val DEFAULT_GOAL = 3
        const val MIN_GOAL = 1
        const val MAX_GOAL = 7
    }
}

/** One take in the practice journal; [id] is null when its voice is no longer kept. */
data class JournalTake(val id: String?, val score: Int, val createdAtMs: Long)

/** A section the student has sung: how it started and the best it has been. */
data class JournalSection(
    val lessonId: String,
    val sectionId: String,
    val lessonTitle: String,
    val sectionTitle: String,
    val first: JournalTake,
    val best: JournalTake,
    val takes: Int,
    val lastAtMs: Long,
    val stamped: Boolean
) {
    /** The first take was the best so far: there is nothing to compare yet. */
    val isSingleStep: Boolean get() = first.id == best.id && first.createdAtMs == best.createdAtMs
}

/** How far a section has come, shown at the end of its row. */
enum class SectionLevel(val label: String?) {
    NOT_SUNG(null),
    STARTED("شروع"),
    FAMILIAR("آشنا"),

    /** Only the teacher's «آفرین» gives this: a score alone can't say a section is mastered. */
    MASTERED("مسلط")
}

object PracticeLevels {
    /**
     * A section's best score from which it counts as «آشنا». The scorer can move a few points on
     * changes nobody hears, so this is a wide band rather than a target; retune it against the
     * calibration takes, here and nowhere else.
     */
    const val FAMILIAR_SCORE = 70

    /** How far a take has to beat the section's best before it is celebrated, for the same reason. */
    const val MIN_BEST_GAIN = 3

    fun level(best: Int?, stamped: Boolean): SectionLevel = when {
        stamped -> SectionLevel.MASTERED
        best == null -> SectionLevel.NOT_SUNG
        best >= FAMILIAR_SCORE -> SectionLevel.FAMILIAR
        else -> SectionLevel.STARTED
    }

    /** Whether a take beat the section's earlier best by enough to say so; a first take has nothing to beat. */
    fun isRealNewBest(score: Int?, previousBest: Int?): Boolean =
        score != null && previousBest != null && score - previousBest >= MIN_BEST_GAIN
}
