package app.melova.data.model

import app.melova.data.remote.dto.LessonOfferDto
import app.melova.data.remote.mapper.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LessonSaleTest {
    private val now = 1_800_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun wholeEurosShowWithoutCentsInPersianDigits() {
        assertEquals("۹ یورو", formatEuro(900))
        assertEquals("۳۵ یورو", formatEuro(3500))
    }

    @Test
    fun centsShowAfterAPersianDecimalPoint() {
        assertEquals("۲۴٫۵۰ یورو", formatEuro(2450))
        assertEquals("۰٫۰۵ یورو", formatEuro(5))
    }

    @Test
    fun aTypedPriceReadsInEitherDigitsAndEitherPoint() {
        assertEquals(900L, parseEuroCents("9"))
        assertEquals(950L, parseEuroCents("۹٫۵"))
        assertEquals(2450L, parseEuroCents("24,50"))
        assertEquals(3500L, parseEuroCents(" ۳۵ € "))
        assertEquals(900L, parseEuroCents("۹ یورو"))
    }

    @Test
    fun aBlankPriceIsZeroAndJunkIsNoPrice() {
        assertEquals(0L, parseEuroCents(""))
        assertNull(parseEuroCents("نه"))
        assertNull(parseEuroCents("9.999"))
        assertNull(parseEuroCents("1.2.3"))
    }

    @Test
    fun aPriceTypesBackTheWayItWasWritten() {
        assertEquals("9", euroInput(900))
        assertEquals("24.5", euroInput(2450))
        assertEquals(950L, parseEuroCents(euroInput(950)))
    }

    @Test
    fun theTeacherVersionMustCostMoreThanThePlainOne() {
        assertNull(salePriceProblem(900, 3500))
        assertNull(salePriceProblem(900, 0))
        assertNull(salePriceProblem(0, 3500))
        assertTrue(salePriceProblem(3500, 900) != null)
        assertTrue(salePriceProblem(900, 900) != null)
        assertTrue(salePriceProblem(null, 900) != null)
        assertTrue(salePriceProblem(200_000, 0) != null)
    }

    @Test
    fun aStudentWithoutTheLessonCannotSendYet() {
        assertEquals(MessageState.NotOwned, LessonOffer().sendState(now))
    }

    @Test
    fun messagesLeftBeforeTheirEndCanBeSent() {
        val offer = LessonOffer(ownedTier = LessonTier.LESSON, messagesLeft = 2, messagesTotal = 3, messagesUntilMs = now + day)
        assertEquals(MessageState.Left(2, now + day), offer.sendState(now))
    }

    @Test
    fun usedUpPlainMessagesOfferTheTeacherVersion() {
        val offer = LessonOffer(
            ownedTier = LessonTier.LESSON,
            messagesLeft = 0,
            messagesTotal = 3,
            messagesUntilMs = now + day,
            teacherPriceNowCents = 2600
        )
        assertEquals(MessageState.UsedUp(canUpgrade = true), offer.sendState(now))
    }

    @Test
    fun expiredMessagesAreToldApartFromUsedOnes() {
        val offer = LessonOffer(ownedTier = LessonTier.WITH_TEACHER, messagesLeft = 0, messagesTotal = 20, messagesUntilMs = now - 1)
        assertEquals(MessageState.Expired(canUpgrade = false), offer.sendState(now))
    }

    @Test
    fun theTeacherVersionCannotBeBoughtTwice() {
        val offer = LessonOffer(ownedTier = LessonTier.WITH_TEACHER, teacherPriceNowCents = 100)
        assertFalse(offer.canUpgrade)
    }

    @Test
    fun ninetyDaysReadAsThreeMonths() {
        assertEquals(3, LessonOffer(messageDays = 90).messageMonths)
        assertEquals(1, LessonOffer(messageDays = 30).messageMonths)
    }

    @Test
    fun theServerOfferMapsSecondsToMillisAndKnowsItsTier() {
        val offer = LessonOfferDto(ownedTier = "teacher", messagesLeft = 19, messagesTotal = 20, messagesUntil = 1_800_000_000.5).toDomain()
        assertEquals(LessonTier.WITH_TEACHER, offer.ownedTier)
        assertEquals(1_800_000_000_500L, offer.messagesUntilMs)
        assertIs<MessageState.Left>(offer.sendState(now))
    }

    @Test
    fun onlyTheFirstSectionOfALessonNotBoughtIsOpen() {
        val section = { id: String -> LessonSection(id, id, "", true, 1000, "v") }
        val lesson = Lesson(
            id = "l1", teacherId = "t1", teacher = "", title = "", subtitle = "", description = "", level = "",
            published = true, hasIntro = false, introVersion = "",
            sections = listOf(section("s1"), section("s2")),
            isFree = false, priceCents = 900, isOwned = false
        )
        assertTrue(lesson.isSectionOpen("s1"))
        assertFalse(lesson.isSectionOpen("s2"))
        assertTrue(lesson.copy(isOwned = true).isSectionOpen("s2"))
    }
}
