# Code 11 cycle dashboard and export implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix imported-history prediction and real-period entry, add a compact linked dashboard and local `.pc` export, update defaults and artwork, and prepare code 11 for Closed Alpha.

**Architecture:** Keep `CyclePredictor` pure and add explicit manual overrides. Store an active period start instead of writing future bleeding. Reuse the current My Calendar reader and add a bounded writer plus one versioned Selia payload in `cloud.db`. Keep Today as navigation and History as analytics.

**Tech Stack:** Kotlin 2.3, Java time, SQLite, Jetpack Compose Material 3, Android document picker, JUnit 4, Java ZIP and data streams.

---

### Task 1: Manual prediction overrides and stored state

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CyclePredictor.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleInsights.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/ForecastSnapshots.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: all six `app/src/main/res/values*/strings.xml` locale files
- Test: `app/src/test/java/com/majkeylab/seliacycles/CyclePredictorTest.kt`

- [ ] **Step 1: Write the failing manual-override regression**

Add a test with the sanitized last starts from the generation 7 backup. Call `CyclePredictor.predict` with `cycleLengthOverride = 32` and assert that the September start changes to `2026-09-11`.

```kotlin
val starts = listOf(
    LocalDate.of(2026, 3, 26),
    LocalDate.of(2026, 4, 23),
    LocalDate.of(2026, 6, 11),
    LocalDate.of(2026, 7, 9),
)
val prediction = CyclePredictor.predict(
    bleedingDays = starts.toSet(),
    defaultCycleLength = 28,
    defaultPeriodLength = 5,
    referenceDate = LocalDate.of(2026, 8, 31),
    cycleLengthOverride = 32,
)
assertEquals(LocalDate.of(2026, 9, 11), prediction.nextPeriodStart)
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.CyclePredictorTest --console=plain
```

Expected: compilation fails because `cycleLengthOverride` does not exist.

- [ ] **Step 3: Add explicit overrides**

Add nullable `cycleLengthOverride`, `periodLengthOverride`, and `activePeriodStart` fields to `AppSettings`. Add optional parameters to `CyclePredictor.predict`.

```kotlin
val cycleLength = cycleLengthOverride ?: cycleLengths.weightedAverageOr(defaultCycleLength)
val periodLength = periodLengthOverride ?: learnedPeriodLength
```

Pass these fields from every production caller. Exclude the group that starts at `activePeriodStart` from the learned period-duration list.

- [ ] **Step 4: Migrate the SQLite settings row**

Increase `DATABASE_VERSION` from 8 to 9. Add nullable `cycle_length_override`, `period_length_override`, and `active_period_start` columns. Keep existing rows null so current users stay in automatic mode.

- [ ] **Step 5: Make manual and automatic modes explicit in Settings**

Display the learned values while automatic mode is active. Changing a stepper stores the corresponding override. Show **Use automatic estimate** when either override exists and clear both values when the user taps it.

- [ ] **Step 6: Run prediction tests and verify GREEN**

Run the Task 1 command. Expected: all `CyclePredictorTest` tests pass.

- [ ] **Step 7: Commit the slice**

```powershell
git add app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt app/src/main/java/com/majkeylab/seliacycles/CyclePredictor.kt app/src/main/java/com/majkeylab/seliacycles/CycleInsights.kt app/src/main/java/com/majkeylab/seliacycles/ForecastSnapshots.kt app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt app/src/main/res app/src/test/java/com/majkeylab/seliacycles/CyclePredictorTest.kt
git commit -m "fix: apply manual cycle overrides"
```

### Task 2: Record only real period days

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/PeriodActions.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/PeriodActionsTest.kt`

- [ ] **Step 1: Write failing Start and End tests**

Assert that `PeriodActions.start(day, logs)` records only `day`. Assert that `PeriodActions.end(end, logs, start)` fills the inclusive range and does not remove unrelated values.

```kotlin
val started = PeriodActions.start(LocalDate.of(2026, 8, 31), emptyList())
assertEquals(listOf(LocalDate.of(2026, 8, 31)), started.map(DayLog::day))
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.PeriodActionsTest --console=plain
```

Expected: the existing Start test reports future recorded days.

- [ ] **Step 3: Implement active Start and End**

Remove `periodLength` from `PeriodActions.start`. In `MainViewModel.startPeriod`, update the log and `activePeriodStart` together through `CycleStore.replace`. In `endPeriod`, require an active start within 13 days, fill the real range, and clear the active start. Clear it when the active period is removed.

- [ ] **Step 4: Bind the primary action to the active start**

Make `TodayPrimaryAction.END_PERIOD` depend on a valid `activePeriodStart`, not on prefilled bleeding for today. Keep the detailed editor as the correction path.

- [ ] **Step 5: Run PeriodActions tests and verify GREEN**

Run the Task 2 command. Expected: all tests pass.

- [ ] **Step 6: Commit the slice**

```powershell
git add app/src/main/java/com/majkeylab/seliacycles/PeriodActions.kt app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt app/src/test/java/com/majkeylab/seliacycles/PeriodActionsTest.kt
git commit -m "fix: record only completed period days"
```

### Task 3: My Calendar compatible `.pc` export

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/MyCalendarExport.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MyCalendarBackup.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/MyCalendarExportTest.kt`
- Modify: all six `app/src/main/res/values*/strings.xml` locale files

- [ ] **Step 1: Write failing container and round-trip tests**

Test the exact header, entry names, generation, SQLite header, period mapping, and the Selia payload. Round-trip a `DayLog` with spotting, mood, symptoms, measurements, intimacy, tests, wellbeing, activity, medication, and imported details.

- [ ] **Step 2: Run the export test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.MyCalendarExportTest --console=plain
```

Expected: compilation fails because `MyCalendarExporter` and `SeliaBackupCodec` do not exist.

- [ ] **Step 3: Implement the bounded codecs**

Use `DataOutputStream` for one versioned `SeliaBackup` BLOB. Use `ObjectOutputStream` and `ZipOutputStream` for the `.pc` container. Reproduce the verified My Calendar tables and entry set. Write only verified common fields into `Period` and `Note`.

- [ ] **Step 4: Extend import detection**

If `SeliaBackup` exists, decode it with the same limits and include full Selia logs, settings, and snapshots in the preview. If it is damaged, fail without falling back to partial common data.

- [ ] **Step 5: Add the document-picker flow**

Use `ActivityResultContracts.CreateDocument("application/octet-stream")`. Suggest `Selia-Cycles-yyyy-MM-dd.pc`. Write on `Dispatchers.IO`. Add the unencrypted-file warning and localized success and failure text.

- [ ] **Step 6: Run export and existing import tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.majkeylab.seliacycles.MyCalendar*" --console=plain
```

Expected: export, source import, signed-period, limit, and full round-trip tests pass.

- [ ] **Step 7: Commit the slice**

```powershell
git add app/src/main/java/com/majkeylab/seliacycles/MyCalendarExport.kt app/src/main/java/com/majkeylab/seliacycles/MyCalendarBackup.kt app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt app/src/test/java/com/majkeylab/seliacycles/MyCalendarExportTest.kt app/src/main/res
git commit -m "feat: export My Calendar pc backups"
```

### Task 4: Compact linked Today dashboard and relief care

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/TodayDashboard.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: all six `app/src/main/res/values*/strings.xml` locale files
- Test: `app/src/test/java/com/majkeylab/seliacycles/CalendarUiModelTest.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/SelfCareTimerTest.kt`

- [ ] **Step 1: Write failing destination tests**

Create a pure dashboard destination model. Assert that next period, fertile window, and ovulation map to their exact dates and that phase and analysis use their dedicated destinations.

- [ ] **Step 2: Run the UI-model tests and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.CalendarUiModelTest --console=plain
```

Expected: compilation fails because the dashboard destination model does not exist.

- [ ] **Step 3: Implement navigation state**

Keep the selected `Screen`, requested calendar date, and phase-sheet visibility in `SeliaCyclesApp`. Pass the requested date to `CalendarScreen` and consume it after the pager moves to the target month.

- [ ] **Step 4: Replace inline analytics with compact rows**

Make the hero, period, fertility, ovulation, phase, and analysis summaries clickable. Remove the expanded week and monthly-analysis block from Today. Open History for charts and metrics.

- [ ] **Step 5: Add phase details and persistent relief care**

Show one short phase sentence on Today and the full guidance in a full-height sheet. Keep a visible relief-care row whenever care is enabled. Add four safe activities with distinct icons and timer instructions.

- [ ] **Step 6: Verify UI model and timer tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.CalendarUiModelTest --tests com.majkeylab.seliacycles.SelfCareTimerTest --console=plain
```

Expected: all selected tests pass.

- [ ] **Step 7: Commit the slice**

```powershell
git add app/src/main/java/com/majkeylab/seliacycles/TodayDashboard.kt app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt app/src/test/java/com/majkeylab/seliacycles/CalendarUiModelTest.kt app/src/test/java/com/majkeylab/seliacycles/SelfCareTimerTest.kt app/src/main/res
git commit -m "feat: link the Today dashboard"
```

### Task 5: Default appearance and adaptive artwork

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Delete: `app/src/main/res/drawable-nodpi/ic_launcher_foreground_art.png`
- Delete: `app/src/main/res/drawable-nodpi/ic_launcher_monochrome_art.png`
- Modify: `docs/play-store/assets/icon-512.png`
- Modify: `docs/play-store/assets/feature-graphic-1024x500.png`
- Test: `app/src/test/java/com/majkeylab/seliacycles/AppPaletteTest.kt`

- [ ] **Step 1: Write the failing default test**

Assert that `AppSettings()` uses `AppTheme.LIGHT`, `AppPalette.OCEAN`, and null manual overrides.

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.AppPaletteTest --console=plain
```

Expected: the default theme or palette assertion fails.

- [ ] **Step 3: Apply the defaults and vector artwork**

Set Light and Ocean defaults. Replace the raster adaptive foreground with a vector black disc, white arrows, and red drop. Use a transparent adaptive background. Keep a separate monochrome silhouette.

- [ ] **Step 4: Render Play assets from the same mark**

Render a 512x512 opaque black Play icon and a 1024x500 feature graphic. Verify exact dimensions, RGB output, safe margins, and no cropped arrowheads.

- [ ] **Step 5: Run the default test and build resources**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.AppPaletteTest assembleDebug --console=plain
```

Expected: tests and resource compilation pass.

- [ ] **Step 6: Commit the slice**

```powershell
git add app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt app/src/main/res docs/play-store/assets app/src/test/java/com/majkeylab/seliacycles/AppPaletteTest.kt
git commit -m "fix: refresh defaults and launcher mark"
```

### Task 6: Code 11 release and physical acceptance

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Modify: `PRIVACY_POLICY.md`
- Modify: `docs/play-store/STORE_LISTING.md`
- Modify: `docs/play-store/DATA_SAFETY.md`
- Create: `docs/qa/2026-08-31-code11-acceptance.md`

- [ ] **Step 1: Set code 11 metadata**

Set `versionCode = 11` and `versionName = "0.9.0-beta.3"`. Document local unencrypted `.pc` export and the fixed prediction behavior.

- [ ] **Step 2: Run the full clean signed gate**

Run the repository's established clean test, lint, APK, and AAB command with the approved local signing-properties path. Expected: zero test failures, zero lint errors, signed release artifacts, and exit code 0.

- [ ] **Step 3: Verify on Huawei `BQLDU19927002646`**

Install the matching debug update without deleting data. Verify manual 32-day override, automatic reset, Start without future reality, End range, every dashboard link, phase sheet, relief-care activities, Light plus Ocean defaults in clean state, and the transparent launcher edge.

- [ ] **Step 4: Verify both `.pc` directions**

Export through the Android document picker. Import it into Selia and compare every field. Use a clean My Calendar test install to verify common periods and notes, then remove only that test installation.

- [ ] **Step 5: Review and commit release evidence**

Run `git diff --check`, review the complete diff, scan staged files for secrets, commit with Conventional Commits, and push `main`. Wait for Android CI.

- [ ] **Step 6: Prepare Closed Alpha**

Upload code 11, the Play icon, and the feature graphic. Remove superseded code from the new draft. Stop before the final **Send changes for review** action and request action-time confirmation.
