# Selia Cycles competitive parity and reliability implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove coherent future predictions and add private calendar filters and recurring symptom insights while improving measured performance and Android backup security.

**Architecture:** Keep prediction and pattern logic in pure Kotlin files with unit tests. Compose stores only transient filter selection and renders the existing blue marker for matching dates. Performance changes cache derived calendar collections; security changes restrict legacy Android backup transport without changing local storage or compatible `.pc` files.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, SQLite, JUnit 4, Gradle, adb, Android gfxinfo/meminfo.

---

### Task 1: Prediction update acceptance

**Files:**
- Create: `app/src/test/java/com/majkeylab/seliacycles/PredictionUpdateAcceptanceTest.kt`
- Modify only if the test fails: `app/src/main/java/com/majkeylab/seliacycles/CycleInsights.kt`
- Modify only if the test fails: `app/src/main/java/com/majkeylab/seliacycles/CalendarMirrorPlanner.kt`

- [ ] Add a test with historical 28-day periods, a stale current snapshot, and a newly recorded real start. Assert the next two live period starts use the new anchor, fertility follows those starts, and calendar mirror events use the same dates.
- [ ] Add edit/remove scenarios that move the anchor and assert every future date moves together.
- [ ] Run `./gradlew testDebugUnitTest --tests '*PredictionUpdateAcceptanceTest*' --console=plain`.
- [ ] If RED, fix the shared estimate source rather than patching Today, Calendar, or mirroring separately. Re-run until GREEN.

### Task 2: Calendar tracker filters

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/TrackerFilter.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/TrackerFilterTest.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: all six `app/src/main/res/values*/strings.xml` files

- [ ] Write failing tests for individual symptom matching, category matching, only-used filter discovery, OR matching, and a three-filter selection limit.
- [ ] Implement `TrackerFilter` as a fixed enum with `matches(log: DayLog)` and `availableFilters(logs)`.
- [ ] Add a Calendar filter button and bottom sheet that exposes only locally used filters, selects at most three, and clears all.
- [ ] While filters are active, show the existing blue marker only when a date matches. Do not add icons inside calendar cells.
- [ ] Run focused tests and `lintDebug`.

### Task 3: Personal symptom patterns

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/PersonalPatterns.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/PersonalPatternsTest.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: all six `app/src/main/res/values*/strings.xml` files

- [ ] Write failing tests for one dominant symptom phase, insufficient samples, fewer than two cycles, and duplicate symptoms on one day.
- [ ] Implement phase assignment from adjacent recorded period starts and the configured luteal phase. Require three samples across two cycles.
- [ ] Add a compact History section showing up to three patterns with symptom, phase, sample count, cycle count, and a non-diagnostic notice.
- [ ] Run focused tests and the complete unit suite.

### Task 4: Calendar performance

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Create: `.reference/tmp/perf-code16-before/` runtime artifacts when a target is available
- Create: `.reference/tmp/perf-code16-after/` runtime artifacts when a target is available

- [ ] Capture `gfxinfo framestats` and `meminfo` for one repeated month-swipe flow on an isolated target.
- [ ] Wrap recorded, predicted, fertility, ovulation, and lookup collections in `remember` keyed by their actual data inputs.
- [ ] Build one localized long-date formatter and one current date per month page, then pass prepared descriptions to day cells.
- [ ] Repeat the same flow and keep the change only if frame or repeated-work evidence improves or remains neutral without regression.

### Task 5: Security hardening

**Files:**
- Modify: `app/src/main/res/xml/backup_rules.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `docs/play-store/DATA_SAFETY.md`
- Create: `docs/security/2026-09-01-local-data-review.md`

- [ ] Record the trust boundaries for import, export, calendar provider, document provider, and device transfer.
- [ ] Add `requireFlags="deviceToDeviceTransfer"` to legacy database/shared-preference backup includes.
- [ ] Add `android:usesCleartextTraffic="false"`; retain no INTERNET permission.
- [ ] Verify import size, row, date, numeric, and text caps; SQL parameterization; export warning; and calendar field allowlist from source and tests.
- [ ] Run Gradle dependency reporting and document any reachable risk. Do not force dependency upgrades.

### Task 6: End-to-end acceptance and release

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Modify: `docs/play-store/STORE_LISTING.md`
- Create: `docs/qa/2026-09-01-code16-reliability.md`

- [ ] Bump to code 16 and `0.9.0-beta.8` only after behavior passes.
- [ ] Run clean unit tests, lint, minified signed APK, and signed AAB.
- [ ] On an isolated emulator or a backed-up phone, enter a controlled real period start and verify next-month period, fertility, ovulation, Today, Calendar, History, and mirror dates update together.
- [ ] Verify add, edit, remove, filter, pattern, import failure, export round trip, reminders, scrolling, memory, and fatal logcat scenarios.
- [ ] Commit the scoped change, push `main`, verify GitHub CI, upload to Closed Alpha, and request fresh confirmation before final Play review submission.
