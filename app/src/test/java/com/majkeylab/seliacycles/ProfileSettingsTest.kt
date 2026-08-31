package com.majkeylab.seliacycles

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ProfileSettingsTest {
    @Test
    fun `luteal phase defaults to fourteen and accepts seven to nineteen days`() {
        assertTrue(AppSettings().lutealPhaseLength == 14)
        AppSettings(lutealPhaseLength = 7)
        AppSettings(lutealPhaseLength = 19)
        assertFailsWith<IllegalArgumentException> { AppSettings(lutealPhaseLength = 6) }
        assertFailsWith<IllegalArgumentException> { AppSettings(lutealPhaseLength = 20) }
    }

    @Test
    fun `profile accepts inclusive body boundaries`() {
        UserProfile(age = 8, heightCm = 100, weightKg = 15.0)
        UserProfile(age = 100, heightCm = 250, weightKg = 400.0)

        assertFailsWith<IllegalArgumentException> { UserProfile(age = 7) }
        assertFailsWith<IllegalArgumentException> { UserProfile(age = 101) }
        assertFailsWith<IllegalArgumentException> { UserProfile(heightCm = 99) }
        assertFailsWith<IllegalArgumentException> { UserProfile(heightCm = 251) }
        assertFailsWith<IllegalArgumentException> { UserProfile(weightKg = 14.9) }
        assertFailsWith<IllegalArgumentException> { UserProfile(weightKg = 400.1) }
    }

    @Test
    fun `life situation gates only medically valid estimates`() {
        assertTrue(settings(LifeSituation.REGULAR_CYCLES).canPredictPeriods)
        assertTrue(settings(LifeSituation.REGULAR_CYCLES).canEstimateFertility)

        assertFalse(settings(LifeSituation.PREGNANT).canPredictPeriods)
        assertFalse(settings(LifeSituation.PREGNANT).canEstimateFertility)
        assertFalse(settings(LifeSituation.MENOPAUSE).canPredictPeriods)
        assertFalse(settings(LifeSituation.MENOPAUSE).canEstimateFertility)

        assertTrue(settings(LifeSituation.HORMONAL_CONTRACEPTION).canPredictPeriods)
        assertFalse(settings(LifeSituation.HORMONAL_CONTRACEPTION).canEstimateFertility)
        assertTrue(settings(LifeSituation.PERIMENOPAUSE).canPredictPeriods)
        assertFalse(settings(LifeSituation.PERIMENOPAUSE).canEstimateFertility)
    }

    @Test
    fun `tracking goal never changes calendar math availability`() {
        TrackingGoal.entries.forEach { goal ->
            val settings = AppSettings(profile = UserProfile(goal = goal))
            assertTrue(settings.canPredictPeriods)
            assertTrue(settings.canEstimateFertility)
        }
    }

    @Test
    fun `legacy simple mode flag no longer hides fertility estimates`() {
        val settings = AppSettings(simpleMode = true)

        assertTrue(settings.canPredictPeriods)
        assertTrue(settings.canEstimateFertility)
    }

    private fun settings(situation: LifeSituation): AppSettings = AppSettings(
        profile = UserProfile(lifeSituation = situation),
    )
}
