package com.majkeylab.seliacycles

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarSyncModelsTest {
    @Test
    fun defaultPartnerPayloadExcludesPrivateDetails() {
        val log = DayLog(
            day = LocalDate.of(2026, 8, 28),
            bleeding = true,
            flow = Flow.MEDIUM,
            mood = Mood.GOOD,
            note = "private",
            weightKg = 68.0,
            temperatureC = 36.6,
            intimacy = Intimacy.PROTECTED,
        )

        val payload = log.toPartnerPayload()

        assertEquals(setOf("day", "bleeding", "flow"), payload.keys)
        assertFalse(payload.values.contains("private"))
    }

    @Test
    fun inviteTokenUsesSixteenBytesWithoutPadding() {
        val token = PartnerInviteToken.encode(ByteArray(16) { it.toByte() })

        assertEquals(22, token.length)
        assertTrue(token.matches(Regex("^[A-Za-z0-9_-]{22}$")))
        assertEquals(token, PartnerInviteToken.normalize(token))
    }

    @Test
    fun rejectsMalformedInviteToken() {
        assertFailsWith<IllegalArgumentException> { PartnerInviteToken.normalize("short") }
        assertFailsWith<IllegalArgumentException> { PartnerInviteToken.normalize("AbCdEfGhIjKlMnOpQrStU!") }
    }
}
