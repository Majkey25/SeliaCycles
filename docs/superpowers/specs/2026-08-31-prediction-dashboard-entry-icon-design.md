# Prediction, dashboard, entry, and icon design

Status: approved on 2026-08-31

## Source evidence

The supplied generation 7 `.pc` backup contains 144 period rows from 2014-12-25 through 2026-07-09. Forty-seven period lengths are negative, and none are zero placeholders. The importer correctly uses the absolute nonzero length.

The prediction error starts after import for two separate reasons:

- `CyclePredictor` uses `defaultCycleLength` and `defaultPeriodLength` only when history cannot provide a value. Changing either setting has no visible effect when the backup contains many periods.
- `PeriodActions.start` writes the expected future duration into `DayLog` as recorded bleeding. The calendar then presents predicted future days as real data.

The Huawei currently uses the Light theme and the Ocean palette.

## Prediction rules

Automatic prediction remains the default. It uses recent valid history and the existing outlier handling.

Changing the cycle length or the period length creates a manual override. The override controls future estimates immediately. **Use automatic estimate** removes both overrides and returns to learned values.

Past and current saved prediction snapshots remain unchanged for comparison with recorded reality. Future estimates always use the latest logs, profile, luteal phase, and manual overrides.

The prediction UI labels automatic and manual values. A manual value must never look like a learned average.

## Record only real period days

**Start period** records only the selected start day and stores an active period start. It does not create future `DayLog` rows.

While a period is active, the primary action is **End period**. Ending the period fills the inclusive range from the active start through the selected end date. The range limit is 14 days.

Removing the active period clears the active start. An active start older than 14 days does not fill a longer range. The day editor remains available for corrections.

`CyclePredictor` excludes the unfinished active period from its period-duration average. The active start still anchors the current cycle.

## Home is a compact navigation dashboard

The Today screen contains short clickable summaries. Detailed text and graphs move to their existing detailed destinations.

The dashboard routes each summary to one clear destination:

- The cycle hero opens the current month in Calendar.
- The next-period row opens the predicted start in Calendar.
- The fertile-window row opens the fertile window in Calendar.
- The ovulation row opens the estimated ovulation day in Calendar.
- The phase card opens a full phase sheet with physical signs, mood, energy, safety text, and the current dates.
- The cycle-analysis row opens History, where metrics and graphs remain.
- The day action opens today's overview or starts and ends the active period.

The Today screen does not show the week strip, monthly forecast list, or prediction charts inline. The screen shows one icon, one label, and one value for each summary.

## Phase details and relief care

The compact phase card shows the phase name and one short sentence. The full phase sheet uses the existing evidence-cautious guidance.

**Relief care** stays visible on Today when the user enables care. It is not hidden inside the phase text. The sheet keeps the existing eight activities and adds four low-risk options:

- lower-back stretch;
- knees-to-chest rest;
- pelvic rocking;
- guided muscle relaxation.

Each activity has a distinct Material icon, a duration, instructions, and the existing stop-if-worse safety text. The feature does not claim treatment or guaranteed relief.

## Defaults

New installations and **Delete all data** use:

- `AppTheme.LIGHT`;
- `AppPalette.OCEAN`;
- automatic prediction with no manual override.

Existing users keep their saved theme and palette.

## Launcher and Play artwork

The logo uses a black circular disc, white cycle arrows, and a red drop.

The adaptive launcher foreground has transparent pixels outside the black disc. The adaptive background is transparent, so supported launchers do not show a white square. The arrow strokes and heads use consistent widths and even gaps at small sizes.

The Google Play icon stays opaque and uses a full black background. The feature graphic uses the same white-arrow logo. The monochrome layer contains the arrow and drop silhouette for themed icons.

## `.pc` transfer

The approved [My Calendar compatible `.pc` export design](2026-08-31-my-calendar-pc-export-design.md) remains part of code 11. The new generation 7 backup is the primary regression input.

The app does not commit the private backup. Tests use a sanitized fixture with the same container structure and the last-cycle pattern required to reproduce the prediction bug.

## Verification and release

Code 11 is complete only when these checks pass:

- a failing regression test proves that a manual override changes the next future month;
- a failing regression test proves that **Start period** creates no future recorded days;
- the generation 7 fixture imports all nonzero signed periods;
- Selia `.pc` export and import preserve all Selia data;
- a clean My Calendar installation accepts the common period and note data;
- every dashboard summary opens the expected month, day, phase sheet, or History;
- the physical Huawei shows the transparent launcher edge and the Light plus Ocean default after a clean QA install;
- the full unit, lint, debug, release APK, and signed AAB build passes.

The release uses version code 11 and version name `0.9.0-beta.3`. The current code 10 Alpha submission stays in review until code 11 passes local acceptance.
