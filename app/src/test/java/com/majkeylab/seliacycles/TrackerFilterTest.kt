package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackerFilterTest {
    private val day = LocalDate.of(2026, 8, 28)

    @Test
    fun `individual symptoms and grouped trackers match only their own data`() {
        val log = DayLog(
            day = day,
            symptoms = setOf(Symptom.CRAMPS),
            mood = Mood.LOW,
            painLevel = 6,
            note = "private",
        )

        assertTrue(TrackerFilter.CRAMPS.matches(log))
        assertTrue(TrackerFilter.MOOD.matches(log))
        assertTrue(TrackerFilter.PAIN.matches(log))
        assertTrue(TrackerFilter.NOTES.matches(log))
        assertFalse(TrackerFilter.HEADACHE.matches(log))
        assertFalse(TrackerFilter.INTIMACY.matches(log))
    }

    @Test
    fun `available filters contain only trackers used in local logs`() {
        val available = TrackerFilter.availableFilters(
            listOf(
                DayLog(day, spotting = true),
                DayLog(day.plusDays(1), energy = WellbeingLevel.LOW, symptoms = setOf(Symptom.FATIGUE)),
            ),
        )

        assertEquals(setOf(TrackerFilter.SPOTTING, TrackerFilter.ENERGY, TrackerFilter.FATIGUE), available.toSet())
    }

    @Test
    fun `selection stops at three and active filters match with or semantics`() {
        val selected = TrackerFilter.toggleSelection(
            TrackerFilter.toggleSelection(
                TrackerFilter.toggleSelection(emptySet(), TrackerFilter.CRAMPS),
                TrackerFilter.MOOD,
            ),
            TrackerFilter.PAIN,
        )
        val capped = TrackerFilter.toggleSelection(selected, TrackerFilter.ENERGY)
        val matching = DayLog(day, mood = Mood.GOOD)

        assertEquals(selected, capped)
        assertTrue(TrackerFilter.matchesAny(capped, matching))
        assertFalse(TrackerFilter.matchesAny(capped, DayLog(day.plusDays(1))))
        assertEquals(selected - TrackerFilter.MOOD, TrackerFilter.toggleSelection(selected, TrackerFilter.MOOD))
    }
}
