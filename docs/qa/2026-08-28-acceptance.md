# Selia Cycles closed beta acceptance

Date: August 28, 2026

## Environment

- Device: dedicated `SeliaCycles_QA` AVD on `emulator-5554`.
- OS: Android 16, API 36, Google Play system image.
- Physical device: Huawei YAL-L21 on `BQLDU19927002646`.
- Physical OS: Android 10, API 29, 1080 x 2340.
- Package: `com.majkeylab.seliacycles`.

## Live scenarios

### Daily log and persistence

1. Opened **Log today**.
2. Enabled period bleeding.
3. Selected medium flow, okay mood, cramps, and a note.
4. Saved the log.
5. Confirmed that Today showed September 25, 2026 and **In 28 days**.
6. Force-stopped and relaunched the app.
7. Confirmed that the record and estimate remained.

Result: passed.

### Encrypted backup failure and recovery

1. Created `SeliaCycles-2026-08-28.seliabackup` through Android DocumentsUI with a test password.
2. Confirmed that the file was 336 bytes and did not crash the app.
3. Restored with a wrong password.
4. Confirmed the explicit rejection message and unchanged current data.
5. Restored with the correct password.
6. Confirmed **Backup restored** and the original Today record.

Result: passed.

### Health Connect denial

1. Opened **Import data** after reading the prominent disclosure.
2. Left first-run Health Connect onboarding with **Go back**.
3. Confirmed **Permission denied. No data was imported.**
4. Confirmed no crash and no local data loss.

Result: passed.

### Nearby workflows

- Navigated Today, Calendar, History, and Settings.
- Verified the recorded calendar marker and history entry.
- Switched Czech and English at runtime.
- Switched dark and light themes.
- Granted notification permission, enabled reminders, and confirmed a WorkManager system job for the package.
- Disabled the reminder after the check.
- Checked the emulator crash buffer after the flows. It was empty.

Result: passed.

## Physical phone verification

- Installed the debug APK only on `BQLDU19927002646` and launched `MainActivity`.
- Logged period bleeding, medium flow, okay mood, cramps, headache, and a note.
- Confirmed the September 25 prediction, cold-relaunch persistence, calendar marker, and history entry.
- Switched to Czech and dark theme; verified the phone screenshots visually.
- Enabled reminders and confirmed the package's periodic WorkManager job in `dumpsys jobscheduler`.
- Confirmed the explicit Health Connect unavailable message on Android 10 without a crash.
- Exported a 350-byte encrypted backup through DocumentsUI.
- Confirmed wrong-password rejection without data loss, then restored successfully with the correct password.
- Checked the AndroidRuntime error buffer after the flows. It was empty.

Result: passed.

### Redesign and My Calendar migration

- Verified the gradient Today hero, seven-day direct editing, primary add/edit action, persistent Save bar, and icon-led settings categories at 1080 x 2340.
- Verified direct flow selection plus mood, symptoms, note, weight, basal temperature, sleep, and protected-intimacy persistence.
- Switched English, Spanish, Czech, system theme, and dark theme at runtime; all six locale resource sets contain the same 200 entries.
- Previewed the supplied `My Calendar-2026-08-28-24.pc` without mutation: 184 supported days from January 22, 2022 through April 19, 2026 and 74 preserved source details.
- Confirmed merge, 28 imported period starts, cold-relaunch persistence, and preservation of the existing August 2026 local record.
- Selected an unrelated ZIP and confirmed explicit rejection with all 28 starts unchanged.
- Exported and restored the version 2 encrypted Selia backup with the imported history intact.
- Removed the temporary measurement record and pushed duplicate `.pc`; moved the generated test backup to Huawei Recently deleted.

Result: passed.

### Partner-sync security

- Verified nine Firestore Emulator scenarios: owner writes, payload validation, immutable identity, anonymous/unrelated denial, reader-only access, transactional one-time invitation, expired invitation denial, owner cleanup, and revocation.
- Verified default partner payload keys are only `day`, `bleeding`, and `flow`.
- Verified the no-config phone build reports Google sync unavailable while all offline features remain usable.
- Live Google sign-in and two-account sync remain gated on verified Firebase/OAuth configuration.

Result: local security gates passed; live account flow pending configuration.

## Store assets

- [Today](../play-store/assets/screenshots/en/01-home.png)
- [Calendar](../play-store/assets/screenshots/en/02-calendar.png)
- [History](../play-store/assets/screenshots/en/03-history.png)
- [Settings](../play-store/assets/screenshots/en/04-settings.png)
- [Physical phone home](screenshots/phone-android10-home.png)
- [Physical phone calendar](screenshots/phone-android10-calendar.png)
- [Physical phone settings](screenshots/phone-android10-settings.png)
- [Physical phone Czech dark settings](screenshots/phone-android10-settings-cs-dark.png)
- [Physical phone backup and import settings](screenshots/phone-android10-settings-low.png)
- [Redesigned Today](screenshots/phone-redesign-home.png)
- [Icon-led daily log](screenshots/phone-icons-log.png)
- [Icon-led settings](screenshots/phone-icons-settings.png)
- [Czech dark appearance and languages](screenshots/phone-redesign-settings-cs-dark.png)
- [My Calendar import preview](screenshots/phone-redesign-import-preview.png)
- [512 px icon](../play-store/assets/icon-512.png)
- [Feature graphic](../play-store/assets/feature-graphic-1024x500.png)
