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

data class DayLog(
    val day: LocalDate,
    val bleeding: Boolean = false,
    val flow: Flow = Flow.NONE,
    val mood: Mood? = null,
    val symptoms: Set<Symptom> = emptySet(),
    val note: String = "",
) {
    init {
        require(day in MIN_DATE..MAX_DATE)
        require(note.length <= MAX_NOTE_LENGTH)
        require((bleeding && flow != Flow.NONE) || (!bleeding && flow == Flow.NONE))
    }

    val isEmpty: Boolean
        get() = !bleeding && mood == null && symptoms.isEmpty() && note.isBlank()

    companion object {
        const val MAX_NOTE_LENGTH = 1_000
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
