# Calendar, fertility, and personal mood design

Date: 2026-08-29

## Goal

Keep the calendar readable while showing recorded periods, the estimate that existed before reality was logged, current future estimates, estimated ovulation, a fertile window, and a cautious personal mood trend. Optionally mirror the same cycle layers into a writable Android calendar that Google Calendar or Outlook already synchronizes.

## Scope

- Preserve one baseline period estimate for each month. A real period does not overwrite that baseline.
- Recalculate future months after every saved or deleted log.
- Backfill the last 12 months from only the information that existed before each month.
- Show recorded and estimated spans together in month views.
- Connect adjacent days into rounded horizontal segments within each calendar week.
- Estimate ovulation as 14 days before an estimated next period.
- Show the fertile window from 5 days before through 1 day after estimated ovulation.
- Derive a mood trend only from the user's own logged moods in comparable cycle phases.
- Mirror recorded periods, baseline/future estimates, fertile windows, and ovulation into one selected Android calendar.
- Keep English, Czech, Slovak, German, Polish, and Spanish resources complete.

## Safety boundaries

- Ovulation and fertile dates are calendar estimates. They do not confirm ovulation and must not be used as contraception.
- Basal temperature can support retrospective awareness but the app does not claim that one reading confirms ovulation.
- A mood trend is not a diagnosis or a promise about a future mood.
- Mood output requires at least three historical mood entries from at least two completed cycles in the same phase. Otherwise the UI says there is not enough personal data.
- Notes, symptoms, measurements, intimacy, and raw mood logs never leave app-private storage.
- Calendar events contain short cycle labels only. They are private by default; explicit Partner view uses the calendar's normal sharing visibility. Selia Cycles has no network permission; the chosen calendar provider controls any account synchronization.

The fertility estimate follows ACOG's average of ovulation about 14 days before the next period and its 5-days-before through 1-day-after fertile interval. NHS and the US Office on Women's Health both warn that ovulation timing varies, so every fertility surface remains explicitly estimated.

## Data model

`ForecastSnapshot` stores `month`, `periodStart`, `earliestStart`, `latestStart`, and `periodLength`. `CycleStore` owns a `forecast_snapshots` SQLite table and inserts snapshots with `CONFLICT_IGNORE`; an existing monthly baseline is immutable.

On load, `ForecastSnapshotPlanner`:

1. backfills missing months from `today - 12 months` through the current month using only bleeding logs before that month;
2. stores the current-month baseline before a new log can change it;
3. never stores future months, so future predictions continue to move with new input.

The selected device calendar ID lives in `noBackupFilesDir`, not the transferable database, because Android calendar row IDs are device-specific.

## Prediction layers

`CycleInsights` derives:

- period spans from the preserved monthly baseline or current dynamic prediction;
- estimated ovulation at `periodStart - 14 days`;
- fertile start at `ovulation - 5 days` and fertile end at `ovulation + 1 day`;
- the current cycle phase: menstrual, follicular, fertile, or luteal;
- a personal phase mood trend from historical `Mood` values when the minimum evidence threshold is met.

No population mood stereotype is used. Ties or insufficient evidence produce no mood estimate.

## UI

The Today screen keeps the current and next month rows. If a month has a recorded period and a saved baseline, both rows are shown with the date difference.

The Calendar screen adds a month comparison below the grid. Day priority is recorded period, predicted period, ovulation, fertile window, then normal day. A recorded/predicted overlap uses the recorded fill plus a predicted outline. Adjacent days of the same visible layer share square inner edges and rounded outer edges, forming one pill across a week row.

A compact Cycle insight block shows estimated ovulation, fertile window, cycle phase, and the personal mood trend or its evidence requirement. Optional Partner view uses the same plain-language block; it does not create a partner account or upload data.

Settings adds Calendar sync and Partner view. Calendar sync requests `READ_CALENDAR` and `WRITE_CALENDAR` only when opened, lists visible writable synchronized calendars, supports one selected target, and provides a disconnect action.

## Calendar mirroring

`CalendarMirrorPlanner` emits bounded all-day spans for 12 months of history and 12 months of future estimates. `CalendarMirror` uses one atomic `CalendarContract` batch: delete only events tagged with Selia's package/custom URI, then insert the current plan. Events are free-time and private unless the user explicitly enables Partner view. Google Calendar, Outlook, Exchange, or another installed provider performs account synchronization.

Mirroring runs after app launch, log changes, prediction-setting changes, calendar selection, and disconnect. Provider failures never block loading private cycle data.

## Verification

- Unit tests cover immutable monthly baselines, historical reconstruction, future recalculation, fertility dates, mood evidence thresholds, and mirror event planning.
- Full Gradle unit, lint, debug APK, and signed release AAB gates run.
- Physical Huawei QA covers permission denial, selecting a writable calendar, recorded/predicted/fertility event creation, re-anchoring after a real period, adjacent pill rendering, preserved historical baseline, disconnect cleanup, and an unchanged old daily-log workflow.
