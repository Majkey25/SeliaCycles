# Intuitive cycle dashboard design

## Product

Selia Cycles is for people who want to record a period in one tap and immediately understand what comes next. The primary action is Start period or End period. Optional health details stay secondary.

## Root cause

The current model builds fertility only from estimated period starts. A future period that the user already recorded is omitted as a fertility boundary, while `CyclePredictor` uses it as an anchor for the following prediction. Today therefore skips the recorded upcoming period, jumps one cycle ahead, and moves the fertile window out of view.

## Data behavior

- The next period is the earliest future date from recorded starts and current estimates.
- Fertility uses the same boundary everywhere: Today, Calendar, day overview, and calendar mirror.
- Saved historical estimates remain visible for prediction-versus-reality comparison.
- Profile gates still hide fertility for pregnancy, hormonal contraception, perimenopause, or menopause as already defined.
- No database migration or cloud service is added.

## Screen hierarchy

### Today

1. Compact gradient cycle hero: cycle day, phase, next period.
2. Large context-aware Start period or End period action.
3. Persistent upcoming timeline: fertile window, ovulation, next period.
4. Seven-day strip.
5. Compact current/next-month rows and optional daily details.

The current large forecast cards and separate dense insight card are replaced by this single hierarchy.

### Calendar

The selected month summary moves above the grid. It shows recorded start, estimate, fertile window, and ovulation before the user scans individual days. Existing connected spans remain.

### Day overview

The sheet keeps the quick period action first, then one concise cycle-status block. Optional recorded details and prediction accuracy follow. The repeated calendar legend is removed from this sheet because the calendar already owns it.

## Interaction and accessibility

- One dominant action per state.
- Minimum Material touch targets stay unchanged.
- Existing palette contrast helpers and localized date formatting remain.
- No decorative animation or new dependency.

## Verification

- RED/GREEN unit test for a future recorded period staying the next fertility boundary.
- Existing prediction, import, profile, palette, and mirror tests.
- Huawei cold launch, one-tap action visibility, current/next month navigation, day-sheet scrolling, and logcat crash check.
