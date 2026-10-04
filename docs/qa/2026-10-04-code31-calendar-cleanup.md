# Code 31: legacy calendar cleanup

## Scope

Android provider app identifiers are not a reliable identity for copies that have passed through account synchronization. Code 31 adds an explicit, calendar-scoped preview of exact legacy Selia titles. Nothing is selected by default. Known ownership excludes other profiles; title-only copies warn that their original profile is unknown.

New copies carry a profile reference in the ordinary description. Reconciliation recognizes that reference after custom-field loss and preserves calendar-edited notes. No Google OAuth client, network service, or permission was added.

## Verification on October 4, 2026

- `testDebugUnitTest`: 189 tests, zero failures.
- `lintDebug`: zero errors, nine existing warnings.
- `assembleQa assembleQaAndroidTest`: passed.
- Physical Huawei YAL-L21, Android 10: full isolated QA suite passed, 56 tests.
- After making both preview dialogs scroll as one list, the cleanup UI, legacy provider and sync safety tests were rerun: nine tests passed.
- Only `com.majkeylab.seliacycles.qa` and its test package were installed. Both were removed after verification. The main app and real calendar provider were untouched.

Provider tests use an in-memory SQLite ContentProvider through Android ContentResolver. They execute the production query, batch, update, deletion, and persistent sync-state paths.

Covered cases:

- Legacy event without custom package or URI is deleted after selection.
- Unrelated title, another calendar, foreign app ownership, recurring and timed records survive.
- Previously enabled sync remains OFF after a simulated deletion failure and a new CalendarMirror instance. Reload creates zero new events.
- 227 selected legacy copies are deleted in batches without deleting the control event in another calendar.
- Two profiles remain isolated after both custom fields are stripped and descriptions gain HTML wrapping. Reload does not create duplicates.
- Package-only metadata loss still uses the profile URI.
- An event changed after preview is not deleted.
- Calendar-added description text survives reconciliation.
- The UI initially disables deletion, forwards only selected IDs, and supports selecting or clearing the listed copies.

Before implementation, legacy lookup and metadata-loss reconciliation tests failed. Independent review found description overwrite during marker migration; its regression failed before the preservation fix and passed afterward.

## Limits

This is not a live Google cloud round-trip test. The user's existing Google Calendar data was not modified or rechecked. Only events exposed by the device provider can be reviewed. The provider controls propagation of deletions to the account. A title alone is not proof of ownership, which is why legacy deletion requires explicit selection.
