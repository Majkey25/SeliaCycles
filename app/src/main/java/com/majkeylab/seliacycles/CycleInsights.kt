package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class CyclePhase { MENSTRUAL, FOLLICULAR, FERTILE, LUTEAL }

enum class FertilityStatus { UNAVAILABLE, OUTSIDE, FERTILE, OVULATION }

enum class EstimateOrigin { SAVED, RECONSTRUCTED, CURRENT }

data class PeriodEstimate(
    val start: LocalDate,
    val endExclusive: LocalDate,
    val earliestStart: LocalDate?,
    val latestStart: LocalDate?,
    val origin: EstimateOrigin,
)

data class FertilityEstimate(
    val periodStart: LocalDate,
    val ovulation: LocalDate,
    val fertileStart: LocalDate,
    val fertileEnd: LocalDate,
)

data class PersonalMoodTrend(
    val mood: Mood,
    val sampleCount: Int,
    val cycleCount: Int,
)

data class DailyCycleInsight(
    val nextPeriodStart: LocalDate?,
    val phase: CyclePhase?,
    val fertility: FertilityEstimate?,
    val fertilityStatus: FertilityStatus,
    val moodTrend: PersonalMoodTrend?,
)

object CycleInsights {
    fun fertilityForPeriod(periodStart: LocalDate, lutealPhaseDays: Int = DEFAULT_LUTEAL_PHASE_DAYS): FertilityEstimate {
        require(lutealPhaseDays in 7..19)
        val ovulation = periodStart.minusDays(lutealPhaseDays.toLong())
        return FertilityEstimate(
            periodStart = periodStart,
            ovulation = ovulation,
            fertileStart = ovulation.minusDays(FERTILE_DAYS_BEFORE),
            fertileEnd = ovulation.plusDays(FERTILE_DAYS_AFTER),
        )
    }

    fun periodEstimates(
        backup: CycleBackup,
        snapshots: Map<YearMonth, ForecastSnapshot>,
        referenceDate: LocalDate = LocalDate.now(),
    ): List<PeriodEstimate> {
        val saved = snapshots.values.map { snapshot ->
            PeriodEstimate(
                start = snapshot.periodStart,
                endExclusive = snapshot.periodStart.plusDays(snapshot.periodLength.toLong()),
                earliestStart = snapshot.earliestStart,
                latestStart = snapshot.latestStart,
                origin = if (snapshot.reconstructed) EstimateOrigin.RECONSTRUCTED else EstimateOrigin.SAVED,
            )
        }
        if (!backup.settings.canPredictPeriods) {
            return saved.filter { !it.start.isAfter(referenceDate) }.sortedBy(PeriodEstimate::start)
        }
        val prediction = prediction(backup, referenceDate)
        val dynamic = prediction.futurePeriodStarts.mapIndexedNotNull { index, start ->
            val month = YearMonth.from(start)
            val snapshot = snapshots[month]
            val include = snapshot == null || prediction.periodStarts.any { actual ->
                YearMonth.from(actual) == month && ChronoUnit.DAYS.between(actual, start) >= MIN_CYCLE_DAYS
            } && start != snapshot.periodStart
            if (!include) return@mapIndexedNotNull null
            PeriodEstimate(
                start = start,
                endExclusive = start.plusDays(prediction.averagePeriodLength.toLong()),
                earliestStart = prediction.earliestPeriodStart.takeIf { index == 0 },
                latestStart = prediction.latestPeriodStart.takeIf { index == 0 },
                origin = EstimateOrigin.CURRENT,
            )
        }
        return (saved + dynamic).sortedBy(PeriodEstimate::start)
    }

    fun calendarPeriodEstimates(
        backup: CycleBackup,
        snapshots: Map<YearMonth, ForecastSnapshot>,
        referenceDate: LocalDate = LocalDate.now(),
    ): List<PeriodEstimate> {
        val currentMonth = YearMonth.from(referenceDate)
        val history = periodEstimates(backup, snapshots, referenceDate)
            .filter { YearMonth.from(it.start) < currentMonth }
        val current = periodEstimates(backup, emptyMap(), referenceDate)
        return (history + current).distinctBy(PeriodEstimate::start).sortedBy(PeriodEstimate::start)
    }

    fun fertilityEstimates(
        backup: CycleBackup,
        snapshots: Map<YearMonth, ForecastSnapshot>,
        referenceDate: LocalDate = LocalDate.now(),
    ): List<FertilityEstimate> {
        if (!backup.settings.canEstimateFertility) return emptyList()
        val futureRecorded = prediction(backup, referenceDate).periodStarts.filter { it.isAfter(referenceDate) }
        val recordedMonths = futureRecorded.mapTo(mutableSetOf(), YearMonth::from)
        val estimated = calendarPeriodEstimates(backup, snapshots, referenceDate).map(PeriodEstimate::start)
            .filterNot { YearMonth.from(it) in recordedMonths }
        return (estimated + futureRecorded)
            .distinct().sorted().map { fertilityForPeriod(it, backup.settings.lutealPhaseLength) }
    }

    fun forDate(
        backup: CycleBackup,
        snapshots: Map<YearMonth, ForecastSnapshot>,
        date: LocalDate = LocalDate.now(),
    ): DailyCycleInsight {
        val prediction = prediction(backup, date)
        val estimates = calendarPeriodEstimates(backup, snapshots, date)
        val estimatedPeriod = estimates.firstOrNull { date >= it.start && date < it.endExclusive }
        val nextPeriod = if (!backup.settings.canPredictPeriods) null else {
            prediction.periodStarts.firstOrNull { it.isAfter(date) }
                ?: estimates.firstOrNull { it.start.isAfter(estimatedPeriod?.start ?: date) }?.start
        }
        val activeStart = prediction.periodStarts.lastOrNull { !it.isAfter(date) }
        val fertility = nextPeriod?.takeIf { backup.settings.canEstimateFertility }?.let {
            fertilityForPeriod(it, backup.settings.lutealPhaseLength)
        }
        val phase = when {
            estimatedPeriod != null || backup.logs.any { it.day == date && it.bleeding } -> CyclePhase.MENSTRUAL
            activeStart == null || fertility == null -> null
            else -> phaseFor(
                day = date,
                cycleStart = activeStart,
                nextPeriodStart = fertility.periodStart,
                periodLength = prediction.averagePeriodLength,
                lutealPhaseDays = backup.settings.lutealPhaseLength,
            )
        }
        return DailyCycleInsight(
            nextPeriodStart = nextPeriod,
            phase = phase,
            fertility = fertility,
            fertilityStatus = when {
                fertility == null -> FertilityStatus.UNAVAILABLE
                date == fertility.ovulation -> FertilityStatus.OVULATION
                date in fertility.fertileStart..fertility.fertileEnd -> FertilityStatus.FERTILE
                else -> FertilityStatus.OUTSIDE
            },
            moodTrend = phase?.let { moodTrend(backup, prediction, it) },
        )
    }

    private fun moodTrend(
        backup: CycleBackup,
        prediction: CyclePrediction,
        targetPhase: CyclePhase,
    ): PersonalMoodTrend? {
        val cycles = prediction.periodStarts.zipWithNext().takeLast(MAX_MOOD_CYCLES)
        val samples = cycles.flatMap { (start, next) ->
            backup.logs.asSequence()
                .filter { it.mood != null && it.day >= start && it.day < next }
                .filter {
                    phaseFor(
                        it.day,
                        start,
                        next,
                        prediction.averagePeriodLength,
                        backup.settings.lutealPhaseLength,
                    ) == targetPhase
                }
                .map { MoodSample(requireNotNull(it.mood), start) }
                .toList()
        }
        if (samples.size < MIN_MOOD_SAMPLES || samples.map(MoodSample::cycleStart).distinct().size < MIN_MOOD_CYCLES) {
            return null
        }
        val sorted = samples.map(MoodSample::mood).sortedBy(Mood::ordinal)
        val mood = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            sorted[sorted.size / 2 - 1].takeIf { it == sorted[sorted.size / 2] }
        } ?: return null
        return PersonalMoodTrend(mood, samples.size, samples.map(MoodSample::cycleStart).distinct().size)
    }

    private fun phaseFor(
        day: LocalDate,
        cycleStart: LocalDate,
        nextPeriodStart: LocalDate,
        periodLength: Int,
        lutealPhaseDays: Int,
    ): CyclePhase? {
        if (day < cycleStart || !day.isBefore(nextPeriodStart)) return null
        val fertility = fertilityForPeriod(nextPeriodStart, lutealPhaseDays)
        return when {
            day < cycleStart.plusDays(periodLength.toLong()) -> CyclePhase.MENSTRUAL
            day < fertility.fertileStart -> CyclePhase.FOLLICULAR
            !day.isAfter(fertility.fertileEnd) -> CyclePhase.FERTILE
            else -> CyclePhase.LUTEAL
        }
    }

    private fun prediction(backup: CycleBackup, referenceDate: LocalDate): CyclePrediction = CyclePredictor.predict(
        bleedingDays = backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
        defaultCycleLength = backup.settings.cycleLength,
        defaultPeriodLength = backup.settings.periodLength,
        referenceDate = referenceDate,
        cycleLengthOverride = backup.settings.cycleLengthOverride,
        periodLengthOverride = backup.settings.periodLengthOverride,
        activePeriodStart = backup.settings.activePeriodStart,
    )

    private data class MoodSample(val mood: Mood, val cycleStart: LocalDate)

    private const val DEFAULT_LUTEAL_PHASE_DAYS = 14
    private const val FERTILE_DAYS_BEFORE = 5L
    private const val FERTILE_DAYS_AFTER = 1L
    private const val MIN_MOOD_SAMPLES = 3
    private const val MIN_MOOD_CYCLES = 2
    private const val MAX_MOOD_CYCLES = 8
    private const val MIN_CYCLE_DAYS = 15L
}
