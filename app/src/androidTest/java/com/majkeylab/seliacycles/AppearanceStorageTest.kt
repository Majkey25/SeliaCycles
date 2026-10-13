package com.majkeylab.seliacycles

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun versionTwelveMigrationPreservesDataAndDefaults() {
        check(context.packageName.endsWith(".qa"))
        val id = UUID.randomUUID().toString()
        val name = profileDatabaseName(id)
        val original = CycleBackup(listOf(DayLog(LocalDate.now(), bleeding = true, flow = Flow.UNKNOWN, note = "QA preserve")),
            AppSettings(palette = AppPalette.FOREST, periodLengthOverride = 6, contraceptionReminderEnabled = true))
        try {
            CycleStore(context, id).use { it.replace(original) }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("""CREATE TABLE old_settings AS SELECT id,cycle_length,period_length,first_day,predictions,
                    reminder,reminder_days,theme,partner_view,palette,luteal_phase,custom_primary,custom_secondary,
                    custom_tertiary,custom_entry,profile_age,profile_height,profile_weight,profile_goal,life_situation,
                    show_phase_guidance,show_self_care,show_cycle_details,simple_mode,cycle_length_override,
                    period_length_override,active_period_start,contraception_reminder,contraception_minute FROM settings""")
                db.execSQL("DROP TABLE settings")
                db.execSQL("ALTER TABLE old_settings RENAME TO settings")
                db.version = 12
            }
            CycleStore(context, id).use {
                assertEquals(13, it.readableDatabase.version)
                assertEquals(original, it.load())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun newAppearanceSurvivesReopenAndRealPcFileRoundTrip() {
        check(context.packageName.endsWith(".qa"))
        val id = UUID.randomUUID().toString()
        val backup = CycleBackup(listOf(DayLog(LocalDate.now(), note = "QA original")), AppSettings(
            palette = AppPalette.CUSTOM, darkPalette = AppPalette.AMOLED, textScalePercent = 125,
            customPalette = CustomPalette(backgroundRgb = 0xFFF8EE, surfaceRgb = 0xFFFFFF, textRgb = 0x202020),
        ))
        try {
            CycleStore(context, id).use { it.replace(backup) }
            val saved = CycleStore(context, id).use { it.load() }
            assertEquals(backup, saved)
            val bytes = ByteArrayOutputStream()
            MyCalendarExporter(context).write(SeliaTransfer(saved, emptyList()), bytes)
            val restored = MyCalendarImporter(context).inspect(ByteArrayInputStream(bytes.toByteArray()))
            assertEquals(backup, restored.seliaTransfer?.backup)
        } finally { context.deleteDatabase(profileDatabaseName(id)) }
    }

    @Test fun queuedAppearanceChangesPreserveNewPeriodAndImportedSettings() {
        check(context.packageName.endsWith(".qa"))
        val id = UUID.randomUUID().toString()
        val original = AppSettings(theme = AppTheme.LIGHT)
        val dark = original.copy(theme = AppTheme.DARK)
        val newer = original.copy(activePeriodStart = LocalDate.now(), periodLengthOverride = 6, reminderEnabled = true)
        try {
            CycleStore(context, id).use { store ->
                store.saveSettings(newer)
                store.saveSettingsChanges(original, dark)
                assertEquals(newer.copy(theme = AppTheme.DARK), store.load().settings)
                store.saveSettingsChanges(dark, original)
                assertEquals(newer, store.load().settings)
                store.saveSettingsChanges(original, original)
                assertEquals(newer, store.load().settings)
            }
        } finally { context.deleteDatabase(profileDatabaseName(id)) }
    }
}
