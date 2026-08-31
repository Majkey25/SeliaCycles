# Day Overview, Profile, and Custom Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add overview-first day navigation, stable sheet scrolling, a local profile with honest prediction gates, and preset/custom color palettes.

**Architecture:** Extend existing immutable settings and SQLite schema, keep prediction math pure, and route selected days through an overview sheet before the existing editor. Reuse Material 3 and current components; add no dependency.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android SQLite, JUnit 4, Gradle.

---

### Task 1: Profile contracts and prediction gates

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleInsights.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/ReminderWorker.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/ProfileSettingsTest.kt`
- Modify: `app/src/test/java/com/majkeylab/seliacycles/CycleInsightsTest.kt`

- [ ] Add RED tests for age/height/weight boundaries and the five life-situation gates.
- [ ] Run `./gradlew.bat testDebugUnitTest --tests com.majkeylab.seliacycles.ProfileSettingsTest --console=plain`; expect missing typed profile symbols.
- [ ] Add `TrackingGoal`, `LifeSituation`, and `UserProfile`, plus `canPredictPeriods` and `canEstimateFertility` properties.
- [ ] Gate dynamic period estimates, fertility insights, and reminders; retain past saved estimates.
- [ ] Run focused profile and insight tests; expect PASS.

### Task 2: Day comparison and overview-first UI

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/DayOverview.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/DayOverviewTest.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Add RED tests for exact, early, late, and unavailable monthly estimate comparisons.
- [ ] Implement `DayOverview.compare(day, backup, snapshots)` using saved snapshot and actual period starts.
- [ ] Add overview/edit sheet modes; calendar taps open overview while explicit log actions open edit.
- [ ] Render status, comparison, phase, fertility, mood, every recorded tracker, note, and compact layer legend before Edit record.
- [ ] Disable sheet gestures on overview and editor, keep one bounded content scroller and fixed footer.
- [ ] Run focused tests and `assembleDebug`; expect PASS.

### Task 3: Profile persistence and Settings UI

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: six locale `strings.xml` files

- [ ] Upgrade SQLite from 4 to 5 with nullable age/height/weight and non-null typed goal/situation defaults.
- [ ] Add icon-backed Profile settings with validated optional fields and choice previews.
- [ ] Show situation-specific prediction and safety notices.
- [ ] Add complete EN/CS/SK/DE/PL/ES strings.
- [ ] Run unit tests, lint, and debug build; expect PASS.

### Task 4: Preset and custom palettes

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleModels.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/AppTheme.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainActivity.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/test/java/com/majkeylab/seliacycles/AppPaletteTest.kt`
- Modify: six locale `strings.xml` files

- [ ] Extend RED palette tests to six distinct presets plus Custom and custom endpoint contrast.
- [ ] Add Forest, Sunset, Lilac, Custom, and typed three-color settings.
- [ ] Derive Material colors, containers, contrast foregrounds, and safe hero gradients without a dependency.
- [ ] Add three validated `#RRGGBB` fields, live preview, and Reset.
- [ ] Run palette tests, lint, and debug assembly; expect PASS.

### Task 5: Code 6 verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `README.md`
- Modify: `PRIVACY.md`
- Create: `docs/qa/2026-08-29-day-overview-profile-custom-theme-acceptance.md`

- [ ] Bump to code 6 / `0.6.0-beta.1` and document local profile/custom colors.
- [ ] Run `testDebugUnitTest lintDebug assembleDebug bundleRelease` with existing external signing properties.
- [ ] Install only on Huawei `BQLDU19927002646` when idle; verify migration, overview-first flow, smooth bottom scroll, profile gating, custom palette persistence, contrast, and regression period logging.
- [ ] Restore original profile, System/Selia appearance, and remove temporary records.
- [ ] Review full diff and leave it uncommitted/unpushed until explicit approval.
