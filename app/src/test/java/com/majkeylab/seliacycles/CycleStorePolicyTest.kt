package com.majkeylab.seliacycles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CycleStorePolicyTest {
    @Test
    fun `capacity allows updates but rejects a new day past the limit`() {
        assertTrue(hasLogCapacity(CycleBackup.MAX_LOGS - 1L, replacing = false))
        assertTrue(hasLogCapacity(CycleBackup.MAX_LOGS.toLong(), replacing = true))
        assertFalse(hasLogCapacity(CycleBackup.MAX_LOGS.toLong(), replacing = false))
    }

    @Test
    fun `imported settings cannot change local partner sharing`() {
        val currentPrivate = AppSettings(partnerViewEnabled = false)
        val currentShared = AppSettings(partnerViewEnabled = true)
        val incoming = AppSettings(cycleLength = 31, partnerViewEnabled = true)

        val privateResult = mergedTransferSettings(currentPrivate, incoming)
        val sharedResult = mergedTransferSettings(currentShared, incoming.copy(partnerViewEnabled = false))

        assertEquals(31, privateResult.cycleLength)
        assertFalse(privateResult.partnerViewEnabled)
        assertTrue(sharedResult.partnerViewEnabled)
    }

    @Test
    fun `imported settings cannot change independent local reminder consent`() {
        val settings = listOf(
            AppSettings(),
            AppSettings(reminderEnabled = true),
            AppSettings(contraceptionReminderEnabled = true),
            AppSettings(reminderEnabled = true, contraceptionReminderEnabled = true),
        )
        settings.forEach { current ->
            val incoming = AppSettings(
                cycleLength = 31,
                reminderEnabled = !current.reminderEnabled,
                contraceptionReminderEnabled = !current.contraceptionReminderEnabled,
            )
            val merged = mergedTransferSettings(current, incoming)

            assertEquals(current.reminderEnabled, merged.reminderEnabled)
            assertEquals(current.contraceptionReminderEnabled, merged.contraceptionReminderEnabled)
            assertEquals(31, merged.cycleLength)
        }
    }
}
