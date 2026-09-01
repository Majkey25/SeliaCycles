# Code 17 period editor acceptance

Date: 2026-09-01

Device: Huawei YAL-L21, Android 10, ADB serial `BQLDU19927002646`.

## Data safety

- Exported the current local data before mutation.
- Backup size: 10,163 bytes.
- SHA-256: `BF36BB5604275B5548E5D0A766C2F33E8602F689EB83798BF52470ED152CFEC4`.
- The original period dates and Today values were restored after every destructive scenario.

## Physical-device scenarios

1. Baseline remained cycle day 20, next period 2026-09-12, fertile window 2026-09-23 to 2026-09-29, and ovulation estimate 2026-09-28 after the code 17 update.
2. Opening 2026-08-13 showed separate `Edit period` and `Add information` actions. The period editor selected the five recorded dates 2026-08-13 to 2026-08-17.
3. Replacing those dates with 2026-08-12 to 2026-08-16 changed only the recorded span. Today immediately moved the next period to 2026-09-11, fertile window to 2026-09-22 to 2026-09-28, and ovulation to 2026-09-27.
4. Restoring 2026-08-13 to 2026-08-17 returned every baseline estimate exactly.
5. `Clear period` produced a valid zero-day selection and removed all five recorded dates. Re-selecting 2026-08-13 to 2026-08-17 restored the original data and estimates.
6. Adding a temporary good mood to non-period date 2026-09-01 created daily information without creating bleeding. Calendar accessibility reported `Recorded values` and the cell showed a blue underline.
7. `Delete information` removed the temporary mood, removed the underline, and left 2026-09-01 without bleeding. Baseline estimates remained unchanged.
8. Recorded menstruation 2026-08-13 to 2026-08-17 showed one connected red span with no underline. A separate date containing optional information showed the short blue underline and no dot.
9. The final editor places its instruction below the date grid. Calendar, edit, and period icons identify the range, instruction, and selected-day count.

## Automated gates

- Period selection tests cover exact replacement, complete clearing, other-period isolation, optional-data preservation, future rejection, and the 14-day bound.
- Daily-information tests prove save and delete preserve period membership and that optional information cannot create bleeding.
- `clean testDebugUnitTest lintDebug assembleRelease bundleRelease`: passed.
- Unit tests: 102, 0 failures.
- Android lint: 0 errors, 8 version/adaptive-icon warnings already reviewed for code 16.
- Release APK SHA-256: `5C9D257AA4A76853C780C98A674525E08EE36A8C658B5BED86AB4818C3E43F5D`.
- Release AAB SHA-256: `D43AD6165589362FDB0842E57FADFFB9325AD12D8783A9C9D87056D2B0B75A6A`.
- APK signature and AAB JAR signature verified. R8 mapping size: 41,270,374 bytes.
