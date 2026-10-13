# Code 33 verification

Version: `0.9.0-beta.25`. App/test source: `2eee1b46a81b6af74ab760a26720548182c68cbd`.

## Changes

- Added Material, AMOLED, Graphite, Sand and Mint appearance presets, independent light/dark custom palettes and 85–140% text sizing.
- Corrected fertility recalculation and historical forecast comparisons, duplicate snapshot saves and transactional reads of logs/settings.
- Hardened compatible backup parsing and restore, profile isolation, calendar-provider mutations and disabled-reminder delivery.
- Preserved calendar navigation across tabs/profiles, improved large-text actions, and virtualized long histories.
- Explicitly enabled full device-transfer backup while retaining cloud exclusions. No network service or permission was added.

## Checks performed

- Local JVM suite: **211 tests, 0 failures, 0 errors**. Debug lint: **0 errors, 11 warnings**. QA/test APK and signed release APK/AAB builds passed.
- Physical Huawei YAL-L21, Android 10/API 29, serial `BQLDU19927002646`: complete instrumentation suite **88/88 passed**, 171.587 seconds, October 13. No emulator was used. A separate Czech screenshot fixture passed in 7.917 seconds; English/Czech captures contain synthetic records only.
- Before the storage fix, the duplicate-snapshot regression failed on this same phone with `IllegalStateException` in `saveForecastSnapshots`. It passed in the final suite. The concurrent-read stress check passed both versions; its earlier race was identified from the nontransactional source path, not claimed as a reproduced device failure.
- Device scenarios include automatic period-day filling, editing/removing periods, future forecast and ovulation updates, retained original forecasts, activity recreation, large histories, import/export, failed-save recovery, read-only profiles, widget settings and disabled calendar-sync cleanup failure/reload.
- Only isolated `.qa` packages were installed and removed. Production app data and global device settings were untouched. All ADB operations ended before the next project's reservation.
- GitHub CI for the app-source commit passed. Emulator CI is an optional manual job and was not run.
- `bundletool validate`, APK signature verification and 16 KiB ZIP alignment passed. The upload certificate matches the preceding release. `jarsigner` verified the bundle with the same self-signed/no-timestamp and JarInputStream warnings present in the preceding bundle.

## Artifact SHA-256

| Artifact | SHA-256 |
| --- | --- |
| QA APK | `614ae765684822e6941d0e8db74c90ec25f148007298e0e5914bf0fb2caceba7` |
| QA test APK | `97460e6cdf174a4c1cd235cc3b45fe695fbfa59b8004ae91d275549590878f35` |
| Signed release APK | `b8faccd4fc0a5ece6d66445db70aaec4fcbfe519346c1ff4fd374992144cb150` |
| Signed release AAB | `23db15a53c51817609600fea04af1d01881da97d91c32a5a467ca55ae75cc205` |

## Review and limits

Independent domain/integration reviews informed the changes. The requested Gemini 3.8 Flash High batch launched 65 of 100 planned source-only tasks: 55 completed, four partial and six failed. Antigravity returned HTTP 429 account quota errors and the remaining 35 were not launched. Findings were checked against callers, tests and runtime; unsupported suggestions were rejected. No scheduled retry was created.

This is software regression verification, not clinical prediction-accuracy validation. No real two-device migration, API 31+ wallpaper-color device test, or cloud Google Calendar round trip was performed. Calendar mutation tests use an isolated Android provider. Native AndroidX graphics binaries are pre-stripped; `debugSymbolLevel=FULL` cannot recover their absent symbols, so Google's advisory may remain. No unrelated calendar events or real health records were deleted.
