package app.melova.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorDetailLaneTest {
    private val teacher = ErrorDetailLane(startMs = 0, endMs = 2_000, markStartMs = 800, markEndMs = 800, speed = 0.65f)
    private val student = ErrorDetailLane(startMs = 100, endMs = 2_300, markStartMs = 1_000, markEndMs = 1_000, speed = 0.65f)

    @Test
    fun theMistakeFallsOnTheSameMomentInBothVoices() {
        assertEquals(1_000, teacher.timeOn(student, 800))
    }

    @Test
    fun theEndsOfBothLanesFallTogether() {
        assertEquals(100, teacher.timeOn(student, 0))
        assertEquals(2_300, teacher.timeOn(student, 2_000))
    }

    @Test
    fun timeBetweenTheMarksRunsStraight() {
        assertEquals(550, teacher.timeOn(student, 400))
    }

    @Test
    fun timeBeyondALaneKeepsItsDistanceFromTheEnd() {
        assertEquals(2_400, teacher.timeOn(student, 2_100))
    }
}
