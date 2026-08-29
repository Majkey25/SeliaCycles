# Selia Cycles 0.4.0-beta.1 acceptance

Date: 2026-08-29

## Build evidence

- Command: `gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease --console=plain -PseliaCyclesKeystoreProperties=<approved-local-properties>`.
- Result: `BUILD SUCCESSFUL`; 113 tasks, 111 executed and 2 up-to-date.
- Unit tests: 25 run, 0 failures, 0 errors, 0 skipped.
- TDD covered monthly baseline reconstruction, immutable snapshots, fertility dates, personal mood evidence thresholds, same-month short cycles, mirror planning, and idempotent calendar diffing.
- Android Lint: 0 errors, 7 existing update/toolchain warnings.
- Locale resource sets: 175 named strings/plurals in each of English, Czech, Slovak, German, Polish, and Spanish.
- Merged release manifest contains optional `READ_CALENDAR` and `WRITE_CALENDAR`; it contains no `INTERNET`, `ACCESS_NETWORK_STATE`, or health-data permission.
- No dependency was added.

## Artifacts

- Debug APK SHA-256: `17645C37031796C7C7EEF9D19FD869530319374DBD2DE3F08EB5EC43CB20E5BD`.
- Signed release AAB SHA-256: `3CDBB7EA69E10E9FCB82B7CCF92848E3D665E139A6C2B953E8D868D1B43890A4`.
- Debug APK signature scheme v2 verified.
- Release AAB: `jar verified`.
- AAB upload-certificate SHA-256 is `A4:F0:7E:70:CC:0D:E4:22:44:11:FD:CD:EB:81:E3:E1:1D:5B:4B:A9:49:23:0C:F1:08:F3:76:3A:39:FD:4A:1E`, exactly matching Google Play Console.
- Microsoft Defender custom scans completed with no matching detection for either artifact.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10. Every ADB command was pinned to this serial. No emulator was used.

### Upgrade and prediction history

- Installed code 4 over code 3 with `adb -s BQLDU19927002646 install --user 0 -r ...`.
- `firstInstallTime` remained `2026-08-28 14:15:59`; the existing 28 recorded period starts remained available.
- SQLite upgraded from version 2 to 3 without data loss.
- August shows both `Recorded start: Aug 28, 2026` and `Reconstructed estimate: Aug 11, 2026 – Aug 15, 2026` with `Reality versus estimate: +15 days`.
- September remained dynamically estimated at September 25–27 after the historical baseline was created.

### Fertility and mood

- Today showed `Phase: Menstrual`.
- Estimated ovulation showed September 12, 2026.
- Estimated fertile window showed September 7–13, 2026.
- Mood correctly stayed unavailable because the existing data did not meet the minimum three comparable entries from two completed cycles.
- The non-contraception warning remained visible with the estimates.

### Calendar UI

- Added a temporary real period entry for August 29 next to August 28.
- Both days rendered as one continuous rounded range rather than two independent circles.
- Deleting the temporary entry restored the original single recorded day and kept the saved August estimate unchanged.
- Recorded, saved/dynamic prediction, fertile, ovulation, and overlap semantics are exposed to accessibility services.

### Google Calendar / Outlook-compatible mirror

- Denied the Android calendar permission once: both calendar permissions stayed denied and no state changed.
- Granted the permission on the next request: Selia listed the writable synchronized Google calendar and the local Phone calendar.
- Selected the Google calendar. Selia created 80 bounded all-day events covering the retained 12-month history and 12-month forecast horizon.
- Only short labels were written: recorded period, estimated period, estimated fertile window, and estimated ovulation. Notes, symptoms, measurements, intimacy, and raw mood logs were absent.
- Adding August 29 extended the existing recorded event; deleting it restored the end date.
- The first full-replacement implementation showed transient provider duplicates. It was replaced with tested key-based update/insert/delete diffing. A cold launch then kept the provider count at exactly 80 immediately, with no transient duplicate growth.
- Cold launch completed in 1,696 ms and the crash buffer contained no Selia Cycles crash.
- `Stop sync` removed every Selia-created provider event; the final provider count was 0. The selected calendar ID file was also absent from `no_backup`, leaving the user's external calendar clean after QA.

## Screenshots

- [Saved estimate, fertility, ovulation, and mood evidence](screenshots/phone-fertility-insight.png)
- [Connected adjacent recorded days](screenshots/phone-connected-period-span.png)
