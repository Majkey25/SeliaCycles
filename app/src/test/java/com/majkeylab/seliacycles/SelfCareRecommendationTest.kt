package com.majkeylab.seliacycles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelfCareRecommendationTest {
    @Test
    fun `care recommendations are phase specific and duplicate free`() {
        val recommendations = CyclePhase.entries.associateWith(::recommendedSelfCareActivities)

        assertEquals(SelfCareActivity.HEAT, recommendations.getValue(CyclePhase.MENSTRUAL).first())
        assertTrue(SelfCareActivity.REST in recommendations.getValue(CyclePhase.LUTEAL))
        assertEquals(recommendations.size, recommendations.values.distinct().size)
        assertTrue(recommendations.values.all { activities -> activities.size == activities.distinct().size })
    }
}
