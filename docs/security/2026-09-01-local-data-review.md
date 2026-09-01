# Selia Cycles local data security review

Date: 2026-09-01

## Assets

- Period dates and cycle estimates.
- Symptoms, moods, pain, energy, stress, intimacy, tests, measurements, medication status, and notes.
- Compatible `.pc` exports and Android Calendar mirror labels.

## Trust boundaries

### Imported `.pc` file

- The app caps the container at 20 MiB, database entries at 10 MiB, entry count at 32, expanded data at 20 MiB, source rows at 20,000, and Selia transfer data at 5 MiB.
- It verifies the object envelope, ZIP names, duplicate entries, SQLite header, required schema, dates, numeric ranges, note lengths, imported-detail lengths, period lengths, and total log count before preview.
- Import runs on `Dispatchers.IO`; preview does not mutate storage. Merge runs in one SQLite transaction.

### Local SQLite database

- Android app-private storage protects the database from ordinary apps.
- SQL writes use fixed table/column names, `ContentValues`, and fixed selection arguments. User text is not concatenated into SQL.
- Model constructors and SQLite checks bound dates, values, lengths, and booleans.

### Android backup and device transfer

- Android 12+ rules exclude all cloud-backup domains and include only database/shared preferences for device transfer.
- Legacy Android rules now require the `deviceToDeviceTransfer` transport flag. Ordinary Auto Backup must not copy reproductive data to cloud storage.
- `allowBackup` remains enabled only because device-to-device migration is an explicit feature.

### Exported `.pc` file

- Export occurs only after the user chooses an Android document destination.
- The format stays unencrypted for compatibility. UI and privacy text must keep warning users to protect the exported file.
- Export uses bounded transfer encoding and closes the selected stream.

### Android Calendar provider

- Calendar access is runtime-permission gated and user-selected.
- Only short recorded/estimated/fertility/ovulation labels and dates are mirrored.
- Notes, symptoms, measurements, intimacy, tests, and imported details never enter calendar events.

### Reminders

- Period reminders require explicit enablement and Android notification permission where applicable.
- Notifications and their channel use private lockscreen visibility so the period estimate is not intentionally exposed on a locked device.

## Network and component exposure

- The manifest has no INTERNET permission and explicitly disables cleartext traffic.
- The launcher activity is exported only for the launcher intent filter.
- The locale metadata service is disabled and not exported.
- No ads, analytics, account identifier, authentication token, or remote API is present.

## Supply chain

- Release runtime dependency resolution completed without unresolved modules, dynamic versions, or snapshots.
- Dependencies are declared with fixed versions or a fixed Compose BOM.
- The Gradle 8.13 wrapper distribution is pinned to the official SHA-256 `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`.
- Android lint remains the available native security/static gate. The project has no configured vulnerability database scanner, so the dependency report does not prove that every transitive library is free of advisories.

## Residual risks

- A user-selected compatible `.pc` export is readable by anyone who obtains the file.
- A selected calendar provider may upload short cycle labels under its own account policy.
- A rooted device, compromised OS, or unlocked device backup can bypass ordinary app-private storage controls.
