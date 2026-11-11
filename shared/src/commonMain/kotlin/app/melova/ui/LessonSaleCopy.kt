package app.melova.ui

import app.melova.data.model.Lesson
import app.melova.data.model.LessonOffer
import app.melova.data.model.LessonTier
import app.melova.data.model.MessageState
import app.melova.data.model.formatEuro
import app.melova.platform.formatPersianInteger

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** Whole days until [untilMs], counting a part day as one; 0 once it has passed. */
internal fun daysLeft(untilMs: Long, nowMs: Long): Long =
    if (untilMs <= nowMs) 0L else (untilMs - nowMs + DAY_MS - 1) / DAY_MS

internal fun tierName(tier: LessonTier): String = when (tier) {
    LessonTier.FREE -> "درس رایگان"
    LessonTier.LESSON -> "درس"
    LessonTier.WITH_TEACHER -> "درس با استاد"
}

/** What a version gives, in one line: «همه‌ی بخش‌ها، تمرین بی‌حد و ۳ پیام به استاد». */
internal fun tierPromise(messages: Int): String =
    "همه‌ی بخش‌ها، تمرین بی‌حد و ${formatPersianInteger(messages.toLong())} پیام به استاد"

/** The rule the purchase card states before anyone pays. */
internal fun ownershipRule(offer: LessonOffer): String =
    "درس برای همیشه مال شماست. پیام‌ها به استاد تا ${formatPersianInteger(offer.messageMonths.toLong())} ماه بعد از خرید."

/**
 * The rule on an upgrade: the lesson is theirs already, so only the messages are news.
 * The server counts messages already sent against the new total, so it is a total, not "new".
 */
internal fun upgradeRule(offer: LessonOffer): String =
    "پیام‌هایتان به استاد روی هم به ${formatPersianInteger(offer.teacherMessages.toLong())} می‌رسد، تا ${formatPersianInteger(offer.messageMonths.toLong())} ماه بعد از ارتقا."

/** How many messages are left and for how long; null when the student doesn't have the lesson. */
internal fun messagesLine(offer: LessonOffer, nowMs: Long): String? = when (val state = offer.sendState(nowMs)) {
    MessageState.NotOwned -> null
    is MessageState.Left -> {
        val days = daysLeft(state.untilMs, nowMs)
        val count = formatPersianInteger(state.count.toLong())
        if (days <= 1L) "$count پیام به استاد مانده، تا امروز" else "$count پیام به استاد مانده، تا ${formatPersianInteger(days)} روز دیگر"
    }
    is MessageState.UsedUp -> "پیام‌های این درس به استاد تمام شده است."
    is MessageState.Expired -> "مهلت پیام‌های این درس به استاد تمام شده است."
}

/** The chip on a lesson card: «رایگان», «درس شما», or the plain version's price. */
internal fun saleChip(lesson: Lesson): String = when {
    lesson.isOwned == true -> "درس شما"
    lesson.isFree -> "رایگان"
    else -> "از ${formatEuro(lesson.priceCents)}"
}

/** An upgrade costs only the difference, and the card says so. */
internal fun upgradeLabel(offer: LessonOffer): String? {
    val price = offer.teacherPriceNowCents ?: return null
    return if (offer.ownedTier == LessonTier.LESSON) "ارتقا به «درس با استاد»، ${formatEuro(price)}" else "خرید «درس با استاد»، ${formatEuro(price)}"
}
