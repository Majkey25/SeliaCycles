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
    fun canEstimateFertility(backup: CycleBackup, prediction: CyclePrediction): Boolean =
        backup.settings.canEstimateFertility && prediction.averageCycleLength > backup.settings.lutealPhaseLength

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
        val dynamic = prediction.estimatedPeriodStarts.mapNotNull { start ->
            val month = YearMonth.from(start)
            val snapshot = snapshots[month]
            val include = snapshot == null || prediction.periodStarts.any { actual ->
                YearMonth.from(actual) == month && ChronoUnit.DAYS.between(actual, start) >= MIN_CYCLE_DAYS
            } && start != snapshot.periodStart
            if (!include) return@mapNotNull null
            val window = prediction.uncertaintyWindow(start)
            PeriodEstimate(
                start = start,
                endExclusive = start.plusDays(prediction.averagePeriodLength.toLong()),
                earliestStart = window.first,
                latestStart = window.second,
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
        return fertilityEstimates(
            backup, prediction(backup, referenceDate), calendarPeriodEstimates(backup, snapshots, referenceDate), referenceDate,
        )
    }

    internal fun fertilityEstimates(
        backup: CycleBackup,
        prediction: CyclePrediction,
        estimates: List<PeriodEstimate>,
        referenceDate: LocalDate,
    ): List<FertilityEstimate> {
        if (!canEstimateFertility(backup, prediction)) return emptyList()
        val futureRecorded = prediction.periodStarts.filter { it.isAfter(referenceDate) }
        val recordedMonths = futureRecorded.mapTo(mutableSetOf(), YearMonth::from)
        val estimated = estimates.map(PeriodEstimate::start)
            .filterNot { YearMonth.from(it) in recordedMonths }
        return (estimated + futureRecorded).distinct().sorted().mapNotNull { start ->
            if (start in futureRecorded) {
                val previous = prediction.periodStarts.lastOrNull { it < start }
                if (previous != null && ChronoUnit.DAYS.between(previous, start) <= backup.settings.lutealPhaseLength) {
                    return@mapNotNull null
                }
            }
            fertilityForPeriod(start, backup.settings.lutealPhaseLength)
        }
    }

    fun forDate(
        backup: CycleBackup,
        snapshots: Map<YearMonth, ForecastSnapshot>,
        date: LocalDate = LocalDate.now(),
        referenceDate: LocalDate = date,
    ): DailyCycleInsight {
        val prediction = prediction(backup, referenceDate)
        val estimates = calendarPeriodEstimates(backup, snapshots, referenceDate)
        val coveringEstimates = estimates.filter { date >= it.start && date < it.endExclusive }
        val estimatedPeriod = coveringEstimates.firstOrNull { it.origin == EstimateOrigin.CURRENT }
            ?: coveringEstimates.firstOrNull()
        val matchedSnapshot = estimatedPeriod?.takeIf { it.origin != EstimateOrigin.CURRENT }?.let { estimate ->
            snapshots.values.firstOrNull { it.periodStart == estimate.start }
        }?.takeIf { CycleAnalysis.closestRecordedStart(it, prediction.periodStarts) != null }
        val unresolvedEstimate = estimatedPeriod.takeIf { backup.settings.canPredictPeriods && matchedSnapshot == null }
        val unconfirmedStart = prediction.periodStarts.lastOrNull()
            ?.takeIf { it <= date && date <= referenceDate && backup.settings.canPredictPeriods }
            ?.plusDays(prediction.averageCycleLength.toLong())
            ?.takeIf { it <= date }
        val recordedBleeding = backup.logs.any { it.day == date && it.bleeding }
        val elapsedCycle = !recordedBleeding && unconfirmedStart?.let { start ->
            date > start.plusDays(maxOf(prediction.averagePeriodLength - 1, prediction.uncertaintyDays).toLong())
        } == true
        val overduePrediction = estimates.firstOrNull { estimate ->
            backup.settings.canPredictPeriods && estimate.origin == EstimateOrigin.CURRENT && estimate.start < date &&
                date <= maxOf(estimate.endExclusive.minusDays(1), estimate.latestStart ?: estimate.start)
        }?.start
        val futurePeriod = if (!backup.settings.canPredictPeriods) null else {
            prediction.periodStarts.firstOrNull { it.isAfter(date) }
                ?: estimates.firstOrNull { it.start.isAfter(date) }?.start
        }
        val displayedPeriod = unconfirmedStart ?: overduePrediction ?: unresolvedEstimate?.start ?: futurePeriod
        val activeStart = prediction.periodStarts.lastOrNull { !it.isAfter(date) }
        val fertilityEstimates = fertilityEstimates(backup, prediction, estimates, referenceDate)
        val fertility = fertilityEstimates.firstOrNull { it.ovulation == date }
            ?: fertilityEstimates.firstOrNull { date in it.fertileStart..it.fertileEnd }
            ?: fertilityEstimates.firstOrNull { it.periodStart == futurePeriod }
        val phase = when {
            recordedBleeding -> CyclePhase.MENSTRUAL
            elapsedCycle -> null
            overduePrediction != null && unresolvedEstimate?.origin != EstimateOrigin.CURRENT -> null
            unresolvedEstimate != null -> CyclePhase.MENSTRUAL
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
            nextPeriodStart = displayedPeriod,
            phase = phase,
            fertility = fertility,
            fertilityStatus = when {
                elapsedCycle -> FertilityStatus.UNAVAILABLE
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
        if (day < cycleStart.plusDays(periodLength.toLong())) return CyclePhase.MENSTRUAL
        if (ChronoUnit.DAYS.between(cycleStart, nextPeriodStart) <= lutealPhaseDays) return null
        val fertility = fertilityForPeriod(nextPeriodStart, lutealPhaseDays)
        return when {
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
