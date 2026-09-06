# Code 22 editor safeguards

Version: `0.9.0-beta.14`. Physical device: Huawei `BQLDU19927002646`, Android 10.

Deleting daily information requires confirmation. The dialog names the date and explains that hidden and imported details are included, while period dates and flow remain unchanged. Closing either editor with unsaved changes offers Keep editing or Discard changes. Unchanged and reverted drafts close without a warning.

The bottom-sheet guard reads current draft state when Android requests dismissal. A Boolean captured during composition missed edits followed immediately by Back, allowing the sheet to hide before the warning. The live predicate prevents that race. Save also reads and validates current field values.

## Verification

- The original three safeguard tests failed before implementation. The final four tests passed together on the phone in 13.030 seconds.
- All 20 instrumentation tests passed together in 35.243 seconds. Coverage includes immediate Back, activity recreation, canceled and confirmed deletion, invalid measurements, period editing and future recalculation, frozen forecast history, profiles, import/export, icon rendering, and store-screen capture.
- `testDebugUnitTest`: 156 tests, no failures, errors, or skips.
- `lintDebug`: zero errors and eight existing warnings. Debug, QA, instrumentation, signed release APK, and signed AAB builds passed.
- APK signature matches the existing upload key. `jarsigner -verify` verified the AAB, with self-signed certificate, timestamp, and ZIP manifest-order warnings.
- A Kotlin metadata parsing failure was resolved by `:app:clean` followed by the complete build. No shared cache or other project's daemon was removed.
- The [deletion dialog](screenshots/code22-delete-confirmation.png) was inspected on the phone using synthetic records.
- Main app updated with `install -r`, without clearing its data. Installed code 22 was verified; cold launch succeeded in 1,804 ms. This single sample is not a performance comparison.
- Isolated QA packages and disposable device captures were removed. Phone released at 11:21 local time, with Home foreground and system settings unchanged.

No database schema, permission, dependency, or prediction algorithm changed in this release. These checks do not establish clinical prediction accuracy. Existing asynchronous save-error recovery is outside this editor-dismissal fix.

Release target is the existing Google Play Alpha closed test. Local checks do not imply Play approval or tester availability.
