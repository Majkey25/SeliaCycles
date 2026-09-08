# Code 24 editor save recovery

Version `0.9.0-beta.16`. Checked September 8, 2026.

## Failure and fix

The period and information editors closed immediately after starting an asynchronous database write. A failed write left the original database intact but removed the unsaved draft from the screen.

Both editors now await the write result. A failure keeps the draft and shows an inline error; retry remains possible. Inputs and dismissal are blocked only while an operation is pending. A failed refresh has a separate retry action. A committed write is not reported as an uncommitted write merely because the subsequent refresh failed.

One ViewModel-owned result survives activity recreation. The restored editor reconnects to it. If the result is unavailable after process loss, the editor reports failure rather than replaying the write. This does not promise recovery after every process termination.

No database migration, permission, dependency, prediction rule or icon changed.

## Physical verification

Huawei `BQLDU19927002646`, Android 10. All destructive fixtures run only in the isolated `.qa` package; personal app data was not cleared or modified by test fixtures.

- Original application: all four initial failure-recovery tests reproduced the prematurely closed editor.
- First candidate: four recovery tests and the then-current 28-test suite passed. Review found a remaining pending-recreation case.
- Pending-recreation baseline: seven tests ran in 18.427 seconds; exactly two failed, covering successful and failed writes interrupted by activity recreation.
- Final candidate: **31/31 instrumentation tests passed in 55.262 seconds**. This includes all seven save-recovery tests plus calendar accessibility, legal links, discard/delete confirmation, settings persistence, prediction recalculation, import/export, profile isolation and UI, icon rendering, and synthetic store screenshots.
- Failure injection uses a real SQLite abort trigger. Pending writes use a transaction lock, not timing sleeps. Tests verify the stored record as well as the visible editor.
- Hardware Back during a pending write leaves the editor open. Success closes it; failure preserves the draft. Both outcomes remain correct through activity recreation. Combined write/read failures expose a retry without discarding edits.
- [Failure screen](screenshots/code24-save-error.png) was visually checked: readable inline error, retained draft and usable actions. Screenshot contains synthetic data only.
- Main app updated with `install -r` and cold-launched successfully; one launch sample was 1,778 ms, not a benchmark. Only the QA app and test packages were removed. Phone released before 11:30 CEST with Home foreground; no system setting changed.

## Local checks and limits

- `testDebugUnitTest`: 156 tests passed.
- `lintDebug`: zero errors, eight existing warnings.
- Debug, QA and instrumentation builds passed. Release signing and remote delivery are checked separately.
- Source review found no further concrete regression in this slice. No claim of exhaustive correctness, medical accuracy or complete accessibility conformance.
- Android 15+ runtime verification remains unavailable: the local API 36 emulator cannot boot because hardware acceleration is unavailable; the documented software fallback also exited before ADB boot. No host virtualization or driver setting was changed.
