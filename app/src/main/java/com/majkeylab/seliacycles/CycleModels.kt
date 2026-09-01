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

enum class AppPalette { SELIA, ROSE, OCEAN, FOREST, SUNSET, LILAC, CUSTOM }

data class CustomPalette(
    val primaryRgb: Int = 0xF4B400,
    val secondaryRgb: Int = 0xC62828,
    val tertiaryRgb: Int = 0x00897B,
    val entryRgb: Int = 0x1565C0,
) {
    init {
        require(primaryRgb in RGB_RANGE)
        require(secondaryRgb in RGB_RANGE)
        require(tertiaryRgb in RGB_RANGE)
        require(entryRgb in RGB_RANGE)
    }

    private companion object {
        val RGB_RANGE = 0..0xFFFFFF
    }
}

enum class TrackingGoal { TRACK_CYCLE, TRYING_TO_CONCEIVE, AVOID_PREGNANCY }

enum class LifeSituation { REGULAR_CYCLES, PREGNANT, HORMONAL_CONTRACEPTION, PERIMENOPAUSE, MENOPAUSE }

data class UserProfile(
    val age: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val goal: TrackingGoal = TrackingGoal.TRACK_CYCLE,
    val lifeSituation: LifeSituation = LifeSituation.REGULAR_CYCLES,
) {
    init {
        require(age == null || age in MIN_AGE..MAX_AGE)
        require(heightCm == null || heightCm in MIN_HEIGHT_CM..MAX_HEIGHT_CM)
        require(weightKg == null || weightKg.isFinite() && weightKg in DayLog.MIN_WEIGHT_KG..DayLog.MAX_WEIGHT_KG)
    }

    companion object {
        const val MIN_AGE = 8
        const val MAX_AGE = 100
        const val MIN_HEIGHT_CM = 100
        const val MAX_HEIGHT_CM = 250
    }
}

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

    val hasCalendarMarker: Boolean
        get() = spotting || mood != null || symptoms.isNotEmpty() || note.isNotBlank() || weightKg != null ||
            temperatureC != null || sleepHours != null || intimacy != null || cervicalMucus != null ||
            ovulationTest != null || pregnancyTest != null || painLevel != null || energy != null || stress != null ||
            activity != null || medication != null || importedDetails.isNotBlank()

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

fun DayLog.preservePeriodFrom(existing: DayLog?, selectedFlow: Flow = existing?.flow ?: Flow.NONE): DayLog {
    val bleeding = existing?.bleeding == true
    return copy(
        bleeding = bleeding,
        flow = if (bleeding) {
            selectedFlow.takeUnless { it == Flow.NONE } ?: requireNotNull(existing).flow
        } else {
            Flow.NONE
        },
    )
}

fun mergeDayLogs(current: DayLog, incoming: DayLog): DayLog {
    require(current.day == incoming.day)
    val bleeding = current.bleeding || incoming.bleeding
    return current.copy(
        bleeding = bleeding,
        spotting = current.spotting || incoming.spotting,
        flow = when {
            !bleeding -> Flow.NONE
            current.bleeding && current.flow != Flow.UNKNOWN -> current.flow
            incoming.bleeding -> incoming.flow.takeUnless { it == Flow.NONE } ?: Flow.UNKNOWN
            else -> Flow.UNKNOWN
        },
        mood = current.mood ?: incoming.mood,
        symptoms = current.symptoms + incoming.symptoms,
        note = current.note.takeIf(String::isNotBlank) ?: incoming.note,
        weightKg = current.weightKg ?: incoming.weightKg,
        temperatureC = current.temperatureC ?: incoming.temperatureC,
        sleepHours = current.sleepHours ?: incoming.sleepHours,
        intimacy = current.intimacy ?: incoming.intimacy,
        cervicalMucus = current.cervicalMucus ?: incoming.cervicalMucus,
        ovulationTest = current.ovulationTest ?: incoming.ovulationTest,
        pregnancyTest = current.pregnancyTest ?: incoming.pregnancyTest,
        painLevel = current.painLevel ?: incoming.painLevel,
        energy = current.energy ?: incoming.energy,
        stress = current.stress ?: incoming.stress,
        activity = current.activity ?: incoming.activity,
        medication = current.medication ?: incoming.medication,
        importedDetails = current.importedDetails.takeIf(String::isNotBlank) ?: incoming.importedDetails,
    )
}

data class AppSettings(
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val cycleLengthOverride: Int? = null,
    val periodLengthOverride: Int? = null,
    val activePeriodStart: LocalDate? = null,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val predictionsEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderDays: Int = 2,
    val lutealPhaseLength: Int = 14,
    val theme: AppTheme = AppTheme.LIGHT,
    val palette: AppPalette = AppPalette.OCEAN,
    val customPalette: CustomPalette = CustomPalette(),
    val partnerViewEnabled: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val showPhaseGuidance: Boolean = true,
    val showSelfCare: Boolean = true,
    val showCycleDetails: Boolean = true,
    val simpleMode: Boolean = false,
) {
    init {
        require(cycleLength in 15..90)
        require(periodLength in 1..14)
        require(cycleLengthOverride == null || cycleLengthOverride in 15..90)
        require(periodLengthOverride == null || periodLengthOverride in 1..14)
        require(activePeriodStart == null || activePeriodStart in DayLog.MIN_DATE..DayLog.MAX_DATE)
        require(firstDayOfWeek == DayOfWeek.MONDAY || firstDayOfWeek == DayOfWeek.SUNDAY)
        require(reminderDays in 0..14)
        require(lutealPhaseLength in 7..19)
    }

    val canPredictPeriods: Boolean
        get() = predictionsEnabled && profile.lifeSituation != LifeSituation.PREGNANT &&
            profile.lifeSituation != LifeSituation.MENOPAUSE

    val canEstimateFertility: Boolean
        get() = canPredictPeriods && profile.lifeSituation != LifeSituation.HORMONAL_CONTRACEPTION &&
            profile.lifeSituation != LifeSituation.PERIMENOPAUSE
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
