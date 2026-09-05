package com.majkeylab.seliacycles

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class LocalProfilesTest {
    @Test
    fun `original database keeps its name and additional profiles are isolated`() {
        assertEquals("selia-cycles.db", profileDatabaseName(LocalProfiles.DEFAULT_ID))
        val first = "8c6295ef-505d-44e7-b054-3492d10504a1"
        val second = "8c6295ef-505d-44e7-b054-3492d10504a2"
        assertEquals("selia-cycles-$first.db", profileDatabaseName(first))
        assertNotEquals(profileDatabaseName(first), profileDatabaseName(second))
    }

    @Test
    fun `profile identifiers cannot escape their database directory or use UUID aliases`() {
        listOf("", "../selia-cycles", "default.db", "1-1-1-1-1", "8C6295EF-505D-44E7-B054-3492D10504A1").forEach { id ->
            assertFailsWith<IllegalArgumentException> { profileDatabaseName(id) }
        }
    }

    @Test
    fun `profile labels are bounded and modes are explicitly chosen`() {
        val id = "8c6295ef-505d-44e7-b054-3492d10504a1"
        assertEquals(UiMode.STANDARD, LocalProfile(id, "Dcera").mode)
        assertEquals(UiMode.SIMPLE, LocalProfile(id, "Dcera", UiMode.SIMPLE).mode)
        assertEquals("", LocalProfile(LocalProfiles.DEFAULT_ID, "").name)
        listOf("", " ", "a".repeat(41), "Name\n", " A").forEach { name ->
            assertFailsWith<IllegalArgumentException> { LocalProfile(id, name) }
        }
    }
}
