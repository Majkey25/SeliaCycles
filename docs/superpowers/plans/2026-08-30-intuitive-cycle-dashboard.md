# Intuitive Cycle Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the fertile window correct after any period input and make the main cycle actions and dates visible without hunting through cards.

**Architecture:** `CycleInsights` becomes the single source for upcoming period and fertility boundaries. Existing Compose screens consume that result and remove duplicated calculations. SQLite schema stays unchanged.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Java time, existing JUnit/Kotlin tests, Android ADB.

---

### Task 1: Canonical upcoming cycle

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleInsights.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/CycleInsightsTest.kt`

- [ ] Add a failing test with recorded starts on August 28 and September 26 while the reference date is August 30. Assert `nextPeriodStart == September 26`, ovulation is September 12, and fertile window is September 7 through 13.
- [ ] Run `./gradlew testDebugUnitTest --tests com.majkeylab.seliacycles.CycleInsightsTest` and confirm the assertion fails because the current result points to the later generated cycle.
- [ ] Add `nextPeriodStart` to `DailyCycleInsight`. Select the earliest future date from actual period starts and period estimates. Build fertility from that date.
- [ ] Add `fertilityEstimates()` that includes future recorded starts and reuses `fertilityForPeriod()`.
- [ ] Run the focused test and the full unit suite.

### Task 2: Shared calendar fertility

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CalendarMirrorPlanner.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/CalendarMirrorPlannerTest.kt`

- [ ] Add a failing mirror test proving a future recorded period still produces the preceding fertile and ovulation events.
- [ ] Replace local fertility mapping in Calendar and mirror planning with `CycleInsights.fertilityEstimates()`.
- [ ] Run both focused test classes and confirm green.

### Task 3: Today hierarchy

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values*/strings.xml`

- [ ] Use `todayInsight.nextPeriodStart` in the hero.
- [ ] Put one context-aware Start/End action directly below the hero.
- [ ] Replace the separate bulky forecast and insight cards with a compact timeline that always shows next period, fertile window, and ovulation when available.
- [ ] Keep the week strip and optional daily-entry summary below the timeline.
- [ ] Compile with `./gradlew assembleDebug`.

### Task 4: Calendar and day overview

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Render `MonthComparison` before the month grid.
- [ ] Use canonical fertility estimates for calendar layers and month summary.
- [ ] Remove the repeated legend from Day overview and keep one concise status block before recorded values.
- [ ] Compile and run lint.

### Task 5: Device acceptance

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `CHANGELOG.md`

- [ ] Bump to version code 7 and `0.7.0-beta.1` after behavior passes.
- [ ] Run `./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleRelease bundleRelease --console=plain`.
- [ ] Install only with `adb -s BQLDU19927002646 install -r app/build/outputs/apk/debug/app-debug.apk`.
- [ ] Verify Today above-fold hierarchy, Start/End state, future recorded boundary, fertile/ovulation dates, Calendar month navigation, day-sheet scrolling, Settings Back, and empty crash log.
- [ ] Run `git diff --check` and `git status --short`. Do not commit, push, or publish without explicit approval.
