# Calendar, Fertility, and Personal Mood Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve monthly prediction baselines, add estimated fertility and personal mood insights, render connected calendar spans, and mirror the same safe layers to an installed calendar provider.

**Architecture:** Pure planners produce deterministic forecast, fertility, mood, and calendar-event models. `CycleStore` persists immutable monthly baselines; `CalendarMirror` is the only Android Calendar Provider boundary. Compose consumes prepared `AppState` values and never performs provider or database I/O.

**Tech Stack:** Kotlin 2.x, Android SDK 36 / min SDK 29, SQLite, Jetpack Compose Material 3, Android `CalendarContract`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-29-calendar-fertility-mood-design.md`

## Global Constraints

- No new dependency, account, server, analytics, or network permission.
- Package remains `com.majkeylab.seliacycles`.
- Calendar row IDs must not enter device-transfer backup.
- Notes, symptoms, measurements, intimacy, and raw moods must not enter calendar events.
- Fertility and mood results must remain estimates, never medical or contraceptive claims.
- Every new user string exists in `values`, `values-cs`, `values-sk`, `values-de`, `values-pl`, and `values-es`.

---

### Task 1: Immutable monthly forecast baselines

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/ForecastSnapshots.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/ForecastSnapshotPlannerTest.kt`

**Interfaces:**
- Produces: `ForecastSnapshot`, `ForecastSnapshotPlanner.missingSnapshots(backup, existing, referenceDate)`.
- Produces: `CycleStore.loadForecastSnapshots()` and `CycleStore.saveForecastSnapshots(snapshots)`.

- [ ] Write tests proving a recorded month keeps a reconstructed pre-input estimate, an existing baseline is not replaced, future months are not persisted, and months without prior data remain absent.
- [ ] Run `gradlew.bat :app:testDebugUnitTest --tests "com.majkeylab.seliacycles.ForecastSnapshotPlannerTest" --console=plain`; expect failure because the planner is missing.
- [ ] Implement the pure planner with a rolling 12-month history and `CyclePredictor` called only with logs earlier than each month.
- [ ] Add SQLite table `forecast_snapshots(month INTEGER PRIMARY KEY, period_start INTEGER NOT NULL, earliest_start INTEGER NOT NULL, latest_start INTEGER NOT NULL, period_length INTEGER NOT NULL)` in database version 3. Use `CONFLICT_IGNORE`.
- [ ] Load/store missing snapshots on the existing IO dispatcher and delete them in `clearAll()`.
- [ ] Re-run the focused test; expect pass.

### Task 2: Fertility and personal mood insights

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/CycleInsights.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/CycleInsightsTest.kt`

**Interfaces:**
- Produces: `FertilityEstimate(periodStart, ovulation, fertileStart, fertileEnd)`.
- Produces: `CyclePhase` and `PersonalMoodTrend(mood, sampleCount)`.
- Produces: `CycleInsights.forDate(backup, snapshots, date)`.

- [ ] Write tests with hand-derived dates: period start August 29 => ovulation August 15, fertile August 10-16.
- [ ] Write tests requiring at least three same-phase moods from at least two completed cycles; insufficient data and tied medians return no trend.
- [ ] Run focused tests; expect missing-type failure.
- [ ] Implement fertility dates and four phase buckets. Use only the user's completed-cycle mood logs, the median mood ordinal, a maximum of eight recent cycles, and no population default.
- [ ] Re-run focused tests; expect pass.

### Task 3: Native Android calendar mirror

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/CalendarMirrorPlanner.kt`
- Create: `app/src/main/java/com/majkeylab/seliacycles/CalendarMirror.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/CalendarMirrorPlannerTest.kt`

**Interfaces:**
- Produces: `CalendarMirrorPlanner.plan(backup, snapshots, referenceDate): List<MirrorEvent>`.
- Produces: `CalendarMirror.snapshot`, `connect`, and `disconnect`.

- [x] Write planner tests first and observe missing-type RED.
- [x] Implement the bounded recorded/predicted period planner and observe GREEN.
- [ ] Extend planner tests for fertile and ovulation events without raw private details.
- [ ] Add manifest `READ_CALENDAR` / `WRITE_CALENDAR` permissions.
- [ ] Query only visible synchronized calendars with contributor-or-higher access.
- [ ] Replace Selia-tagged events in one `CalendarContract.AUTHORITY` batch; use UTC all-day spans, free availability, private access by default, normal provider visibility only in explicit Partner view, and no alarm.
- [ ] Keep selected ID in `noBackupFilesDir`; isolate provider failures from private-data loading.

### Task 4: Connected calendar UI and complete translations

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-cs/strings.xml`
- Modify: `app/src/main/res/values-sk/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`
- Modify: `app/src/main/res/values-pl/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`

**Interfaces:**
- Consumes: snapshots and insights exposed in `AppState`.
- Produces: calendar permission flow, target selector, disconnect, comparison rows, insight card, and connected day segments.

- [ ] Add a Calendar sync settings category with one runtime permission request and one target list.
- [ ] Show recorded and saved-estimate rows together, including signed day difference.
- [ ] Show estimated ovulation, fertile window, phase, mood evidence, and the non-contraception notice.
- [ ] Compute previous/next connection flags per week row. Use rounded outer corners, square inner corners, recorded fill, predicted outline on overlap, and separate fertility/ovulation colors.
- [ ] Add Partner view as an optional plain-language insight surface without a partner account.
- [ ] Add matching keys in all six locales and run Android resource merging through the normal build.

### Task 5: Release and physical-device acceptance

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Modify: `PRIVACY.md`
- Modify: `docs/play-store/STORE_LISTING.md`
- Modify: `docs/play-store/DATA_SAFETY.md`
- Create: `docs/qa/2026-08-29-calendar-fertility-acceptance.md`

**Interfaces:**
- Produces: version code 4 / `0.4.0-beta.1`, signed AAB, physical QA evidence, and Play-ready disclosures.

- [ ] Update privacy wording: Selia has no network permission; selected labels can be copied to a calendar provider and that provider may synchronize them.
- [ ] Run `gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease --console=plain`.
- [ ] Review `git diff --check`, manifest permissions, dependency graph, staged secret scan, and signed artifact hashes.
- [ ] Install only on Huawei `BQLDU19927002646` with `adb -s BQLDU19927002646 install --user 0 -r ...`.
- [ ] Verify live: denied permission, selected target, provider events, updated real period, preserved baseline, fertile/ovulation layers, connected spans, disconnect cleanup, and daily-log regression.
- [ ] Commit atomically with Conventional Commits, push after all gates pass, wait for GitHub Android CI, then update Closed Alpha only if Play permits a safe replacement without deleting the reviewed release.
