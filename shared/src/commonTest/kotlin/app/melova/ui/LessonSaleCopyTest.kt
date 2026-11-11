package app.melova.ui

import app.melova.data.model.Lesson
import app.melova.data.model.LessonOffer
import app.melova.data.model.LessonTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LessonSaleCopyTest {
    private val now = 1_800_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private fun lesson(isFree: Boolean = false, isOwned: Boolean? = null) = Lesson(
        id = "l1", teacherId = "t1", teacher = "", title = "", subtitle = "", description = "", level = "",
        published = true, hasIntro = false, introVersion = "", sections = emptyList(),
        isFree = isFree, priceCents = if (isFree) 0 else 900, isOwned = isOwned
    )

    @Test
    fun aPartDayLeftCountsAsADay() {
        assertEquals(1L, daysLeft(now + 1, now))
        assertEquals(45L, daysLeft(now + 44 * day + 5, now))
        assertEquals(0L, daysLeft(now - 1, now))
    }

    @Test
    fun messagesLeftSayHowManyAndForHowLong() {
        val offer = LessonOffer(ownedTier = LessonTier.LESSON, messagesLeft = 2, messagesTotal = 3, messagesUntilMs = now + 45 * day)
        assertEquals("۲ پیام به استاد مانده، تا ۴۵ روز دیگر", messagesLine(offer, now))
    }

    @Test
    fun theLastDaySaysToday() {
        val offer = LessonOffer(ownedTier = LessonTier.LESSON, messagesLeft = 1, messagesTotal = 3, messagesUntilMs = now + day / 2)
        assertEquals("۱ پیام به استاد مانده، تا امروز", messagesLine(offer, now))
    }

    @Test
    fun noLineForALessonTheStudentDoesNotHave() {
        assertNull(messagesLine(LessonOffer(), now))
    }

    @Test
    fun thePurchaseRuleSaysForeverAndThreeMonths() {
        assertEquals("درس برای همیشه مال شماست. پیام‌ها به استاد تا ۳ ماه بعد از خرید.", ownershipRule(LessonOffer()))
    }

    @Test
    fun theUpgradeRuleSaysTheMessagesAddUpToTheNewTotal() {
        assertEquals(
            "پیام‌هایتان به استاد روی هم به ۲۰ می‌رسد، تا ۳ ماه بعد از ارتقا.",
            upgradeRule(LessonOffer(ownedTier = LessonTier.LESSON))
        )
    }

    @Test
    fun theLessonChipSaysFreeOwnedOrTheStartingPrice() {
        assertEquals("رایگان", saleChip(lesson(isFree = true)))
        assertEquals("درس شما", saleChip(lesson(isOwned = true)))
        assertEquals("از ۹ یورو", saleChip(lesson()))
    }
}
