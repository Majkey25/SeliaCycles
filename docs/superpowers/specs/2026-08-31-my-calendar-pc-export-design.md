# My Calendar compatible `.pc` export design

Status: approved on 2026-08-31

## Outcome

Selia Cycles exports one local `.pc` file. Selia Cycles can import the file without losing Selia-only data. My Calendar can import the fields that its format supports.

The export uses Android's document picker. The app does not add a storage permission, an account, or network access.

## Verified source format

The existing importer and the retained inspection record define the source format:

- A Java `ObjectOutputStream` writes the integers `-1`, `1`, and `0`.
- A ZIP stream follows the integers.
- The ZIP contains `1.timezone`, `1.generation`, `1.info`, `cloud.db`, `1.user`, `1.period`, `1.pill`, `1.note`, and `1.pill_record`.
- `1.generation` contains `24`.
- `cloud.db` is SQLite 3 and contains `User`, `Period`, `Note`, `Pill`, `note_field_meta`, and `android_metadata`.
- A nonzero signed `Period.period` value is a recorded period length. A zero value is a placeholder.

The exporter reproduces this structure. It does not invent a second file format with a `.pc` suffix.

## Chosen design

The export is a hybrid My Calendar database with a Selia extension.

The standard My Calendar tables contain only verified common fields. My Calendar ignores the extra `SeliaBackup` table. Selia Cycles reads `SeliaBackup` when it imports its own export.

This design avoids two bad outcomes:

- A My Calendar-only export would discard Selia-only trackers during a round trip.
- A Selia-only export would not restore in My Calendar.

## Common data mapping

The exporter converts each connected run of recorded bleeding days into one `Period` row. The row uses a `yyyyMMdd` start date and a positive length from 1 through 14.

The exporter writes a `Note` row for each day that has a supported common value:

- `note`
- `weight`
- `temperature`
- `sleep`
- `intimate`
- `condom`

The exporter writes a My Calendar mood, symptom, cervical-fluid, or test code only after a clean reference backup proves the exact code. The exporter never guesses proprietary codes.

My Calendar cannot display Selia-only fields such as stress, energy, activity, medication, spotting, or the saved prediction range. The Selia extension preserves those values.

## Selia extension

`cloud.db` contains one extra table:

```sql
CREATE TABLE SeliaBackup (
    version INTEGER PRIMARY KEY NOT NULL,
    payload BLOB NOT NULL
)
```

The versioned binary payload contains:

- every `DayLog` field;
- every `AppSettings` and `UserProfile` field;
- every `ForecastSnapshot` field.

The codec uses `DataOutputStream` and `DataInputStream`. It writes enum names, not enum ordinals. The reader enforces the existing date, string, measurement, record-count, and file-size limits before it returns data.

If `SeliaBackup` is absent, the importer keeps the current My Calendar path. If `SeliaBackup` is valid, the preview reports the complete Selia record range and merges the full Selia data. A damaged Selia extension fails the import. The importer does not silently fall back to partial data.

## User flow

Settings > Data and transfer contains two adjacent actions:

1. **Export `.pc` backup** opens `CreateDocument` with a suggested name such as `Selia-Cycles-2026-08-31.pc`.
2. **Import `.pc` backup** keeps the current preview and merge confirmation.

The export description states that `.pc` is unencrypted because My Calendar must read it. It tells the user to keep the file private. The export action is disabled when the app has no records.

The view model writes the file on `Dispatchers.IO`. A failed write returns a localized error and does not modify app data.

## Compatibility proof

Implementation starts with a sanitized reference fixture from a clean My Calendar installation. The fixture contains no personal data. Its schema, ZIP entry names, and sidecar structure become regression evidence.

The feature is complete only when all checks pass:

- Selia exports a valid bounded container that `MyCalendarContainerReader` reads.
- Selia imports its own export and preserves every log, setting, and forecast snapshot.
- Selia still imports the existing My Calendar fixture, including signed period lengths and zero placeholders.
- A clean My Calendar installation imports an exported period and note with the expected dates.
- Empty, oversized, truncated, duplicate-entry, and invalid-extension inputs fail without changing stored data.

The clean My Calendar test installation contains only test data. Removing it after verification cannot remove user data.

## Security and release

The exporter uses a private temporary database and deletes it after the document write. It never exports calendar-provider IDs or permissions.

The privacy policy and Play listing state that `.pc` export is local and unencrypted. Exporting data does not change the Data safety answers because Selia does not collect or transmit the file.

The implementation increments the Android build to code 11. The code 10 Alpha submission remains under review until code 11 is ready.
