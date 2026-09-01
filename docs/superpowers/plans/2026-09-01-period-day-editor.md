# Period day editor implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add direct period-day selection, separate daily information editing, and replace calendar dots with information underlines.

**Architecture:** Keep period mutations in `PeriodActions` and persist them through one `MainViewModel` operation. Compose owns only transient date selection. Reuse the existing calendar colors, sheets and `DayLog` model without adding a dependency or database migration.

**Tech stack:** Kotlin, Jetpack Compose Material 3, SQLite, Kotlin test, Gradle.

---

### Task 1: Period selection model

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/PeriodActions.kt`
- Test: `app/src/test/java/com/majkeylab/seliacycles/PeriodActionsTest.kt`

- [ ] Add failing tests proving replacement can shorten, extend and clear one period while preserving optional details and other periods.
- [ ] Run `./gradlew testDebugUnitTest --tests com.majkeylab.seliacycles.PeriodActionsTest --console=plain` and verify the new tests fail because `replace` does not exist.
- [ ] Add `periodDays`, `isValidSelection` and `replace` with a 14-day bound and no future bleeding.
- [ ] Run the focused test and verify it passes.

### Task 2: Persistence and separated information ownership

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Add `savePeriodDays(day, selectedDays)` that uses `PeriodActions.replace` and updates `activePeriodStart` only when necessary.
- [ ] Split `DaySheetMode.EDIT` into `PERIOD` and `DETAILS`.
- [ ] Make the details sheet preserve `initial.bleeding` and remove `Flow.NONE` as a period toggle.
- [ ] Make deleting details preserve the current period day.
- [ ] Compile with `./gradlew compileDebugKotlin --console=plain`.

### Task 3: Period editor UI

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/main/res/values*/strings.xml`

- [ ] Add a compact four-week `PeriodEditorSheet` using seven-column date rows and circular date toggles.
- [ ] Disable future dates and show selection validation inline.
- [ ] Add distinct `Edit period` and `Add/Edit information` actions to the day overview.
- [ ] Add matching translations for English, Czech, Slovak, German, Polish and Spanish.
- [ ] Compile and run lint after the UI slice.

### Task 4: Calendar information underline

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`
- Modify: `app/src/test/java/com/majkeylab/seliacycles/CalendarUiModelTest.kt`
- Modify: `app/src/main/res/values*/strings.xml`

- [ ] Keep the existing test proving bleeding alone has no optional-information marker.
- [ ] Remove the saved-prediction overlap underline from recorded period cells.
- [ ] Replace the top-right blue dot with a centered blue underline below the day number.
- [ ] Change the legend marker and wording to match the underline.

### Task 5: Release acceptance

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Create: `docs/qa/2026-09-01-code17-period-editor.md`

- [ ] Bump to code 17 and document the user-visible behavior.
- [ ] Run a clean unit-test, lint, APK and AAB gate with release signing.
- [ ] Install the matching debug build on `BQLDU19927002646`, back up data, test extend/shorten/remove/detail-preservation/prediction recalculation, and restore the original state.
- [ ] Review the final diff, commit, push and wait for green GitHub CI.
- [ ] Upload the signed code 17 AAB to closed Alpha and stop for the required action-time confirmation before final submission.
