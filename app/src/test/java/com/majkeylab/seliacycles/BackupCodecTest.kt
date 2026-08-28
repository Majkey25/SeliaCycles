package com.majkeylab.seliacycles

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackupCodecTest {
    @Test
    fun encryptedBackupRoundTripsWithoutPlaintext() {
        val backup = CycleBackup(
            logs = listOf(
                DayLog(
                    day = LocalDate.of(2026, 8, 28),
                    bleeding = true,
                    flow = Flow.MEDIUM,
                    mood = Mood.OKAY,
                    symptoms = setOf(Symptom.CRAMPS, Symptom.FATIGUE),
                    note = "Private note",
                ),
                DayLog(
                    day = LocalDate.of(2026, 8, 29),
                    symptoms = setOf(Symptom.FATIGUE),
                ),
            ),
            settings = AppSettings(firstDayOfWeek = DayOfWeek.MONDAY),
        )

        val encrypted = BackupCodec.encrypt(backup, "correct horse".toCharArray())

        assertEquals(backup, BackupCodec.decrypt(encrypted, "correct horse".toCharArray()))
        assert(!encrypted.decodeToString().contains("Private note"))
    }

    @Test
    fun wrongPasswordFailsWithoutPartialData() {
        val encrypted = BackupCodec.encrypt(CycleBackup(), "correct horse".toCharArray())

        assertFailsWith<BackupFormatException> {
            BackupCodec.decrypt(encrypted, "wrong password".toCharArray())
        }
    }
}
