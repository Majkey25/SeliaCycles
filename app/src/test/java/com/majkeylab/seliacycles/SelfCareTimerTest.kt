package com.majkeylab.seliacycles

import kotlin.test.Test
import kotlin.test.assertEquals

class SelfCareTimerTest {
    @Test
    fun `remaining time uses the elapsed target and never becomes negative`() {
        assertEquals(3, SelfCareTimer.remainingSeconds(targetMillis = 3_000, nowMillis = 0))
        assertEquals(2, SelfCareTimer.remainingSeconds(targetMillis = 3_000, nowMillis = 1_001))
        assertEquals(0, SelfCareTimer.remainingSeconds(targetMillis = 3_000, nowMillis = 3_500))
    }
}
