# Selia Cycles 0.3.0-beta.1 acceptance

Date: 2026-08-28

## Build evidence

- Command: `gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease --console=plain`
- Result: `BUILD SUCCESSFUL`; 112 tasks, 110 executed and 2 up-to-date.
- Predictor tests: 12 run, 0 failures, 0 errors, 0 skipped.
- Android Lint: 0 errors, 7 update/toolchain warnings.
- Locale resource sets: 139 named strings/plurals in each of English, Czech, Slovak, German, Polish, and Spanish.
- `git diff --check`: no whitespace errors.
- Firebase, Credential Manager, Google ID, and Health Connect are absent from `debugRuntimeClasspath`.
- Merged release manifest has no `INTERNET`, `ACCESS_NETWORK_STATE`, or health-data permission.

## Artifacts

- Debug APK SHA-256: `FC0F2C9E8936B30FB4E50FEEDA9A49E8F5A51B03AE3CBD72B32B061EBAF3AA31`
- Signed release AAB SHA-256: `42981904C255E6F273558443F94103B482EC0747EA6039BB47CAB9837E0B123B`
- APK signature scheme v2 verified.
- AAB: `jar verified`.
- Microsoft Defender custom scans completed with no matching threat detection.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10. Every ADB command was pinned to this serial. No emulator was used.

### Upgrade and data preservation

- Installed with `adb -s BQLDU19927002646 install --user 0 -r ...`.
- Version changed from `0.2.0-beta.1` / code 2 to `0.3.0-beta.1` / code 3.
- `firstInstallTime` remained `2026-08-28 14:15:59`.
- Existing database remained present at 28,672 bytes.
- History remained 28 recorded starts; latest real start remained August 28, 2026.

### Prediction behavior

- Previous build on the same data: 44-day average and September 26–October 26 window.
- New build: 29-day average and September 25–27 window.
- This month shows `Recorded start: Aug 28, 2026`.
- Next month shows `Estimated start: Sep 25, 2026 – Sep 27, 2026`.
- Added an adjacent real bleeding start on August 27: cycle day changed to 2 and forecast re-anchored to September 24–26.
- Deleted the temporary August 27 record: start, cycle day, and September 25–27 forecast returned exactly. History remained 28 starts.

### Negative and regression paths

- Predictions OFF hides forecast rows and now says `Predictions are turned off in Settings.`
- Predictions were restored ON and the September 25–27 estimate returned.
- Calendar day edit, Period selection, Save, Delete record, History, Settings navigation, System theme, and System language were exercised live.
- Settings contains five clear categories. Data and transfer contains only device transfer information and Delete all data.
- Cold launch completed in 1,602 ms, process remained running, and the crash buffer contained no Selia Cycles crash.
- Installed requested permissions contain notifications and WorkManager scheduling permissions only; no internet, network-state, or health-data permission.

## Screenshots

- [Current and next month prediction](screenshots/phone-local-prediction-home.png)
- [Local data and device transfer settings](screenshots/phone-local-data-settings.png)
