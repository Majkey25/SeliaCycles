# Start-period range, code 26

Version `0.9.0-beta.18`. This replaces the incomplete beta.17 behavior: editor prefill and a lighter estimate did not make the Start action save the expected range.

## Behavior

- Start uses the existing prediction engine to fill the configured or learned duration. The first day is confirmed; added days carry `automaticBleeding` and use the period color.
- The range persists across activity/database reopening. Future automatically filled days can be opened and edited. End confirms the completed range and clears the remainder without deleting optional information.
- Duration learning and recorded-day statistics exclude automatic days. Confirmed imports win over automatic flags when records merge. Saved forecasts are not rewritten.
- Database 11 adds a default-false column. A recent, still-open single-day period from database 10 receives its expected continuation; confirmed records and optional details are preserved. Completed periods are not expanded.
- The Selia backup payload supports versions 1 and 2. Version 2 preserves the new flag. Backups without automatic days still use version 1. External-compatible period rows include confirmed bleeding, not unconfirmed future days.

## Verification

- The new default-range test failed on the prior implementation: one day instead of five. It passes after the Start implementation changed.
- 165 unit tests passed. Lint: zero errors and eight existing warnings. Debug, QA, instrumentation and signed APK/AAB builds passed.
- Huawei `BQLDU19927002646`, Android 10: 39/39 tests passed in 62.828 seconds, including legacy-schema migration, migration of an open single-day period, database reopen, full `.pc` round trip, actual Start-button input, learned/manual length, future-day editing and the existing regression suite.
- After synchronizing screenshot capture with the UI, the five focused entry tests passed again in 11.087 seconds. Initial captures caught transitions rather than the settled calendar; product assertions already passed.
- Reviewed synthetic screenshots show [six marked days after Start](screenshots/code26-start-six-days.png) and [the shortened five-day range](screenshots/code26-start-five-days.png).
- The main app was updated without clearing data. Code 26 cold launch succeeded in 1,830 ms, a single sample rather than a benchmark. QA packages were removed and the phone was released early before 16:45 CEST, with Home foreground and system settings unchanged.

No new dependency, permission, network service, signing key, or clinical accuracy claim was added. This is targeted software verification, not a claim that the whole app has no defects. Remote publication is checked separately.
