package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SymptomPattern(
    val symptom: Symptom,
    val phase: CyclePhase,
    val sampleCount: Int,
    val cycleCount: Int,
)

object PersonalPatterns {
    fun symptomPatterns(backup: CycleBackup): List<SymptomPattern> {
        val starts = CyclePredictor.predict(
            bleedingDays = backup.logs.filter(DayLog::bleeding).mapTo(mutableSetOf(), DayLog::day),
            defaultCycleLength = backup.settings.cycleLength,
            defaultPeriodLength = backup.settings.periodLength,
            cycleLengthOverride = backup.settings.cycleLengthOverride,
            periodLengthOverride = backup.settings.periodLengthOverride,
        ).periodStarts
        val cycles = starts.zipWithNext().takeLast(MAX_CYCLES)
        val observations = cycles.flatMap { (start, next) ->
            backup.logs.asSequence().filter { it.day >= start && it.day < next && it.symptoms.isNotEmpty() }
                .flatMap { log ->
                    val phase = phaseFor(log, start, next, backup.settings.lutealPhaseLength)
                        ?: return@flatMap emptySequence()
                    log.symptoms.asSequence().map { symptom -> Observation(symptom, phase, start) }
                }.toList()
        }
        return observations.groupBy(Observation::symptom).mapNotNull { (symptom, samples) ->
            val strongest = samples.groupBy(Observation::phase).maxWithOrNull(
                compareBy<Map.Entry<CyclePhase, List<Observation>>> { it.value.size }
                    .thenByDescending { it.key.ordinal },
            ) ?: return@mapNotNull null
            val cycleCount = strongest.value.map(Observation::cycleStart).distinct().size
            SymptomPattern(symptom, strongest.key, strongest.value.size, cycleCount)
                .takeIf { it.sampleCount >= MIN_SAMPLES && it.cycleCount >= MIN_CYCLES }
        }.sortedWith(compareByDescending<SymptomPattern> { it.sampleCount }.thenBy { it.symptom.ordinal })
            .take(MAX_PATTERNS)
    }

    private fun phaseFor(log: DayLog, cycleStart: LocalDate, nextPeriod: LocalDate, lutealPhaseDays: Int): CyclePhase? {
        if (log.bleeding) return CyclePhase.MENSTRUAL
        if (ChronoUnit.DAYS.between(cycleStart, nextPeriod) <= lutealPhaseDays) return null
        val fertility = CycleInsights.fertilityForPeriod(nextPeriod, lutealPhaseDays)
        return when {
            log.day < fertility.fertileStart -> CyclePhase.FOLLICULAR
            !log.day.isAfter(fertility.fertileEnd) -> CyclePhase.FERTILE
            else -> CyclePhase.LUTEAL
        }
    }

    private data class Observation(
        val symptom: Symptom,
        val phase: CyclePhase,
        val cycleStart: LocalDate,
    )

    private const val MIN_SAMPLES = 3
    private const val MIN_CYCLES = 2
    private const val MAX_CYCLES = 8
    private const val MAX_PATTERNS = 3
}
