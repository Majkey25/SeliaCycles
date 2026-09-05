package com.majkeylab.seliacycles

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class LocalProfilesStorageTest {
    private val target get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "profiles_test_${UUID.randomUUID()}"
    private val created = mutableListOf<String>()
    private lateinit var context: Context
    private lateinit var profiles: LocalProfiles

    @Before
    fun isolatedRegistry() {
        check(target.packageName.endsWith(".qa")) { "Tests must use the isolated QA application" }
        context = object : ContextWrapper(target) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences(preferencesName, mode)
        }
        profiles = LocalProfiles(context)
    }

    @After
    fun cleanup() {
        created.forEach { target.deleteDatabase(profileDatabaseName(it)) }
        target.deleteSharedPreferences(preferencesName)
    }

    @Test
    fun profileChangesPersistAndSelectedRemovalReturnsToOriginal() {
        assertEquals(LocalProfile(LocalProfiles.DEFAULT_ID, ""), profiles.selected())
        val daughter = create(" Dcera ", UiMode.SIMPLE)
        assertEquals("Dcera", daughter.name)
        profiles.select(daughter.id)
        assertEquals(daughter, LocalProfiles(context).selected())
        val changed = profiles.update(daughter.id, "Anna", UiMode.DETAILED)
        assertEquals(changed, LocalProfiles(context).selected())
        profiles.remove(daughter.id)
        assertEquals(LocalProfiles.DEFAULT_ID, LocalProfiles(context).selected().id)
        assertThrows(IllegalArgumentException::class.java) { profiles.remove(LocalProfiles.DEFAULT_ID) }
        assertThrows(IllegalArgumentException::class.java) { profiles.select(daughter.id) }
    }

    @Test
    fun separateDatabasesKeepOriginalAndOtherProfileDataUntouched() {
        val original = CycleStore(context).use { it.load() }
        val first = create("Anna")
        val second = create("Eva")
        val today = LocalDate.now()
        CycleStore(context, first.id).use { it.saveLog(DayLog(today, note = "Anna only")) }
        CycleStore(context, second.id).use {
            assertEquals(emptyList<DayLog>(), it.load().logs)
            it.saveLog(DayLog(today, note = "Eva only"))
        }
        assertThrows(IllegalStateException::class.java) { profiles.remove(first.id) }
        CycleStore(context, first.id).use { assertEquals("Anna only", it.load().logs.single().note) }
        CycleStore(context, second.id).use { assertEquals("Eva only", it.load().logs.single().note) }
        CycleStore(context).use { assertEquals(original, it.load()) }
        check(context.deleteDatabase(profileDatabaseName(first.id)))
        profiles.remove(first.id)
        CycleStore(context, second.id).use { assertEquals("Eva only", it.load().logs.single().note) }
    }

    @Test
    fun profileLimitRejectsCreationWithoutChangingRegistry() {
        repeat(LocalProfiles.MAX_PROFILES - 1) { create("Profile $it") }
        val before = profiles.profiles()
        assertThrows(IllegalArgumentException::class.java) { profiles.create("One too many") }
        assertEquals(before, profiles.profiles())
    }

    private fun create(name: String, mode: UiMode = UiMode.STANDARD): LocalProfile =
        profiles.create(name, mode).also { created += it.id }
}
