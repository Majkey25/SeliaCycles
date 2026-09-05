package com.majkeylab.seliacycles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ProfileIntegrationTest {
    private val first = "2c0d7569-a923-41fb-bb5d-793798249f44"
    private val second = "93455a59-6929-4ca5-b75a-a8f9ec86c623"

    @Test
    fun `original profile preserves calendar and reminder identities`() {
        assertEquals("selia://calendar-mirror/", calendarMirrorUriPrefix(LocalProfiles.DEFAULT_ID))
        assertEquals("calendar-mirror-id", calendarMirrorSelectionFile(LocalProfiles.DEFAULT_ID))
        assertEquals("selia-cycles-period-reminder", reminderWorkName(LocalProfiles.DEFAULT_ID))
        assertEquals("reminder-state", reminderStateName(LocalProfiles.DEFAULT_ID))
        assertNull(reminderNotificationTag(LocalProfiles.DEFAULT_ID))
    }

    @Test
    fun `profile event prefixes cannot match another profiles prefix query`() {
        val prefixes = listOf(LocalProfiles.DEFAULT_ID, first, second).map(::calendarMirrorUriPrefix)
        prefixes.forEachIndexed { index, prefix ->
            prefixes.filterIndexed { other, _ -> other != index }.forEach { other ->
                assertFalse((other + "recorded/2026-09-05").startsWith(prefix))
            }
        }
        assertEquals(3, listOf(LocalProfiles.DEFAULT_ID, first, second).map(::calendarMirrorSelectionFile).toSet().size)
        assertEquals(3, listOf(LocalProfiles.DEFAULT_ID, first, second).map(::reminderWorkName).toSet().size)
        assertEquals(3, listOf(LocalProfiles.DEFAULT_ID, first, second).map(::reminderStateName).toSet().size)
        assertEquals(3, listOf(LocalProfiles.DEFAULT_ID, first, second).map(::reminderNotificationTag).toSet().size)
    }

    @Test
    fun `invalid ids cannot widen calendar queries or escape local files`() {
        listOf("", "../default", "%", "_", "DEFAULT", "1-1-1-1-1", first.uppercase()).forEach { id ->
            assertFailsWith<IllegalArgumentException> { calendarMirrorUriPrefix(id) }
            assertFailsWith<IllegalArgumentException> { calendarMirrorSelectionFile(id) }
            assertFailsWith<IllegalArgumentException> { reminderWorkName(id) }
            assertFailsWith<IllegalArgumentException> { reminderStateName(id) }
            assertFailsWith<IllegalArgumentException> { reminderNotificationTag(id) }
        }
    }
}
