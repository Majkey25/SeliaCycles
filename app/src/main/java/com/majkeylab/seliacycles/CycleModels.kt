package com.majkeylab.seliacycles

import java.time.DayOfWeek
import java.time.LocalDate

enum class Flow { NONE, UNKNOWN, LIGHT, MEDIUM, HEAVY }

enum class Mood { GREAT, GOOD, OKAY, LOW, BAD }

enum class Symptom {
    CRAMPS,
    HEADACHE,
    BLOATING,
    TENDER_BREASTS,
    FATIGUE,
    ACNE,
    CRAVINGS,
    BACKACHE,
}

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class AppPalette { SELIA, ROSE, OCEAN }

enum class Intimacy { SEX, PROTECTED }

enum class CervicalMucus { DRY, STICKY, CREAMY, WATERY, EGG_WHITE, UNUSUAL }

enum class TestResult { NEGATIVE, POSITIVE, INVALID }

enum class WellbeingLevel { LOW, MEDIUM, HIGH }

enum class ActivityLevel { LIGHT, MODERATE, INTENSE }

enum class MedicationStatus { TAKEN, MISSED }

data class DayLog(
    val day: LocalDate,
    val bleeding: Boolean = false,
    val spotting: Boolean = false,
    val flow: Flow = Flow.NONE,
    val mood: Mood? = null,
    val symptoms: Set<Symptom> = emptySet(),
    val note: String = "",
    val weightKg: Double? = null,
    val temperatureC: Double? = null,
    val sleepHours: Double? = null,
    val intimacy: Intimacy? = null,
    val cervicalMucus: CervicalMucus? = null,
    val ovulationTest: TestResult? = null,
    val pregnancyTest: TestResult? = null,
    val painLevel: Int? = null,
    val energy: WellbeingLevel? = null,
    val stress: WellbeingLevel? = null,
    val activity: ActivityLevel? = null,
    val medication: MedicationStatus? = null,
    val importedDetails: String = "",
) {
    init {
        require(day in MIN_DATE..MAX_DATE)
        require(note.length <= MAX_NOTE_LENGTH)
        require(importedDetails.length <= MAX_IMPORTED_DETAILS_LENGTH)
        require(weightKg == null || weightKg.isFinite() && weightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG)
        require(temperatureC == null || temperatureC.isFinite() && temperatureC in MIN_TEMPERATURE_C..MAX_TEMPERATURE_C)
        require(sleepHours == null || sleepHours.isFinite() && sleepHours in 0.0..24.0)
        require(painLevel == null || painLevel in 0..10)
        require((bleeding && flow != Flow.NONE) || (!bleeding && flow == Flow.NONE))
    }

    val isEmpty: Boolean
        get() = !bleeding && !spotting && mood == null && symptoms.isEmpty() && note.isBlank() && weightKg == null &&
            temperatureC == null && sleepHours == null && intimacy == null && cervicalMucus == null &&
            ovulationTest == null && pregnancyTest == null && painLevel == null && energy == null && stress == null &&
            activity == null && medication == null && importedDetails.isBlank()

    companion object {
        const val MAX_NOTE_LENGTH = 1_000
        const val MAX_IMPORTED_DETAILS_LENGTH = 2_000
        const val MIN_WEIGHT_KG = 15.0
        const val MAX_WEIGHT_KG = 400.0
        const val MIN_TEMPERATURE_C = 30.0
        const val MAX_TEMPERATURE_C = 45.0
        val MIN_DATE: LocalDate = LocalDate.of(1900, 1, 1)
        val MAX_DATE: LocalDate = LocalDate.of(2100, 12, 31)
    }
}

data class AppSettings(
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val predictionsEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderDays: Int = 2,
    val theme: AppTheme = AppTheme.SYSTEM,
    val palette: AppPalette = AppPalette.SELIA,
    val partnerViewEnabled: Boolean = false,
) {
    init {
        require(cycleLength in 15..90)
        require(periodLength in 1..14)
        require(firstDayOfWeek == DayOfWeek.MONDAY || firstDayOfWeek == DayOfWeek.SUNDAY)
        require(reminderDays in 0..14)
    }
}

data class CycleBackup(
    val logs: List<DayLog> = emptyList(),
    val settings: AppSettings = AppSettings(),
) {
    init {
        require(logs.size <= MAX_LOGS)
        require(logs.map(DayLog::day).distinct().size == logs.size)
    }

    companion object {
        // ponytail: 10,000 daily records cover over 27 years; page archives only if users hit this ceiling.
        const val MAX_LOGS = 10_000
    }
}
