# Code 16 reliability acceptance

Date: 2026-09-01

Device: Huawei YAL-L21, Android 10, ADB serial `BQLDU19927002646`.

## Data safety

- Exported the existing local data before mutation.
- Backup size: 10,163 bytes.
- SHA-256: `BF36BB5604275B5548E5D0A766C2F33E8602F689EB83798BF52470ED152CFEC4`.
- No app data, calendar data, or backup file was deleted.

## Physical-device scenarios

1. Baseline: cycle day 20, next period 2026-09-12, fertile window 2026-09-23 to 2026-09-29, ovulation estimate 2026-09-28.
2. New period start on 2026-09-01: Today changed to cycle day 1; next period moved to 2026-10-01; fertile window moved to 2026-09-12 to 2026-09-18; ovulation moved to 2026-09-17. October Calendar showed 2026-10-01 to 2026-10-05 and the following estimate on 2026-10-31.
3. Removal: removed the temporary connected period through the day overview. Baseline values returned exactly.
4. Cycle length change: changed the override from 30 to 31 days. Today moved the next estimate to 2026-09-13 and October Calendar moved the following estimate to 2026-10-14. Restoring 30 days returned the baseline.
5. Calendar filter: the sheet exposed only trackers present in local logs, selected count changed to 1 of 3, the header badge appeared, and clearing removed the badge. No data mutation was needed.
6. Calendar mirror: before the fix a warm launch rewrote many unchanged provider rows. After the fix a second launch performed provider queries only; no insert, update, or delete was logged.
7. Relaunch: no fatal exception was present in the captured startup log and the baseline values remained intact.

## Automated gates

- `testDebugUnitTest`: 98 tests, 0 failures after the calendar no-op regression was added.
- `lintDebug`: 0 errors. Remaining warnings are version/update notices plus the known adaptive-icon directory warning.
- `assembleRelease` and `bundleRelease`: passed with R8 and release signing.

## Release-install boundary

The phone currently contains code 16 signed with the Android debug key for data-preserving QA. Android correctly rejected installing the Play-signed APK over it because the certificates differ. The app was not uninstalled because that would erase the local database. Release compilation and signature verification are separate gates; Play delivery remains the production installation path.
