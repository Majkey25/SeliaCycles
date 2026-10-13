package com.majkeylab.seliacycles

import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContraceptionTest {
    private val day = LocalDate.of(2026, 10, 3)

    @Test fun `contraception is a calendar record and remains separate from other medication`() {
        val log = DayLog(day, contraception = ContraceptionStatus.TAKEN)
        assertFalse(log.isEmpty)
        assertTrue(log.hasCalendarMarker)
        assertTrue(TrackerFilter.CONTRACEPTION.matches(log))
        assertEquals(ContraceptionStatus.TAKEN, mergeDayLogs(DayLog(day, medication = MedicationStatus.MISSED), log).contraception)
        assertEquals(MedicationStatus.MISSED, mergeDayLogs(DayLog(day, medication = MedicationStatus.MISSED), log).medication)
    }

    @Test fun `local backup preserves contraception settings and records`() {
        val transfer = SeliaTransfer(CycleBackup(
            listOf(DayLog(day, contraception = ContraceptionStatus.PAUSE)),
            AppSettings(contraceptionReminderEnabled = true, contraceptionReminderMinute = 9 * 60 + 15),
        ), emptyList())
        assertEquals(transfer, SeliaBackupCodec.decode(SeliaBackupCodec.encode(transfer)))
    }

    @Test fun `reminder timing follows local days through daylight saving and skips handled days`() {
        val settings = AppSettings(profile = UserProfile(lifeSituation = LifeSituation.HORMONAL_CONTRACEPTION),
            contraceptionReminderEnabled = true, contraceptionReminderMinute = 2 * 60 + 30)
        val before = ZonedDateTime.parse("2026-03-29T00:30:00+01:00[Europe/Prague]")
        assertEquals(3, nextContraceptionReminder(settings, before, false)?.hour)
        assertEquals(30, nextContraceptionReminder(settings, before, true)?.dayOfMonth)
        assertEquals(null, nextContraceptionReminder(settings.copy(contraceptionReminderEnabled = false), before, false))
        assertEquals(null, nextContraceptionReminder(settings.copy(profile = UserProfile()), before, false))
    }

    @Test fun `reload keeps an already scheduled reminder that Android has delayed`() {
        val settings = AppSettings(profile = UserProfile(lifeSituation = LifeSituation.HORMONAL_CONTRACEPTION),
            contraceptionReminderEnabled = true, contraceptionReminderMinute = 9 * 60)
        val now = ZonedDateTime.parse("2026-10-03T09:10:00+02:00[Europe/Prague]")
        assertEquals(day, nextContraceptionReminder(settings, now, false, day)?.toLocalDate())
        assertEquals(day.plusDays(1), nextContraceptionReminder(settings, now, true, day)?.toLocalDate())
    }

    @Test fun `blocked notifications keep future alarms without replaying overdue alarms`() {
        val settings = AppSettings(profile = UserProfile(lifeSituation = LifeSituation.HORMONAL_CONTRACEPTION),
            contraceptionReminderEnabled = true, contraceptionReminderMinute = 9 * 60)
        val now = ZonedDateTime.parse("2026-10-03T09:10:00+02:00[Europe/Prague]")
        val scheduled = now.withHour(9).withMinute(0)
        assertEquals(scheduled.plusDays(1), nextContraceptionReminder(settings, now, false, day, notificationsEnabled = false))
        assertEquals(scheduled, nextContraceptionReminder(settings, now, false, day, notificationsEnabled = true))
        assertEquals(scheduled, nextContraceptionReminder(settings, now.minusHours(1), false, day, notificationsEnabled = false))
    }

    @Test fun `delivery remains due when an autumn clock overlap moves local time backwards`() {
        val minute = 2 * 60 + 30
        assertFalse(isContraceptionReminderDue(minute, ZonedDateTime.parse("2026-10-25T02:10:00+02:00[Europe/Prague]")))
        assertTrue(isContraceptionReminderDue(minute, ZonedDateTime.parse("2026-10-25T02:30:00+02:00[Europe/Prague]")))
        assertTrue(isContraceptionReminderDue(minute, ZonedDateTime.parse("2026-10-25T02:10:00+01:00[Europe/Prague]")))
    }

    @Test fun `delivery waits for the adjusted alarm instant during a spring clock gap`() {
        val minute = 2 * 60 + 30
        assertFalse(isContraceptionReminderDue(minute, ZonedDateTime.parse("2026-03-29T03:29:59+02:00[Europe/Prague]")))
        assertTrue(isContraceptionReminderDue(minute, ZonedDateTime.parse("2026-03-29T03:30:00+02:00[Europe/Prague]")))
    }
}
