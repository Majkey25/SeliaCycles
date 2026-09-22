package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfCarePlanTest {
    private val day = LocalDate.of(2026, 9, 2)
    private val insight = DailyCycleInsight(null, CyclePhase.MENSTRUAL, null,
        FertilityStatus.UNAVAILABLE, null, 2, MenstrualStage.EARLY)

    @Test fun `headache overrides the generic phase list`() {
        val plan = selfCarePlan(insight, DayLog(day, symptoms = setOf(Symptom.HEADACHE)), day)
        assertEquals(SelfCareActivity.HYDRATION, plan.activities.first())
        assertTrue(plan.fromEntries)
    }

    @Test fun `backache brings gentle back care forward without duplicates`() {
        val plan = selfCarePlan(insight, DayLog(day, symptoms = setOf(Symptom.BACKACHE)), day)
        assertEquals(SelfCareActivity.BACK_STRETCH, plan.activities.first())
        assertEquals(plan.activities.distinct(), plan.activities)
    }

    @Test fun `severe pain prioritizes help rather than exercise`() {
        val plan = selfCarePlan(insight, DayLog(day, painLevel = 8), day)
        assertTrue(plan.needsMedicalAdvice)
        assertFalse(SelfCareActivity.MOVEMENT in plan.activities)
        assertFalse(SelfCareActivity.MASSAGE in plan.activities)
    }

    @Test fun `pregnancy with bleeding is not ordinary menstrual self care`() {
        val log = DayLog(day, bleeding = true, flow = Flow.LIGHT)
        val plan = selfCarePlan(insight, log, day, LifeSituation.PREGNANT)
        assertTrue(plan.needsMedicalAdvice)
        assertFalse(SelfCareActivity.KNEES_TO_CHEST in plan.activities)
    }

    @Test fun `day two includes banana oats and food tips have no timer`() {
        val plan = selfCarePlan(insight, null, day)
        assertTrue(SelfCareActivity.BANANA_OATS in plan.activities.take(4))
        assertEquals(0, SelfCareActivity.BANANA_OATS.minutes)
        val next = selfCarePlan(insight.copy(menstrualDay = 3), null, day.plusDays(1))
        assertTrue(SelfCareActivity.CALCIUM_SNACK in next.activities.take(4))
        assertFalse(plan.fromEntries)
    }

    @Test fun `bloating does not suggest a lentil and bean meal`() {
        val plan = selfCarePlan(insight, DayLog(day, symptoms = setOf(Symptom.BLOATING)), day)
        assertEquals(SelfCareActivity.SMALL_MEAL, plan.activities.first())
        assertFalse(SelfCareActivity.IRON_MEAL in plan.activities)
    }

    @Test fun `mood and energy reorder care but another day does not`() {
        val low = DayLog(day, mood = Mood.LOW)
        assertEquals(SelfCareActivity.BREATHING, selfCarePlan(insight, low, day).activities.first())
        assertEquals(SelfCareActivity.REST, selfCarePlan(insight, low.copy(energy = WellbeingLevel.LOW), day).activities.first())
        assertFalse(selfCarePlan(insight, low.copy(day = day.minusDays(1)), day).fromEntries)
    }

    @Test fun `every recorded symptom changes the list without duplication`() {
        Symptom.entries.forEach { symptom ->
            val plan = selfCarePlan(insight, DayLog(day, symptoms = setOf(symptom)), day)
            assertTrue(plan.fromEntries, symptom.name)
            assertEquals(plan.activities.distinct(), plan.activities)
        }
    }

    @Test fun `unknown cycles and pregnancy do not claim a menstrual phase`() {
        assertFalse(selfCarePlan(insight.copy(phase = null), null, day).showPhase)
        assertFalse(selfCarePlan(insight, DayLog(day, pregnancyTest = TestResult.POSITIVE), day).showPhase)
        assertTrue(selfCarePlan(insight, null, day).showPhase)
        assertTrue(selfCarePlan(insight, DayLog(day, spotting = true), day, LifeSituation.MENOPAUSE).needsMedicalAdvice)
    }

    @Test fun `prior positive test informs care until a later valid result`() {
        val positive = DayLog(day.minusDays(2), pregnancyTest = TestResult.POSITIVE)
        val bleeding = DayLog(day, bleeding = true, flow = Flow.LIGHT)
        assertEquals(SelfCareAlert.PREGNANCY, selfCarePlan(insight, bleeding, day, history = listOf(positive)).alert)
        val negative = DayLog(day.minusDays(1), pregnancyTest = TestResult.NEGATIVE)
        assertFalse(selfCarePlan(insight, bleeding, day, history = listOf(positive, negative)).needsMedicalAdvice)
        assertFalse(selfCarePlan(insight, bleeding, day, history = listOf(positive.copy(day = day.plusDays(1)))).needsMedicalAdvice)
    }
}
