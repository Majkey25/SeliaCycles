# Calendar Redesign and My Calendar Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a clear calendar-first Selia Cycles UI and safely merge the supplied My Calendar `.pc` data into the local calendar.

**Architecture:** Keep the existing offline-first ViewModel and SQLite store. Add one bounded My Calendar reader that extracts and validates the embedded `cloud.db`, transform supported rows into typed `DayLog` values, preview before an atomic merge, and retain unknown source details without inventing labels. Redesign Compose surfaces in place so the primary daily action is obvious without adding a UI framework.

**Tech Stack:** Kotlin 2.3, Android 10+, Jetpack Compose Material 3, Android SQLite, Java object/ZIP streams, JUnit 4, Gradle Android lint.

---

### Task 1: Bounded My Calendar container reader

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/MyCalendarBackup.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/MyCalendarBackupTest.kt`

- [ ] **Step 1: Write the failing wrapper tests**

Test a synthetic stream written with `ObjectOutputStream`, three metadata integers, and `ZipOutputStream`. Assert that `cloud.db` is returned, duplicate entries fail, a missing database fails, and an entry over the limit fails.

```kotlin
val result = MyCalendarContainer.read(ByteArrayInputStream(fixture(mapOf("cloud.db" to sqliteHeader))))
assertContentEquals(sqliteHeader, result.database)
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*MyCalendarBackupTest" --console=plain`

Expected: compilation failure because `MyCalendarContainer` does not exist.

- [ ] **Step 3: Implement the smallest bounded reader**

Expose:

```kotlin
data class MyCalendarContainer(val database: ByteArray, val generation: String?)

object MyCalendarContainerReader {
    const val MAX_FILE_BYTES = 20 * 1024 * 1024
    fun read(input: InputStream): MyCalendarContainer
}
```

Validate Java stream metadata `-1, 1, 0`, at most 32 unique ZIP entries, a 10 MiB `cloud.db`, a 64 KiB generation value, and the `SQLite format 3\u0000` header.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Task 1 command. Expected: all `MyCalendarBackupTest` cases pass.

### Task 2: Typed imported rows and safe merge rules

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/BackupCodec.kt`
- Modify: `app/src/test/java/com/majkeylab/seliacycles/BackupCodecTest.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/MyCalendarTransformTest.kt`

- [ ] **Step 1: Write failing model and merge tests**

Add tests for canonical metric bounds, v1 backup compatibility, v2 round-trip, and merge precedence:

```kotlin
val current = DayLog(day, mood = Mood.GOOD, note = "keep")
val imported = DayLog(day, bleeding = true, flow = Flow.MEDIUM, weightKg = 70.2)
assertEquals("keep", mergeDayLogs(current, imported).note)
assertEquals(Flow.MEDIUM, mergeDayLogs(current, imported).flow)
```

- [ ] **Step 2: Verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*BackupCodecTest" --tests "*MyCalendarTransformTest" --console=plain`

Expected: missing properties and merge function failures.

- [ ] **Step 3: Add minimal typed fields and database migration**

Add optional `weightKg`, `temperatureC`, `sleepHours`, and `intimacy` to `DayLog`, plus bounded `importedDetails`. Bump the SQLite database to version 2 and add nullable columns through `ALTER TABLE`. Store metric values only. Update `isEmpty`, cursor mapping, values, and transaction merge.

- [ ] **Step 4: Version Selia backup JSON without breaking v1**

Write version 2. Read both versions 1 and 2, defaulting new fields for version 1. Reject all other versions.

- [ ] **Step 5: Verify GREEN**

Run the Task 2 command. Expected: all focused tests pass.

### Task 3: Read verified My Calendar SQLite rows

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MyCalendarBackup.kt`
- Modify: `app/src/test/java/com/majkeylab/seliacycles/MyCalendarTransformTest.kt`

- [ ] **Step 1: Write failing transformation tests**

Test strict `yyyyMMdd` dates, period durations `1..14`, measurement bounds, note truncation rejection, malformed legacy detail limits, and unsupported period rows.

- [ ] **Step 2: Verify RED**

Run the Task 2 focused command. Expected: missing transformer APIs.

- [ ] **Step 3: Implement Android SQLite boundary and pure transformer**

Expose:

```kotlin
data class MyCalendarPreview(
    val logs: List<DayLog>,
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val unsupportedDetails: Int,
)

class MyCalendarImporter(private val context: Context) {
    fun inspect(input: InputStream): MyCalendarPreview
}
```

Write `cloud.db` to a private cache file, open read-only, verify required tables and columns with `PRAGMA table_info`, query only named columns, close and delete the file in `finally`. Convert supported values and preserve bounded unknown mood/symptom codes in `importedDetails`.

- [ ] **Step 4: Verify GREEN**

Run focused tests. Then inspect the supplied backup locally without printing dates or notes and confirm non-empty preview.

### Task 4: Preview and confirm import

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-cs/strings.xml`

- [ ] **Step 1: Add ViewModel import state**

Add `myCalendarPreview`, `inspectMyCalendar(uri)`, `confirmMyCalendarImport()`, and `cancelMyCalendarImport()`. Read and parse on `Dispatchers.IO`; mutate the store only after confirmation under `storeMutex`.

- [ ] **Step 2: Add dedicated picker and preview dialog**

Use `OpenDocument` and show source, day count, range, unsupported-detail count, Merge, and Cancel. Never request a password for `.pc`.

- [ ] **Step 3: Run unit tests, lint, and debug build**

Run: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`

Expected: BUILD SUCCESSFUL.

### Task 5: Adaptive prediction with honest uncertainty

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CyclePredictor.kt`
- Modify: `app/src/test/java/com/majkeylab/seliacycles/CyclePredictorTest.kt`

- [ ] **Step 1: Write failing recent-history and variability tests**

Assert that newer complete cycles carry more weight, invalid intervals are ignored, and varied cycles produce a wider uncertainty range.

- [ ] **Step 2: Verify RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "*CyclePredictorTest" --console=plain`

- [ ] **Step 3: Implement a bounded deterministic model**

Use at most the last six valid cycle intervals, integer recency weights, and mean absolute deviation. Add `uncertaintyDays` and derived earliest/latest dates. Do not use weight as a causal predictor.

- [ ] **Step 4: Verify GREEN**

Run the focused predictor tests. Expected: all pass.

### Task 6: Calendar-first Compose redesign

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/AppTheme.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-cs/strings.xml`

- [ ] **Step 1: Replace the Today hierarchy**

Add a high-contrast rose/violet gradient hero, cycle day, estimate window, seven-day strip, and one large add/edit FAB. Keep the medical notice visible but secondary.

- [ ] **Step 2: Simplify the day editor**

Make none/light/medium/heavy one direct choice. Use a full-height sheet with scrollable details and a stable bottom Save action. Put weight, temperature, sleep, intimacy, and imported details behind More details.

- [ ] **Step 3: Simplify Settings**

Show only category rows at first. Open one focused detail sheet for cycle, appearance/language, reminders, data/import, tracking, or privacy.

- [ ] **Step 4: Align Calendar and History**

Reuse recorded/estimated states, increase day hit targets, and keep metrics readable without card spam.

- [ ] **Step 5: Run static gates**

Run: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`

Expected: BUILD SUCCESSFUL with no new lint errors.

### Task 7: Complete language choices

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Create: `app/src/main/res/values-sk/strings.xml`
- Create: `app/src/main/res/values-de/strings.xml`
- Create: `app/src/main/res/values-pl/strings.xml`
- Create: `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Add locale filters and selector values**

Keep the empty locale tag as System. Add `sk`, `de`, `pl`, and `es` beside `en` and `cs`.

- [ ] **Step 2: Add complete UTF-8 translations**

Translate every user-visible base string and plural. Do not expose a language with partial English fallback.

- [ ] **Step 3: Build every locale**

Run the Task 6 static gates. Expected: resource merge and lint succeed.

### Task 8: Physical-phone acceptance and release gates

**Files:**
- Modify: `docs/qa/2026-08-28-acceptance.md`
- Add: `docs/qa/screenshots/phone-redesign-*.png`

- [ ] **Step 1: Install only on the approved phone**

Run: `adb -s BQLDU19927002646 install --user 0 -r app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2: Verify live scenarios**

Test quick log, calendar-day edit, advanced details, cold persistence, My Calendar preview/merge, malformed-file rejection, all languages, light/dark/system themes, reminders, existing Selia encrypted backup, and Health Connect unavailable behavior.

- [ ] **Step 3: Run final build**

Run the repository's full clean test/lint/debug/release bundle command with external signing properties. Verify AAB signature and SHA-256.

- [ ] **Step 4: Hostile diff review**

Check scope, data-loss paths, parser bounds, migration, translation coverage, accessibility, secrets, generated artifacts, and `git diff --check` before commit.

