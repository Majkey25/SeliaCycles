# Selia Cycles UI and Phase Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make calendar meaning visually obvious, compact oversized controls, and add optional evidence-based phase education and care.

**Architecture:** Keep the existing Compose navigation and data model. Add one calendar color helper and one phase-to-care mapping, then reuse existing components and localized resources. Do not add dependencies or speculative health logic.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android string resources, JUnit 4, Gradle, adb.

---

### Task 1: Lock the color and care behavior with failing tests

**Files:**
- Modify: `app/src/test/java/com/majkeylab/seliacycles/AppPaletteTest.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/SelfCareRecommendationTest.kt`

- [ ] Add a test that calls `calendarPredictedPeriodColor(Color(0xFFB71C1C))` and expects unchanged RGB plus alpha `0.28f`.
- [ ] Add a test that expects menstrual recommendations to start with `HEAT` and luteal recommendations to include `REST`, while every phase list remains distinct and duplicate-free.
- [ ] Run `./gradlew testDebugUnitTest --tests '*AppPaletteTest*' --tests '*SelfCareRecommendationTest*'` and verify failure because the helper and recommendation API do not exist.

### Task 2: Implement calendar-specific semantic colors

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/AppTheme.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Add `internal fun calendarPredictedPeriodColor(periodColor: Color) = periodColor.copy(alpha = 0.28f)`.
- [ ] Use the strong `periodColor` only for recorded bleeding.
- [ ] Use `calendarPredictedPeriodColor(periodColor)` for predictions in day cells, overlap marker, month metrics, and legend.
- [ ] Use `MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)` for fertility and `primary.copy(alpha = 0.20f)` for ovulation so neither dominates menstruation.
- [ ] Run the two focused tests and verify they pass.

### Task 3: Make the calendar legend scan cleanly

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Replace the two-column legend rows with one full-width `LegendItem` per state.
- [ ] Keep the legend collapsed below the calendar and retain complete descriptions.
- [ ] Keep calendar cells icon-free except the existing tiny user-entry dot.

### Task 4: Expand phase education without deterministic claims

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-cs/strings.xml`
- Modify: `app/src/main/res/values-sk/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`
- Modify: `app/src/main/res/values-pl/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`

- [ ] Rebuild `PhaseGuidanceCard` as a full-width column with a compact heading row so body text is not squeezed beside the phase icon.
- [ ] Keep physical signs and mood/energy visible by default.
- [ ] Add a local `expanded` state and Read more / Show less action.
- [ ] When expanded, show localized Emotions and relationships, Care for this phase, personal-pattern reminder, and medical disclaimer sections.
- [ ] Map menstrual, follicular, fertile/ovulation, and luteal phases to specific localized paragraphs using `@StringRes` helper functions.
- [ ] Keep every claim conditional and explicitly state that the estimate does not predict personality or confirm ovulation.

### Task 5: Make relief care phase-aware

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Create: `app/src/test/java/com/majkeylab/seliacycles/SelfCareRecommendationTest.kt`

- [ ] Change `SelfCareActivity` visibility to `internal` and add `recommendedSelfCareActivities(phase)` with fixed safe subsets.
- [ ] Pass the current phase into `SelfCareSheet`.
- [ ] Show the same evidence-based care paragraph at the top of the sheet and list only relevant activities for that phase.
- [ ] Keep the existing timers, instructions, stop conditions, and safety disclaimer.
- [ ] Run `SelfCareRecommendationTest` and verify pass.

### Task 6: Remove remaining confusing UI choices

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Replace three tall theme rows with one compact three-column icon selector.
- [ ] Replace the stroller icon for Trying to conceive with `FavoriteBorder`.
- [ ] Remove future ovulation and fertile-window dates from a selected day's status card; keep only states relevant to that day.
- [ ] Reduce settings category vertical padding and icon size without shrinking touch targets below 48 dp.

### Task 7: Release verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `CHANGELOG.md`
- Modify: `docs/play-store/DATA_SAFETY.md`
- Modify: `docs/play-store/STORE_LISTING.md`
- Create: `docs/qa/2026-09-01-code15-ui-phase-guidance.md`

- [ ] Bump to version code 15 and `0.9.0-beta.7`.
- [ ] Run `./gradlew clean testDebugUnitTest lintDebug assembleRelease bundleRelease --console=plain` with the verified release signing properties.
- [ ] Verify APK/AAB hashes, signing certificate, ReTrace mapping, and R8 metadata.
- [ ] Install the minified build only on `BQLDU19927002646` without clearing data.
- [ ] Verify Home, Calendar, expanded legend, day details, Read more, phase-aware relief care, compact themes, settings scroll, Czech copy, and fatal logcat.
- [ ] Review the diff, commit the scoped change, push `main`, and verify GitHub CI.
- [ ] Prepare Closed Alpha code 15. Ask for fresh confirmation immediately before the final Google Play review submission.
