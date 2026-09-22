package com.majkeylab.seliacycles

import java.time.LocalDate

internal data class SelfCarePlan(
    val activities: List<SelfCareActivity>,
    val fromEntries: Boolean = false,
    val alert: SelfCareAlert? = null,
    val showPhase: Boolean = false,
) {
    val needsMedicalAdvice get() = alert != null
}

internal enum class SelfCareAlert { SEVERE_PAIN, PREGNANCY, MENOPAUSE }

internal fun selfCarePlan(
    insight: DailyCycleInsight,
    log: DayLog?,
    day: LocalDate,
    situation: LifeSituation = LifeSituation.REGULAR_CYCLES,
    history: List<DayLog> = emptyList(),
): SelfCarePlan {
    val entry = log?.takeIf { it.day == day }
    val symptoms = entry?.symptoms.orEmpty()
    val latestTest = entry?.pregnancyTest?.takeIf { it != TestResult.INVALID } ?: history
        .filter { it.day < day && it.pregnancyTest != null && it.pregnancyTest != TestResult.INVALID }
        .maxByOrNull(DayLog::day)?.pregnancyTest
    val pregnant = situation == LifeSituation.PREGNANT || latestTest == TestResult.POSITIVE
    val bleeding = entry?.confirmedBleeding == true || entry?.spotting == true
    val alert = when {
        pregnant && (bleeding || (entry?.painLevel ?: 0) > 0 ||
            Symptom.HEADACHE in symptoms || Symptom.CRAMPS in symptoms) -> SelfCareAlert.PREGNANCY
        situation == LifeSituation.MENOPAUSE && bleeding -> SelfCareAlert.MENOPAUSE
        (entry?.painLevel ?: 0) >= 7 -> SelfCareAlert.SEVERE_PAIN
        else -> null
    }
    if (alert != null) return SelfCarePlan(listOf(SelfCareActivity.REST, SelfCareActivity.BREATHING), true, alert)

    val priorities = buildList {
        if (Symptom.HEADACHE in symptoms) addAll(listOf(SelfCareActivity.HYDRATION, SelfCareActivity.SCREEN_BREAK, SelfCareActivity.SMALL_MEAL))
        if (Symptom.BACKACHE in symptoms) addAll(listOf(SelfCareActivity.BACK_STRETCH, SelfCareActivity.HEAT))
        if (Symptom.CRAMPS in symptoms || (entry?.painLevel ?: 0) > 0) {
            addAll(listOf(SelfCareActivity.HEAT, SelfCareActivity.PELVIC_ROCKING, SelfCareActivity.MASSAGE))
        }
        if (Symptom.BLOATING in symptoms) addAll(listOf(SelfCareActivity.SMALL_MEAL, SelfCareActivity.WALK, SelfCareActivity.HYDRATION))
        if (Symptom.FATIGUE in symptoms || entry?.energy == WellbeingLevel.LOW || (entry?.sleepHours ?: 8.0) < 6.0) {
            addAll(listOf(SelfCareActivity.REST, SelfCareActivity.BANANA_OATS, SelfCareActivity.SLEEP_ROUTINE))
        }
        if (entry?.mood in setOf(Mood.LOW, Mood.BAD) || entry?.stress == WellbeingLevel.HIGH) {
            addAll(listOf(SelfCareActivity.BREATHING, SelfCareActivity.WALK, SelfCareActivity.MUSCLE_RELAXATION))
        }
        if (Symptom.CRAVINGS in symptoms) addAll(listOf(SelfCareActivity.BANANA_OATS, SelfCareActivity.CALCIUM_SNACK))
        if (Symptom.TENDER_BREASTS in symptoms) add(SelfCareActivity.BREAST_COMFORT)
        if (Symptom.ACNE in symptoms) add(SelfCareActivity.SKIN_CARE)
    }
    // Rotate ordinary meal ideas for variety, not because a food treats a particular cycle day.
    val phase = if (pregnant || situation == LifeSituation.MENOPAUSE) null else insight.phase
    val foods = listOf(SelfCareActivity.IRON_MEAL, SelfCareActivity.BANANA_OATS, SelfCareActivity.CALCIUM_SNACK)
    val food = if (Symptom.BLOATING in symptoms) SelfCareActivity.SMALL_MEAL else {
        foods[Math.floorMod((insight.menstrualDay.takeIf { phase != null } ?: day.dayOfYear) - 1, foods.size)]
    }
    val base = recommendedSelfCareActivities(phase, insight.menstrualStage)
    val ordered = (priorities + base.take(1) + food + base.drop(1) + SelfCareActivity.entries).distinct().filterNot {
        (Symptom.BLOATING in symptoms && it == SelfCareActivity.IRON_MEAL) ||
            (pregnant && it in setOf(SelfCareActivity.HEAT, SelfCareActivity.MASSAGE,
                SelfCareActivity.BACK_STRETCH, SelfCareActivity.KNEES_TO_CHEST, SelfCareActivity.PELVIC_ROCKING))
    }
    return SelfCarePlan(ordered, priorities.isNotEmpty(), showPhase = phase != null)
}
