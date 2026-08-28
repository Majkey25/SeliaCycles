# Local Cycle Prediction Design

## Goal

Keep Selia Cycles local and simple while showing useful estimates for the current and next month.

## Product behavior

- A recorded bleeding day belongs to the same period when it is at most two days after the previous recorded bleeding day.
- The first day of the newest recorded period is always the prediction anchor.
- A newly recorded real period therefore replaces the old forecast anchor immediately.
- The Today screen shows two compact rows: this month and next month.
- A row shows `Recorded` when a real period starts in that month, `Estimated` when a forecast exists, or `No estimate` without history.
- The Calendar screen marks every forecasted period day in the displayed month, not only one global next date.
- Predictions remain estimates and never claim medical certainty.

## Prediction model

Use deterministic local statistics only.

1. Group recorded bleeding days into periods.
2. Calculate intervals between starts.
3. Learn a personal baseline from plausible recent intervals.
4. Normalize a long interval only when it closely matches two or more baseline cycles. This treats missed logging as missing cycles instead of teaching an artificial 56- or 84-day cycle.
5. Reject isolated outliers around the median, then use a recency-weighted mean of at most eight intervals.
6. Calculate uncertainty from recent absolute deviations.
7. Starting from the latest real period, generate future starts through the next twelve months.

Defaults remain user configurable and are used when history is insufficient.

## Privacy and transfer

- Remove Google sign-in, Firebase, partner calendars, Health Connect, My Calendar import, and manual backup/restore.
- Keep the existing SQLite schema so imported and manually logged records survive an APK update.
- Keep Android native device-to-device transfer for the database and settings.
- Exclude all Android 12+ cloud-backup domains.
- A native backup agent emits data only when Android marks the transport as device-to-device, including on the authorized Android 10 Huawei.

## Validation

- Unit tests cover real-start re-anchoring, skipped cycles, outliers, current month, next month, no-history, and existing behavior.
- Android test/lint/APK/AAB gates pass.
- Install with `adb -s BQLDU19927002646 install --user 0 -r ...` so the phone database is preserved.
- Live checks cover recorded-current-month, future forecast, edit flow, settings, cold start, package permissions, and crash buffer.
