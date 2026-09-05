package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CycleInsightsTest {
    @Test
    fun `historical day detail uses the same saved fertility baseline as its calendar`() {
        val backup = CycleBackup(logs = listOf(
            LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 29), LocalDate.of(2026, 9, 26),
        ).flatMap { period(it) })
        val start = LocalDate.of(2026, 9, 25)
        val snapshot = ForecastSnapshot(java.time.YearMonth.from(start), start, start.minusDays(2), start.plusDays(2), 3, false)
        val snapshots = mapOf(snapshot.month to snapshot)
        val reference = LocalDate.of(2026, 10, 1)
        val selected = LocalDate.of(2026, 9, 11)
        val calendar = CycleInsights.fertilityEstimates(backup, snapshots, reference).single { it.ovulation == selected }

        val insight = CycleInsights.forDate(backup, snapshots, selected, referenceDate = reference)

        assertEquals(calendar, insight.fertility)
        assertEquals(FertilityStatus.OVULATION, insight.fertilityStatus)
    }

    @Test
    fun `viewing a future day does not reuse todays pending period`() {
        val backup = CycleBackup(logs = period(LocalDate.of(2026, 9, 1)))
        val selected = LocalDate.of(2026, 11, 26)

        val insight = CycleInsights.forDate(backup, emptyMap(), selected, referenceDate = LocalDate.of(2026, 9, 10))

        assertEquals(LocalDate.of(2026, 11, 24), insight.nextPeriodStart)
        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(LocalDate.of(2026, 12, 22), insight.fertility?.periodStart)
    }

    @Test
    fun `luteal phase longer than modeled cycle disables fertility without changing period dates`() {
        val backup = shortCycle(lutealPhaseDays = 19)
        val reference = LocalDate.of(2026, 9, 12)
        val insight = CycleInsights.forDate(backup, emptyMap(), reference)

        assertEquals(LocalDate.of(2026, 9, 16), insight.nextPeriodStart)
        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertNull(insight.fertility)
        assertEquals(FertilityStatus.UNAVAILABLE, insight.fertilityStatus)
        assertTrue(CycleInsights.fertilityEstimates(backup, emptyMap(), reference).isEmpty())
        assertEquals(19, backup.settings.lutealPhaseLength)
    }

    @Test
    fun `daily fertility matches the calendar window across a short cycle boundary`() {
        val backup = shortCycle(lutealPhaseDays = 14)
        val reference = LocalDate.of(2026, 9, 12)
        val calendar = CycleInsights.fertilityEstimates(backup, emptyMap(), reference)
            .single { reference in it.fertileStart..it.fertileEnd }
        val insight = CycleInsights.forDate(backup, emptyMap(), reference)

        assertEquals(LocalDate.of(2026, 9, 16), insight.nextPeriodStart)
        assertEquals(LocalDate.of(2026, 10, 1), calendar.periodStart)
        assertEquals(calendar, insight.fertility)
        assertEquals(FertilityStatus.FERTILE, insight.fertilityStatus)
        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
    }

    @Test
    fun `recorded bleeding does not erase a possible overlapping fertility window`() {
        val insight = CycleInsights.forDate(shortCycle(lutealPhaseDays = 14), emptyMap(), LocalDate.of(2026, 9, 2))

        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(FertilityStatus.OVULATION, insight.fertilityStatus)
        assertEquals(LocalDate.of(2026, 9, 2), insight.fertility?.ovulation)
    }

    @Test
    fun `a longer average cannot put historical ovulation before its recorded cycle start`() {
        val starts = listOf(
            LocalDate.of(2026, 6, 6), LocalDate.of(2026, 7, 4), LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 29), LocalDate.of(2026, 9, 13),
        )
        val backup = CycleBackup(logs = starts.flatMap { period(it) }, settings = AppSettings(lutealPhaseLength = 19))
        val reference = LocalDate.of(2026, 9, 1)

        assertTrue(CycleInsights.fertilityEstimates(backup, emptyMap(), reference).none {
            it.periodStart == LocalDate.of(2026, 9, 13)
        })
        assertNull(CycleInsights.forDate(backup, emptyMap(), reference).fertility)
    }

    @Test
    fun `future calendar estimates retain widening uncertainty windows`() {
        val backup = CycleBackup(logs = period(LocalDate.of(2026, 8, 1)))
        val estimates = CycleInsights.calendarPeriodEstimates(backup, emptyMap(), LocalDate.of(2026, 8, 10))

        assertTrue(estimates.size > 2)
        assertTrue(estimates.all { it.earliestStart != null && it.latestStart != null })
        assertTrue(requireNotNull(estimates[1].latestStart).toEpochDay() - estimates[1].start.toEpochDay() >
            requireNotNull(estimates[0].latestStart).toEpochDay() - estimates[0].start.toEpochDay())
    }

    @Test
    fun `disabled forecasts cannot classify today from a historical estimate`() {
        val start = LocalDate.of(2026, 8, 31)
        val snapshot = ForecastSnapshot(java.time.YearMonth.from(start), start, start, start, 5, false)
        val backup = CycleBackup(settings = AppSettings(predictionsEnabled = false))

        val insight = CycleInsights.forDate(backup, mapOf(snapshot.month to snapshot), LocalDate.of(2026, 9, 1))

        assertNull(insight.nextPeriodStart)
        assertNull(insight.phase)
        assertEquals(FertilityStatus.UNAVAILABLE, insight.fertilityStatus)
        assertEquals(listOf(start), CycleInsights.periodEstimates(
            backup, mapOf(snapshot.month to snapshot), LocalDate.of(2026, 9, 1),
        ).map(PeriodEstimate::start))
    }

    @Test
    fun `overdue start remains visible throughout its uncertainty window`() {
        val backup = CycleBackup(logs = listOf(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26), LocalDate.of(2026, 4, 10), LocalDate.of(2026, 5, 8),
        ).flatMap { period(it) })

        val insight = CycleInsights.forDate(backup, emptyMap(), LocalDate.of(2026, 6, 8))

        assertEquals(LocalDate.of(2026, 6, 5), insight.nextPeriodStart)
        assertNull(insight.phase)
        assertEquals(LocalDate.of(2026, 7, 3), insight.fertility?.periodStart)
    }

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
    fun `overdue expected period stays visible without moving future fertility backward`() {
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 6, 26)) + period(LocalDate.of(2026, 7, 24)),
            settings = AppSettings(cycleLength = 28, periodLength = 3),
        )

        val insight = CycleInsights.forDate(backup, emptyMap(), LocalDate.of(2026, 8, 22))

        assertEquals(LocalDate.of(2026, 8, 21), insight.nextPeriodStart)
        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(LocalDate.of(2026, 9, 18), insight.fertility?.periodStart)
    }

    @Test
    fun `recorded period supersedes a covering saved estimate`() {
        val actualStart = LocalDate.of(2026, 8, 27)
        val logs = (0L..5L).map { offset ->
            DayLog(actualStart.plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
        }
        val snapshot = ForecastSnapshot(
            month = java.time.YearMonth.of(2026, 8),
            periodStart = LocalDate.of(2026, 8, 28),
            earliestStart = LocalDate.of(2026, 8, 26),
            latestStart = LocalDate.of(2026, 8, 30),
            periodLength = 5,
            reconstructed = false,
        )
        val backup = CycleBackup(
            logs = logs,
            settings = AppSettings(cycleLength = 28, periodLength = 5, cycleLengthOverride = 28),
        )

        val insight = CycleInsights.forDate(backup, mapOf(snapshot.month to snapshot), LocalDate.of(2026, 9, 1))

        assertEquals(LocalDate.of(2026, 9, 24), insight.nextPeriodStart)
        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(LocalDate.of(2026, 9, 24), insight.fertility?.periodStart)
    }

    @Test
    fun `current estimate takes precedence over an overlapping historical snapshot`() {
        val backup = CycleBackup(
            logs = period(LocalDate.of(2026, 8, 3)),
            settings = AppSettings(cycleLength = 28, periodLength = 3),
        )
        val snapshot = ForecastSnapshot(
            month = java.time.YearMonth.of(2026, 8),
            periodStart = LocalDate.of(2026, 8, 28),
            earliestStart = LocalDate.of(2026, 8, 26),
            latestStart = LocalDate.of(2026, 8, 30),
            periodLength = 5,
            reconstructed = false,
        )

        val insight = CycleInsights.forDate(backup, mapOf(snapshot.month to snapshot), LocalDate.of(2026, 9, 1))

        assertEquals(LocalDate.of(2026, 8, 31), insight.nextPeriodStart)
        assertEquals(CyclePhase.MENSTRUAL, insight.phase)
        assertEquals(LocalDate.of(2026, 9, 28), insight.fertility?.periodStart)
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

    private fun shortCycle(lutealPhaseDays: Int): CycleBackup = CycleBackup(
        logs = (0L..11L).map { offset ->
            DayLog(LocalDate.of(2026, 9, 1).plusDays(offset), bleeding = true, flow = Flow.UNKNOWN)
        },
        settings = AppSettings(cycleLengthOverride = 15, periodLengthOverride = 14, lutealPhaseLength = lutealPhaseDays),
    )

    private fun period(start: LocalDate, vararg moods: Mood): List<DayLog> = (0L..2L).map { offset ->
        DayLog(
            day = start.plusDays(offset),
            bleeding = true,
            flow = Flow.UNKNOWN,
            mood = moods.getOrNull(offset.toInt()),
        )
    }
}
