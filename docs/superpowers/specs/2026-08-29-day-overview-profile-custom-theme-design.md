# Day Overview, Profile, and Custom Theme Design

## Goal

Make a selected day informative before it becomes editable, remove modal-scroll flicker, add a medically honest local profile, and expand appearance settings with more preset palettes plus user-defined colors.

## Day flow

Tapping a calendar or week day opens a read-only day overview. It shows:

- the selected date and its calendar status: recorded period, saved/current estimate, fertile estimate, ovulation estimate, or ordinary day;
- the saved monthly estimate beside the recorded start, including exact, early, or late difference when both exist;
- cycle phase and the calendar fertility status valid for that date;
- recorded flow, spotting, mood, symptoms, reproductive signs, wellbeing trackers, measurements, intimacy, and private note when present;
- a compact legend explaining only the layers visible for the selected day.

The footer contains Close and Edit record. Explicit logging actions such as the plus button and Log today may still open the editor directly.

## Scroll behavior

Both overview and editor use a fully expanded modal sheet with sheet drag gestures disabled. One bounded vertical content scroller sits above a fixed footer. This removes competition between sheet dragging and content scrolling, which caused flicker near the bottom. Back from the editor returns to the overview; Save closes the flow.

## Local profile

Settings gains a Profile page with optional age, height, and baseline weight plus typed choices:

- goal: track cycle, trying to conceive, or avoid pregnancy;
- life situation: regular cycles, pregnant, hormonal contraception, perimenopause, or menopause.

The profile stays in app-private SQLite and device-to-device transfer. Age and body measurements are context only; they do not silently alter cycle length because the app has no validated clinical model for that. Personal bleeding history remains the prediction input.

Prediction behavior changes only where medically justified:

- pregnancy and menopause suppress future period and fertility estimates and cancel reminder work;
- hormonal contraception suppresses ovulation/fertility estimates because some methods stop ovulation;
- perimenopause keeps history-based period estimates but suppresses calendar ovulation/fertility estimates and shows an irregularity warning;
- trying to conceive prioritizes fertility information;
- avoid-pregnancy mode shows a strong warning that Selia is not contraception.

Historical saved estimates remain available for comparison.

## Appearance

Preset palettes become Selia, Rose, Ocean, Forest, Sunset, and Lilac. A seventh Custom palette accepts three mechanical `#RRGGBB` values for primary, secondary/period, and fertility colors. Every valid edit applies immediately and updates the preview. Invalid or incomplete hex text is never persisted.

The theme derives readable foreground and container colors from the custom values. Hero-gradient endpoints are darkened when required so white text keeps at least 4.5:1 contrast. Reset restores the current Selia defaults.

## Storage and versioning

SQLite schema version 5 adds profile and custom-color columns with safe defaults. App release version becomes code 6 / `0.6.0-beta.1`; code 5 remains untouched on Google Play while code 6 is tested locally.

## Safety sources

- ACOG: perimenopause changes cycle regularity and ovulation may not occur every month.
- Office on Women’s Health: pregnancy and menopause stop ovulation; perimenopause may skip ovulation.
- NHS: some hormonal contraception stops ovulation.

The app remains informational and does not diagnose pregnancy, menopause, fertility, or contraceptive safety.

## Verification

Unit tests cover profile validation, prediction gates, historical estimate retention, day comparison outcomes, preset/custom palette uniqueness, custom hex parsing, and gradient contrast. Huawei QA covers day-overview-first navigation, bottom scrolling without flicker, editor return/save, profile persistence/gating, all preset previews, custom colors, cold restart, and restoration of the user’s original settings and records.
