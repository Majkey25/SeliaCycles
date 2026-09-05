package com.majkeylab.seliacycles

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderPolicyTest {
    @Test
    fun `delayed work notifies once before or on the expected day`() {
        val periodKey = 20_000L

        assertTrue(shouldNotifyPeriod(3, 3, periodKey, lastNotifiedKey = null))
        assertTrue(shouldNotifyPeriod(0, 3, periodKey, lastNotifiedKey = null))
        assertFalse(shouldNotifyPeriod(-1, 3, periodKey, lastNotifiedKey = null))
        assertFalse(shouldNotifyPeriod(4, 3, periodKey, lastNotifiedKey = null))
        assertFalse(shouldNotifyPeriod(2, 3, periodKey, lastNotifiedKey = periodKey))
    }
}
