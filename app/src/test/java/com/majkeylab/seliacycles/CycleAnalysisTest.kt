package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class CycleAnalysisTest {
    @Test
    fun `recorded average excludes an unfinished period and has no default for empty history`() {
        val first = LocalDate.of(2026, 8, 1)
        val second = first.plusDays(28)
        val bleeding = (0L..4L).map(first::plusDays).toSet() + second
        assertEquals(5, CycleAnalysis.averageRecordedPeriodDays(listOf(first, second), bleeding, second))
        assertEquals(null, CycleAnalysis.averageRecordedPeriodDays(emptyList(), emptySet(), null))
    }

    @Test
    fun `impossible luteal estimates are omitted without removing observed cycle lengths`() {
        val first = LocalDate.of(2026, 9, 1)
        val starts = listOf(first, first.plusDays(15), first.plusDays(43))
        val history = CycleAnalysis.recentHistory(starts, starts.toSet(), lutealPhaseDays = 19)

        assertEquals(listOf(15, 28), CycleAnalysis.recentLengths(starts).map(CycleLengthSample::days))
        assertEquals(listOf(first.plusDays(15)), history.map(CycleHistorySample::start))
    }

    @Test
    fun `reconstructed history is not measured as a prediction made in advance`() {
        val start = LocalDate.of(2026, 9, 1)
        val snapshot = ForecastSnapshot(YearMonth.from(start), start, start.minusDays(2), start.plusDays(2), 5, true)
        assertEquals(null, CycleAnalysis.predictionAccuracy(listOf(start), mapOf(snapshot.month to snapshot)))
    }

    @Test
    fun `chart preserves long observed cycles without assuming missed tracking`() {
        val first = LocalDate.of(2026, 1, 1)
        val starts = listOf(first, first.plusDays(28), first.plusDays(90), first.plusDays(121))

        assertEquals(
            listOf(28, 62, 31),
            CycleAnalysis.recentLengths(starts).map(CycleLengthSample::days),
        )
    }

    @Test
    fun `history combines cycle period and fertility dates`() {
        val first = LocalDate.of(2026, 1, 1)
        val second = LocalDate.of(2026, 1, 29)
        val third = LocalDate.of(2026, 2, 26)
        val bleeding = (0L..4L).map(first::plusDays) + (0L..2L).map(second::plusDays)

        assertEquals(
            listOf(
                CycleHistorySample(
                    start = first,
                    cycleDays = 28,
                    periodDays = 5,
                    ovulation = LocalDate.of(2026, 1, 15),
                    fertileStart = LocalDate.of(2026, 1, 10),
                    fertileEnd = LocalDate.of(2026, 1, 16),
                ),
                CycleHistorySample(
                    start = second,
                    cycleDays = 28,
                    periodDays = 3,
                    ovulation = LocalDate.of(2026, 2, 12),
                    fertileStart = LocalDate.of(2026, 2, 7),
                    fertileEnd = LocalDate.of(2026, 2, 13),
                ),
            ),
            CycleAnalysis.recentHistory(listOf(first, second, third), bleeding.toSet(), lutealPhaseDays = 14),
        )
    }

    @Test
    fun `accuracy summarizes saved windows against recorded starts`() {
        val january = ForecastSnapshot(
            month = YearMonth.of(2026, 1),
            periodStart = LocalDate.of(2026, 1, 10),
            earliestStart = LocalDate.of(2026, 1, 8),
            latestStart = LocalDate.of(2026, 1, 12),
            periodLength = 5,
            reconstructed = false,
        )
        val february = ForecastSnapshot(
            month = YearMonth.of(2026, 2),
            periodStart = LocalDate.of(2026, 2, 5),
            earliestStart = LocalDate.of(2026, 2, 4),
            latestStart = LocalDate.of(2026, 2, 6),
            periodLength = 5,
            reconstructed = false,
        )

        assertEquals(
            PredictionAccuracySummary(sampleCount = 2, averageErrorDays = 3, withinWindowCount = 1),
            CycleAnalysis.predictionAccuracy(
                periodStarts = listOf(LocalDate.of(2026, 1, 12), LocalDate.of(2026, 2, 9)),
                snapshots = mapOf(january.month to january, february.month to february),
            ),
        )
    }

    @Test
    fun `accuracy matches reality across a month boundary`() {
        val snapshot = ForecastSnapshot(
            month = YearMonth.of(2026, 1),
            periodStart = LocalDate.of(2026, 1, 31),
            earliestStart = LocalDate.of(2026, 1, 29),
            latestStart = LocalDate.of(2026, 2, 2),
            periodLength = 5,
            reconstructed = false,
        )

        assertEquals(
            PredictionAccuracySummary(sampleCount = 1, averageErrorDays = 1, withinWindowCount = 1),
            CycleAnalysis.predictionAccuracy(
                periodStarts = listOf(LocalDate.of(2026, 2, 1)),
                snapshots = mapOf(snapshot.month to snapshot),
            ),
        )
    }
}
