# Fertility, daily guidance, and profiles: code 27

Version `0.9.0-beta.19`. Physical verification and publication are pending.

## Verified locally

- 174 JVM tests passed with `testDebugUnitTest`.
- The new historical-fertility regression failed before the fix. Saved forecast dates remain unchanged while the fertility window follows the recorded next period.
- A shortened-period regression failed before the phase fix. The day after a confirmed three-day period no longer uses a seven-day setting to show menstrual guidance.
- A later-bleeding mood regression failed before the sample-selection fix. Confirmed day-seven observations are no longer discarded by a three-day duration setting.
- Leap-year, month-boundary, and year-boundary date checks passed. Bleeding-duration changes alone do not shift the ovulation estimate.
- `lintDebug`: zero errors and eight existing warnings.
- Debug, QA, instrumentation, signed release APK, and signed AAB builds passed.
- APK verification reports the existing upload certificate SHA-256 `a4f07e70cc0de4224411fdcdeb81e3e11d5b4ba949230cf108f3763a39fd4a1e`. AAB signature verification passed with self-signed-certificate and ZIP-stream-layout warnings from `jarsigner`.

## Physical verification blocked

Huawei `BQLDU19927002646` went offline during the preceding project's test window. After that project released it, `adb -s BQLDU19927002646 get-state` returned `error: device offline`. A serial-scoped `reconnect` completed, but a fresh `get-state` still returned offline. No server restart, device reboot, installation, data deletion, or system-setting change was performed by Cycles.

The QA APK and test APK are built. Pending device checks cover distinct early/middle/later guidance, care navigation, period-edit ovulation refresh after recreation, profile name/icon persistence, and the existing regression suite. Tests use the separate `.qa` package and synthetic records.

Code 27 is not published to Google Play while this check is blocked. Code 26 (`0.9.0-beta.18`) was confirmed available to Alpha testers in Play Console on 19 September 2026.

## Limits

This release corrects software behavior. It does not establish clinical prediction accuracy or promise parity with another app. [The evidence note](../research/2026-09-19-daily-guidance.md) documents the guidance sources and counting convention. There are no new dependencies, permissions, cloud services, or database migrations.
