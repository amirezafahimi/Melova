package app.melova.data.model

import app.melova.data.remote.dto.JournalSectionDto
import app.melova.data.remote.dto.JournalTakeDto
import app.melova.data.remote.dto.PracticeWeekDto
import app.melova.data.repository.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PracticeLevelsTest {
    @Test
    fun onlyTheTeachersStampMakesASectionMastered() {
        assertEquals(SectionLevel.FAMILIAR, PracticeLevels.level(best = 100, stamped = false))
        assertEquals(SectionLevel.MASTERED, PracticeLevels.level(best = 55, stamped = true))
    }

    @Test
    fun aSectionNotSungHasNoLevel() {
        assertEquals(SectionLevel.NOT_SUNG, PracticeLevels.level(best = null, stamped = false))
        assertNull(SectionLevel.NOT_SUNG.label)
    }

    @Test
    fun aSmallGainOverTheBestIsNotCalledARecord() {
        assertFalse(PracticeLevels.isRealNewBest(score = 52, previousBest = 50))
        assertTrue(PracticeLevels.isRealNewBest(score = 53, previousBest = 50))
    }

    @Test
    fun aFirstTakeHasNoRecordToBeat() {
        assertFalse(PracticeLevels.isRealNewBest(score = 90, previousBest = null))
    }

    @Test
    fun aShortWeekFromTheServerStillHasSevenDays() {
        val week = PracticeWeekDto(goal = 9, days = listOf(true, false), today = 1, practised = 1).toDomain()
        assertEquals(7, week.days.size)
        assertEquals(PracticeWeek.MAX_GOAL, week.goal)
        assertEquals(listOf(true, false, false, false, false, false, false), week.days)
    }

    @Test
    fun aJournalSectionWithOneTakeHasNothingToCompare() {
        val take = JournalTakeDto(id = "pt-1", score = 60, createdAt = 10.0)
        val section = JournalSectionDto(lessonId = "ls", sectionId = "sec", first = take, best = take, takes = 1).toDomain()
        assertTrue(assertNotNull(section).isSingleStep)
    }
}
