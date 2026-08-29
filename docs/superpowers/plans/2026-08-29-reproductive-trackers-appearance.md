# Reproductive Trackers and Appearance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add locally stored reproductive trackers, a clear daily fertility estimate, and preview-based appearance settings.

**Architecture:** Extend the existing immutable models and SQLite helper, reuse the current daily sheet and settings page, and keep prediction anchored only to true bleeding. Resolve three built-in Material 3 palettes in `AppTheme.kt`; no dependency or service is added.

**Tech Stack:** Kotlin 2, Jetpack Compose Material 3, Android SQLite, JUnit 4, Gradle.

---

### Task 1: Tracker contracts

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/DayLogTrackersTest.kt`

- [ ] Write failing tests proving a spotting-only log is non-empty without being bleeding, pain accepts 0 and 10 but rejects -1 and 11, and each optional tracker can be cleared.
- [ ] Run `./gradlew testDebugUnitTest --tests com.majkeylab.seliacycles.DayLogTrackersTest --console=plain`; expect compilation failure because the new fields and enums do not exist.
- [ ] Add the smallest typed enums and `DayLog` fields required by the tests; update validation and `isEmpty`.
- [ ] Rerun the focused test; expect PASS.

### Task 2: SQLite persistence and migration

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt`

- [ ] Add schema columns for all new `DayLog` fields and `AppSettings.palette`.
- [ ] Add the version-4 migration using `ALTER TABLE` defaults that preserve existing records.
- [ ] Extend ordered column arrays, cursor decoding, and parameterized `ContentValues` writes.
- [ ] Run `./gradlew testDebugUnitTest --console=plain`; expect all model and prediction tests to pass.

### Task 3: Fertility status

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleInsights.kt`
- Modify: `app/src/test/java/com/majkeylab/seliacycles/CycleInsightsTest.kt`

- [ ] Add failing boundary tests for unavailable, outside-window, fertile-window, and estimated-ovulation-day states.
- [ ] Run the focused `CycleInsightsTest`; expect failure because the status is absent.
- [ ] Add one typed `FertilityStatus` field derived from the existing estimate without changing the prediction algorithm.
- [ ] Rerun the focused test; expect PASS.

### Task 4: Daily logging UI and translations

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-cs/strings.xml`
- Modify: `app/src/main/res/values-sk/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`
- Modify: `app/src/main/res/values-pl/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`

- [ ] Add icon-backed reproductive and wellness sections under More details using existing `ChoiceRow`, `FilterChip`, and `Stepper` components.
- [ ] Save every optional field into `DayLog`; tapping an active option clears it.
- [ ] Show the typed fertility status and today's recorded signals in the existing insight section.
- [ ] Add complete UTF-8 strings in all six supported locales.
- [ ] Run `./gradlew lintDebug assembleDebug --console=plain`; expect zero lint errors and a successful APK.

### Task 5: Appearance previews

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/AppTheme.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainActivity.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: the six `strings.xml` files from Task 4
- Create: `app/src/test/java/com/majkeylab/seliacycles/AppPaletteTest.kt`

- [ ] Add a failing test proving each palette resolves different representative colors and Selia remains the default.
- [ ] Run the focused test; expect compilation failure because `AppPalette` is absent.
- [ ] Add Selia, Rose, and Ocean light/dark schemes and pass the saved palette into `SeliaCyclesTheme`.
- [ ] Replace text-only appearance choices with accessible preview tiles that visibly expose theme and palette before selection.
- [ ] Rerun unit tests, lint, and debug assembly; expect PASS.

### Task 6: Release and live verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `README.md`
- Modify: `PRIVACY.md`
- Modify: `docs/play-store/DATA_SAFETY.md` only if wording is incomplete

- [ ] Bump to version code 5 and `0.5.0-beta.1` after behavior is complete.
- [ ] Run `./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease --console=plain` with the existing external signing properties; expect tests, lint, APK, and signed AAB success.
- [ ] Install the debug APK with `adb -s BQLDU19927002646 install -r ...` only when the Huawei is idle.
- [ ] Verify happy path: save spotting, egg-white mucus, positive LH, pain, energy, and medication; reopen and confirm persistence.
- [ ] Verify edge path: pain 0 and 10 save; 11 cannot be created; tapping selected optional chips clears them.
- [ ] Verify negative path: invalid numeric values disable Save; spotting does not become a period or re-anchor prediction.
- [ ] Verify regression: record and remove a normal period day; restart process; switch all theme modes and palettes from previews.
- [ ] Review `git diff --check`, `git diff --stat`, and the full diff. Do not commit, push, open a PR, or submit Play changes without current explicit approval.
