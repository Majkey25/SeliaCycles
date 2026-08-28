# Local Cycle Prediction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace cloud/import complexity with robust local prediction and visible current/next-month forecasts.

**Architecture:** `CyclePredictor` remains the single pure prediction boundary. Compose consumes its monthly forecast list. SQLite models and schema remain unchanged to preserve existing phone data; removed integrations leave no compatibility shim.

**Tech Stack:** Kotlin 2.3, Java time, Jetpack Compose Material 3, SQLite, Android native backup rules, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-28-local-cycle-prediction-design.md`

## Global Constraints

- Package stays `com.majkeylab.seliacycles`.
- No network, account, Firebase, Health Connect, external import, or manual backup feature.
- No new dependency.
- Existing SQLite data must survive `adb install -r`.
- Every ADB command targets `BQLDU19927002646`.
- No emulator.

---

### Task 1: Robust local predictor

**Files:**
- Modify: `app/src/test/java/com/majkeylab/seliacycles/CyclePredictorTest.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CyclePredictor.kt`

**Interfaces:**
- Consumes: recorded bleeding dates, defaults, explicit `referenceDate`.
- Produces: `CyclePrediction.futurePeriodStarts` and two `MonthlyForecast` values.

- [ ] Add failing tests for skipped 56/84-day gaps, a single outlier, real-start re-anchoring, current/next month, and no history.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests '*CyclePredictorTest' --console=plain`; confirm RED failures reference missing forecast behavior.
- [ ] Implement period grouping, interval normalization, robust filtering, future starts, and monthly forecasts in `CyclePredictor.kt`.
- [ ] Run the same focused test command; require PASS.

### Task 2: Current and next month UI

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values*/strings.xml`

**Interfaces:**
- Consumes: `CyclePrediction.monthlyForecasts` and `futurePeriodStarts`.
- Produces: two forecast rows on Today and multi-month predicted days on Calendar.

- [ ] Add translated strings for `Forecast`, `This month`, `Next month`, `Recorded`, `Estimated`, and `No estimate` in all six locales.
- [ ] Render exactly two icon-led forecast rows below the hero.
- [ ] Build predicted calendar days from all generated future starts.
- [ ] Run focused unit tests and `:app:lintDebug :app:assembleDebug`.

### Task 3: Remove cloud and external transfer features

**Files:**
- Delete: `BackupCodec.kt`, `CalendarSyncModels.kt`, `CalendarSyncRepository.kt`, `GoogleAccountManager.kt`, `HealthConnectImporter.kt`, `MyCalendarBackup.kt` and their tests.
- Delete: Firebase rules/config/tests and `docs/firebase-setup.md`.
- Modify: `MainActivity.kt`, `MainViewModel.kt`, `SeliaCyclesApp.kt`, Gradle files, manifest, backup XML, CI, strings, README/privacy/store documents.

**Interfaces:**
- Consumes: existing `CycleStore` and Android device transfer.
- Produces: local-only app with no network permission or external account surface.

- [ ] Remove integration call sites first; keep `saveLog`, `saveSettings`, `clearAll`, reload, reminders, and messages.
- [ ] Remove unused code, dependencies, permissions, resources, tests, and Firebase CI job.
- [ ] Configure Android 12+ rules with cloud backup disabled and database/settings included for device transfer.
- [ ] Preserve legacy Android 10 transfer rules and explain this platform limit in user-facing privacy text.
- [ ] Bump version to code 3 / `0.3.0-beta.1`.

### Task 4: Full and physical-phone verification

**Files:**
- Modify: `docs/qa/2026-08-28-acceptance.md`
- Update screenshots only when UI changed.

**Interfaces:**
- Consumes: debug APK and preserved phone database.
- Produces: exact build, runtime, permission, and crash evidence.

- [ ] Run `./gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease --console=plain`.
- [ ] Review `git diff --check`, changed-file diff, merged manifest, dependency report, APK metadata, and package permissions.
- [ ] Install with the pinned Huawei serial and `-r`; verify local history count survives.
- [ ] Live test: current recorded forecast, next-month estimate, add/edit/delete nearby day, settings, old calendar workflow, cold start, and crash buffer.
- [ ] Update QA evidence, rerun the full gate, then commit/push only because the user already authorized repository delivery.
