package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CycleInsightsTest {
    @Test
    fun `pregnancy hides future estimates but keeps saved history`() {
        val august = ForecastSnapshot(
            month = java.time.YearMonth.of(2026, 8),
            periodStart = LocalDate.of(2026, 8, 5),
            earliestStart = LocalDate.of(2026, 8, 3),
            latestStart = LocalDate.of(2026, 8, 7),
            periodLength = 3,
            reconstructed = false,
        )
        val september = august.copy(
            month = java.time.YearMonth.of(2026, 9),
            periodStart = LocalDate.of(2026, 9, 5),
            earliestStart = LocalDate.of(2026, 9, 3),
            latestStart = LocalDate.of(2026, 9, 7),
        )
        val backup = CycleBackup(
            settings = AppSettings(profile = UserProfile(lifeSituation = LifeSituation.PREGNANT)),
        )

        assertEquals(
            listOf(august.periodStart),
            CycleInsights.periodEstimates(
                backup,
                mapOf(august.month to august, september.month to september),
                LocalDate.of(2026, 8, 20),
            ).map(PeriodEstimate::start),
        )
    }

    @Test
    fun `hormonal contraception hides calendar fertility estimate`() {
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 6, 1)) + period(LocalDate.of(2026, 7, 1)),
            settings = AppSettings(profile = UserProfile(lifeSituation = LifeSituation.HORMONAL_CONTRACEPTION)),
        )

        val insight = CycleInsights.forDate(backup, emptyMap(), LocalDate.of(2026, 7, 17))

        assertNull(insight.fertility)
        assertEquals(FertilityStatus.UNAVAILABLE, insight.fertilityStatus)
    }

    @Test
    fun `reports calendar fertility status at exact boundaries`() {
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 6, 1)) + period(LocalDate.of(2026, 7, 1)),
        )

        assertEquals(FertilityStatus.OUTSIDE, CycleInsights.forDate(backup, emptyMap(), LocalDate.of(2026, 7, 11)).fertilityStatus)
        assertEquals(FertilityStatus.FERTILE, CycleInsights.forDate(backup, emptyMap(), LocalDate.of(2026, 7, 12)).fertilityStatus)
        assertEquals(FertilityStatus.OVULATION, CycleInsights.forDate(backup, emptyMap(), LocalDate.of(2026, 7, 17)).fertilityStatus)
        assertEquals(FertilityStatus.FERTILE, CycleInsights.forDate(backup, emptyMap(), LocalDate.of(2026, 7, 18)).fertilityStatus)
    }

    @Test
    fun `future recorded period remains the next fertility boundary`() {
        val futureStart = LocalDate.of(2026, 9, 26)
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 6, 1)) +
                period(LocalDate.of(2026, 7, 1)) +
                period(LocalDate.of(2026, 8, 28)) +
                period(futureStart),
        )

        val predicted = LocalDate.of(2026, 9, 25)
        val snapshot = ForecastSnapshot(
            month = java.time.YearMonth.of(2026, 9),
            periodStart = predicted,
            earliestStart = predicted.minusDays(2),
            latestStart = predicted.plusDays(2),
            periodLength = 5,
            reconstructed = false,
        )

        val insight = CycleInsights.forDate(
            backup,
            mapOf(snapshot.month to snapshot),
            LocalDate.of(2026, 8, 30),
        )

        assertEquals(futureStart, insight.nextPeriodStart)
        assertEquals(LocalDate.of(2026, 9, 12), insight.fertility?.ovulation)
        assertEquals(LocalDate.of(2026, 9, 7), insight.fertility?.fertileStart)
        assertEquals(LocalDate.of(2026, 9, 13), insight.fertility?.fertileEnd)
    }

    @Test
    fun `calendar fertility includes a future recorded period`() {
        val futureStart = LocalDate.of(2026, 9, 26)
        val predicted = LocalDate.of(2026, 9, 24)
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 7, 1)) +
                period(LocalDate.of(2026, 8, 28)) +
                period(futureStart),
        )

        val snapshot = ForecastSnapshot(
            month = java.time.YearMonth.of(2026, 9),
            periodStart = predicted,
            earliestStart = predicted.minusDays(2),
            latestStart = predicted.plusDays(2),
            periodLength = 5,
            reconstructed = false,
        )
        val starts = CycleInsights.fertilityEstimates(
            backup,
            mapOf(snapshot.month to snapshot),
            LocalDate.of(2026, 8, 30),
        )
            .map(FertilityEstimate::periodStart)

        assertTrue(futureStart in starts)
        assertEquals(1, starts.count { java.time.YearMonth.from(it) == snapshot.month })
    }

    @Test
    fun `saved current baseline cannot break the current prediction sequence`() {
        val reference = LocalDate.of(2026, 9, 1)
        val savedStart = LocalDate.of(2026, 9, 26)
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 8, 13)),
            settings = AppSettings(
                cycleLength = 30,
                periodLength = 5,
                cycleLengthOverride = 30,
                periodLengthOverride = 5,
            ),
        )
        val snapshot = ForecastSnapshot(
            month = java.time.YearMonth.of(2026, 9),
            periodStart = savedStart,
            earliestStart = savedStart.minusDays(1),
            latestStart = savedStart.plusDays(1),
            periodLength = 4,
            reconstructed = false,
        )

        val insight = CycleInsights.forDate(backup, mapOf(snapshot.month to snapshot), reference)
        val fertility = CycleInsights.fertilityEstimates(backup, mapOf(snapshot.month to snapshot), reference)
            .filter { !it.periodStart.isBefore(reference) }

        assertEquals(LocalDate.of(2026, 9, 12), insight.nextPeriodStart)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 10, 12)),
            fertility.take(2).map(FertilityEstimate::periodStart),
        )
        assertTrue(fertility[1].fertileStart.isAfter(fertility[0].periodStart.plusDays(4)))
    }

    @Test
    fun `reports unavailable fertility without cycle history`() {
        assertEquals(
            FertilityStatus.UNAVAILABLE,
            CycleInsights.forDate(CycleBackup(), emptyMap(), LocalDate.of(2026, 7, 17)).fertilityStatus,
        )
    }

    @Test
    fun derivesEstimatedOvulationAndFertileWindowFromPeriodStart() {
        val fertility = CycleInsights.fertilityForPeriod(LocalDate.of(2026, 8, 29))

        assertEquals(LocalDate.of(2026, 8, 15), fertility.ovulation)
        assertEquals(LocalDate.of(2026, 8, 10), fertility.fertileStart)
        assertEquals(LocalDate.of(2026, 8, 16), fertility.fertileEnd)
    }

    @Test
    fun `custom luteal phase shifts ovulation and fertile window`() {
        val fertility = CycleInsights.fertilityForPeriod(LocalDate.of(2026, 8, 29), lutealPhaseDays = 12)

        assertEquals(LocalDate.of(2026, 8, 17), fertility.ovulation)
        assertEquals(LocalDate.of(2026, 8, 12), fertility.fertileStart)
        assertEquals(LocalDate.of(2026, 8, 18), fertility.fertileEnd)
    }

    @Test
    fun predictsMoodOnlyFromEnoughPersonalSamePhaseHistory() {
        val logs = period(LocalDate.of(2026, 6, 1), Mood.GOOD, Mood.GOOD) +
            period(LocalDate.of(2026, 7, 1), Mood.GOOD) +
            period(LocalDate.of(2026, 8, 1))

        val insight = CycleInsights.forDate(CycleBackup(logs = logs), emptyMap(), LocalDate.of(2026, 8, 1))

        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(PersonalMoodTrend(Mood.GOOD, sampleCount = 3, cycleCount = 2), insight.moodTrend)
    }

    @Test
    fun omitsMoodWhenEvidenceIsInsufficientOrSplit() {
        val insufficient = period(LocalDate.of(2026, 6, 1), Mood.GOOD, Mood.GOOD) +
            period(LocalDate.of(2026, 7, 1))
        val split = period(LocalDate.of(2026, 5, 1), Mood.GOOD, Mood.BAD) +
            period(LocalDate.of(2026, 6, 1), Mood.GOOD, Mood.BAD) +
            period(LocalDate.of(2026, 7, 1))

        assertNull(CycleInsights.forDate(CycleBackup(logs = insufficient), emptyMap(), LocalDate.of(2026, 7, 1)).moodTrend)
        assertNull(CycleInsights.forDate(CycleBackup(logs = split), emptyMap(), LocalDate.of(2026, 7, 1)).moodTrend)
    }

    @Test
    fun treatsAnEstimatedPeriodDayAsMenstrualPhase() {
        val logs = period(LocalDate.of(2026, 6, 1)) + period(LocalDate.of(2026, 7, 1))

        val insight = CycleInsights.forDate(CycleBackup(logs = logs), emptyMap(), LocalDate.of(2026, 7, 31))

        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
    }

    @Test
    fun keepsASecondFuturePeriodInTheSameMonthAfterRealityIsLogged() {
        val month = java.time.YearMonth.of(2026, 8)
        val snapshot = ForecastSnapshot(
            month = month,
            periodStart = LocalDate.of(2026, 8, 5),
            earliestStart = LocalDate.of(2026, 8, 3),
            latestStart = LocalDate.of(2026, 8, 7),
            periodLength = 3,
            reconstructed = false,
        )
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 8, 10)),
            settings = AppSettings(cycleLength = 15, periodLength = 3),
        )

        val starts = CycleInsights.periodEstimates(backup, mapOf(month to snapshot), LocalDate.of(2026, 8, 20))
            .map(PeriodEstimate::start)

        assertEquals(listOf(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 25)), starts.take(2))
    }

    private fun period(start: LocalDate, vararg moods: Mood): List<DayLog> = (0L..2L).map { offset ->
        DayLog(
            day = start.plusDays(offset),
            bleeding = true,
            flow = Flow.UNKNOWN,
            mood = moods.getOrNull(offset.toInt()),
        )
    }
}
