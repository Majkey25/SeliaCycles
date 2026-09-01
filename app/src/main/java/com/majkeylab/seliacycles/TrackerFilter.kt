package com.majkeylab.seliacycles

enum class TrackerFilter(private val symptom: Symptom? = null) {
    CRAMPS(Symptom.CRAMPS),
    HEADACHE(Symptom.HEADACHE),
    BLOATING(Symptom.BLOATING),
    TENDER_BREASTS(Symptom.TENDER_BREASTS),
    FATIGUE(Symptom.FATIGUE),
    ACNE(Symptom.ACNE),
    CRAVINGS(Symptom.CRAVINGS),
    BACKACHE(Symptom.BACKACHE),
    SPOTTING,
    MOOD,
    PAIN,
    ENERGY,
    STRESS,
    INTIMACY,
    TESTS,
    NOTES;

    fun matches(log: DayLog): Boolean = symptom?.let { it in log.symptoms } ?: when (this) {
        SPOTTING -> log.spotting
        MOOD -> log.mood != null
        PAIN -> log.painLevel != null
        ENERGY -> log.energy != null
        STRESS -> log.stress != null
        INTIMACY -> log.intimacy != null
        TESTS -> log.ovulationTest != null || log.pregnancyTest != null
        NOTES -> log.note.isNotBlank() || log.importedDetails.isNotBlank()
        else -> false
    }

    companion object {
        const val MAX_SELECTED = 3

        fun availableFilters(logs: List<DayLog>): List<TrackerFilter> = entries.filter { filter ->
            logs.any(filter::matches)
        }

        fun toggleSelection(selected: Set<TrackerFilter>, filter: TrackerFilter): Set<TrackerFilter> = when {
            filter in selected -> selected - filter
            selected.size == MAX_SELECTED -> selected
            else -> selected + filter
        }

        fun matchesAny(filters: Set<TrackerFilter>, log: DayLog): Boolean = filters.any { it.matches(log) }
    }
}
