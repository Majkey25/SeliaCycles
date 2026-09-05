# Code 21 local profiles and calendar controls

Version: `0.9.0-beta.13`. Device: Huawei `BQLDU19927002646`, Android 10.

## Changes

Local profiles own separate SQLite databases, reminders, notification identities, and calendar-mirror event prefixes. The original database keeps its existing filename. Import, export, and clearing records apply only to the selected profile. Simple mode hides advanced input and fertility tracks without deleting stored details; Detailed mode opens advanced input and shows estimated fertile-window uncertainty.

Profile switching resets drafts and rejects stale document callbacks. Failed database reads show a retry action and block writes instead of exposing editable default settings. A deliberately invalid setting in a synthetic database verifies that recovery preserves the existing records.

The calendar has a full-width period action below its grid, a stronger ring for today, and a quieter selected-day ring. Daily fertility labels reuse the same calculation as calendar tracks. Calendar estimates cannot confirm ovulation or provide contraception; see the [NHS fertility-awareness guidance](https://www.nhs.uk/contraception/methods-of-contraception/natural-family-planning/).

## Verification

- `testDebugUnitTest`: 156 tests, zero failures or errors.
- `lintDebug`: zero errors, eight warnings. Warnings cover dependency/SDK updates, the existing resource qualifier, and the intentionally opaque icon asset used inside an adaptive launcher resource.
- Debug, QA, instrumentation, signed release APK and signed AAB builds succeeded.
- All 16 instrumentation tests passed together on the final build in 24.268 seconds, in the isolated `.qa` package. Coverage includes period editing and recalculation, frozen history, draft recreation, monthly navigation, import/export, independent profiles, stale callbacks, failed-read recovery, display modes, icon rendering, and eight real store captures.
- The icon test's original concrete drawable-type assertion failed because Huawei returns a `BitmapDrawable`. Checking the packaged adaptive XML and actual render exposed clipped tabs. Proportional 12% padding replaced fixed sizing. Both 48-pixel and 192-pixel platform renders pass the edge-clearance check and were visually inspected.
- Main app updates used `install -r`, without clearing personal data. Final cold launch: 2,430 ms, one sample. Earlier samples were 1,894 and 2,006 ms; this is not a controlled startup benchmark.
- Synthetic 10,000-record benchmark: state construction median 14.419 ms; status-only copy median 0.00625 ms. This is not an overall application speed claim.
- All eight store images are raw 1080 × 1920 captures of synthetic data. No generated app screen or marketing frame is included. Sizes and hashes are in `screenshots-manifest.json`.
- Temporary screenshot size 1080 × 1920 was restored to physical 1080 × 2340. QA packages were removed after testing. Final device release: 19:28:38 local time.

The screenshot review also reproduced a missed-period bug: August 1–3 records with a 28-day cycle showed a follicular phase on September 5 and moved the next period to September 26. The dashboard now retains August 29 as overdue and leaves the current phase unknown. Future calendar tracks remain projections. The regression failed before the fix; the updated domain tests pass. A later recorded start recalculates normally.

The day overview no longer offers an End action just because a period occurred in the past 14 days. It requires an active period, a recorded period covering that day, or a covering estimate. History chart labels now include complete localized dates, distinguishing two cycles in the same month.

## Delivery

Release target: the existing Google Play alpha track. Store availability is verified separately in Play Console and is not implied by local test results.
