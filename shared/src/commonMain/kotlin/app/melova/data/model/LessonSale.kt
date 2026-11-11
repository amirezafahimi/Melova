package app.melova.data.model

import app.melova.platform.persianDigits

/**
 * The two versions a lesson sells in. Both open every section for good and never limit
 * practice; they differ only in how many takes the student can send the teacher.
 */
enum class LessonTier(val apiName: String) {
    /** A free lesson the student added. */
    FREE("free"),

    /** «درس»: a few messages to the teacher. */
    LESSON("lesson"),

    /** «درس با استاد»: many more messages. */
    WITH_TEACHER("teacher");

    companion object {
        fun fromRaw(raw: String?): LessonTier? = entries.firstOrNull { it.apiName == raw }
    }
}

/**
 * A lesson's selling side as this student sees it. Prices "now" are what each version
 * would cost them today (an upgrade costs the difference); null when it isn't for sale to them.
 */
data class LessonOffer(
    val ownedTier: LessonTier? = null,
    val messagesLeft: Int = 0,
    val messagesTotal: Int = 0,
    /** When the messages run out; null before any purchase. */
    val messagesUntilMs: Long? = null,
    val lessonPriceNowCents: Long? = null,
    val teacherPriceNowCents: Long? = null,
    val lessonMessages: Int = 3,
    val teacherMessages: Int = 20,
    val messageDays: Int = 90,
    val answerDays: Int = 3
) {
    val owned: Boolean
        get() = ownedTier != null

    val canUpgrade: Boolean
        get() = ownedTier != LessonTier.WITH_TEACHER && teacherPriceNowCents != null

    /** Whole months, as the purchase card says it («۳ ماه»). */
    val messageMonths: Int
        get() = (messageDays + 15) / 30

    /** What the send button can do, at [nowMs]. */
    fun sendState(nowMs: Long): MessageState = when {
        ownedTier == null -> MessageState.NotOwned
        messagesLeft > 0 && (messagesUntilMs ?: 0L) > nowMs -> MessageState.Left(messagesLeft, messagesUntilMs ?: 0L)
        messagesTotal > 0 && (messagesUntilMs ?: 0L) <= nowMs -> MessageState.Expired(canUpgrade)
        else -> MessageState.UsedUp(canUpgrade)
    }
}

/** Where the student stands on sending a take to the teacher. */
sealed interface MessageState {
    /** They don't have the lesson; buying it (or adding it, when free) comes first. */
    data object NotOwned : MessageState

    data class Left(val count: Int, val untilMs: Long) : MessageState

    /** Every message was sent. [canUpgrade]: «درس با استاد» is on offer to them. */
    data class UsedUp(val canUpgrade: Boolean) : MessageState

    /** The three months passed with messages unsent. */
    data class Expired(val canUpgrade: Boolean) : MessageState
}

/** The student just bought a version; [redirectUrl] when the provider wants them on its page. */
data class LessonCheckout(
    val paid: Boolean,
    val redirectUrl: String?,
    val offer: LessonOffer,
    val account: StudentAccount
)

/** One sale in the teacher's ledger. */
data class TeacherSale(
    val id: String,
    val lessonId: String,
    val tier: LessonTier,
    /** Bought as the difference on top of the plain version. */
    val upgrade: Boolean,
    val grossCents: Long,
    val teacherCents: Long,
    val studentName: String,
    val createdAtMs: Long
)

data class EarningsTotal(
    val count: Int = 0,
    val grossCents: Long = 0L,
    val teacherCents: Long = 0L,
    val melovaCents: Long = 0L
)

/** What the teacher earned from their lessons; recorded only, nothing is paid out yet. */
data class TeacherEarnings(
    val cutPercent: Int,
    val total: EarningsTotal,
    val thisMonth: EarningsTotal,
    val sales: List<TeacherSale>
)

/** Suggested prices for a new sale, in euro cents. */
const val SUGGESTED_LESSON_PRICE_CENTS = 900L
const val SUGGESTED_TEACHER_PRICE_CENTS = 3500L
const val MAX_PRICE_CENTS = 100_000L

/**
 * Euro cents as the app shows them, in Persian digits: «۹ یورو», «۲۴٫۵۰ یورو».
 * Whole euros drop the cents.
 */
fun formatEuro(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val euros = abs / 100
    val rest = abs % 100
    val text = if (rest == 0L) "$euros" else "$euros٫${rest.toString().padStart(2, '0')}"
    return "$sign${persianDigits(text)} یورو"
}

/**
 * What a teacher typed as a price («۹», «9.5», «۲۴٫۵۰») in cents; null when it isn't a price.
 * Blank is 0, which means "not sold".
 */
fun parseEuroCents(text: String): Long? {
    val latin = text.trim().map { char ->
        when (char) {
            in '۰'..'۹' -> '0' + (char - '۰')
            in '٠'..'٩' -> '0' + (char - '٠')
            '٫', ',', '/' -> '.'
            else -> char
        }
    }.joinToString("").trim().removeSuffix("€").removeSuffix("یورو").trim()
    if (latin.isEmpty()) return 0L
    val parts = latin.split('.')
    if (parts.size > 2 || parts.any { part -> part.any { !it.isDigit() } } || parts[0].isEmpty()) return null
    val whole = parts[0].toLongOrNull() ?: return null
    val cents = parts.getOrNull(1)?.let { if (it.length > 2) return null else it.padEnd(2, '0').toLong() } ?: 0L
    return whole * 100 + cents
}

/** A price as the teacher types it back in: «9» or «24.5». */
fun euroInput(cents: Long): String =
    if (cents % 100 == 0L) "${cents / 100}" else "${cents / 100}.${(cents % 100).toString().padStart(2, '0').trimEnd('0')}"

/** Why the teacher's prices can't be saved; null when they can. */
fun salePriceProblem(lessonCents: Long?, teacherCents: Long?): String? = when {
    lessonCents == null || teacherCents == null -> "قیمت را فقط با عدد بنویسید، مثل ۹ یا ۹٫۵۰."
    lessonCents > MAX_PRICE_CENTS || teacherCents > MAX_PRICE_CENTS -> "قیمت بیشتر از ۱۰۰۰ یورو نمی‌شود."
    teacherCents in 1..lessonCents -> "قیمت «درس با استاد» باید از قیمت «درس» بیشتر باشد."
    else -> null
}
