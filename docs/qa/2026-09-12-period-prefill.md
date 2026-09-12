# Period duration prefill

Version `0.9.0-beta.17`, code 25.

New entries preselect the configured duration, or the duration learned from previous periods when automatic calculation is enabled. Dates after today are not stored as recorded bleeding. An active period instead adds an estimated continuation to the calendar and month overview. Confirming the end removes that continuation.

Existing records keep their exact selection. Users can remove preselected dates before saving. The existing prediction engine still supplies the duration; no second learning algorithm, new database field, permission, or dependency was added. Continuation does not create a second overdue period or an additional fertility window.

## Regression checks

- The initial three continuation tests failed against the previous implementation. One fixture initially omitted the required flow value; after correcting it, all three failed because the continuation was absent.
- Unit coverage checks five days across August/September, a six-day learned duration, a three-day manual override, unchanged saved forecasts, removal/end, expiration, and paused prediction states.
- Prefill coverage checks historical days, clipping at today, preservation of an existing selection, and rejection of invalid durations or future starting dates.
- Device tests exercise learned/manual prefill, shortening the selected range, draft recreation, today entry, and end confirmation in the isolated `.qa` application.

## Results on September 12

- 161 JVM tests passed. Lint reported zero errors and eight existing warnings. Debug, isolated QA, instrumentation, signed APK and AAB builds passed.
- Huawei `BQLDU19927002646`, Android 10: **34/34 instrumentation tests passed in 56.516 seconds**. The first device run had two screenshot-helper failures because a modal creates two Compose roots; switching the test capture to UIAutomation fixed them. No product assertion was removed.
- [Six-day prefill](screenshots/code25-period-prefill.png) and [estimated continuation](screenshots/code25-period-continuation.png) were visually checked. Both contain synthetic records only.
- The main app was updated using `install -r`, without clearing personal data. Code 25 and a successful cold launch were verified. The single launch sample was 1,802 ms, not a benchmark.
- Only isolated QA packages were removed. The phone was released before 10:50 CEST with Home foreground and no system/viewport changes.

No clinical-accuracy claim is made by these software tests. Publication is verified separately from the local build.
