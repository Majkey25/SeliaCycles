# Selia Cycles 0.9.0-beta.3 acceptance

Date: 2026-08-31

## Build evidence

- Final command: `gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease bundleRelease --console=plain -PseliaCyclesKeystoreProperties=<approved-local-properties>`.
- Result: `BUILD SUCCESSFUL`; 119 tasks, 117 executed and 2 up-to-date.
- Unit tests: 84 run, 0 failures, 0 errors, 0 skipped.
- Android Lint completed successfully.
- Version: code 11, `0.9.0-beta.3`, package `com.majkeylab.seliacycles`.
- Signed release APK: 49,077,886 bytes, SHA-256 `18FD1CD7D9B1341F77E85F4D299017C26C2DB12D9A2D42FF3AA4EB07913D7AE5`.
- Signed release AAB: 13,981,279 bytes, SHA-256 `3FFD028ACA31A04FE6C4ED3413F18EDEFE746F1E779D26832F05B18AD0EC32C6`.
- Upload certificate SHA-256: `A4:F0:7E:70:CC:0D:E4:22:44:11:FD:CD:EB:81:E3:E1:1D:5B:4B:A9:49:23:0C:F1:08:F3:76:3A:39:FD:4A:1E`.

## Fixed behavior

- Manual cycle and period lengths override learned history immediately. Live QA changed the learned 30-day cycle to 31 days and the next period moved from September 12 to September 13 without restart; automatic mode was then restored.
- Starting a period records only the confirmed start day. Ending it fills only the completed inclusive span.
- Database version 10 removes legacy future bleeding flags while preserving any other future-day details. The retained device database has 190 rows and zero future bleeding rows.
- Future dates cannot be marked as real menstruation from the day overview. They still expose optional record editing.
- The old Simple mode is no longer shown and cannot suppress fertility estimates. Its legacy database field is normalized off for update compatibility.
- Today is a compact linked dashboard. Period, fertile-window, ovulation, phase, self-care, History, and day-overview entries all open their corresponding destination.
- Dashboard-to-calendar navigation switches to the target month and marks the exact target with a circular outline.
- Calendar swipes work in both directions. Adjacent-month dates remain muted; opening one switches to its full month before showing the day sheet.
- Day-sheet scrolling reached phase feelings, safety advice, relief care, and recorded values without closing or flickering.
- Relief care exposes twelve guided routines with semantic icons and timers.
- New installs default to Light theme and Ocean palette. Existing installations retain their saved appearance.

## `.pc` interoperability

- Selia exports the generation-7 Java-object/ZIP envelope with metadata `-1,1,0`, a valid `cloud.db`, encrypted My Calendar `.user`, `.period`, `.note`, `.pill`, and `.pill_record` sidecars, plus a versioned full-fidelity Selia payload.
- Live Selia export contained 190 dated records. Reopening it in Selia produced a 190-day import preview from January 22, 2022 through August 28, 2026. Cancelling the preview left the database unchanged.
- My Calendar `1.77.365.GP` was installed from Google Play on the clean test target. It recognized the Selia `.pc`, displayed its overwrite warning, restored it, and opened the populated cycle dashboard with period and fertility information.
- My Calendar receives the common period, note, measurement, sleep, and intimacy subset it understands. Reimporting into Selia restores every Selia field, settings, and saved forecast snapshot.
- The temporary My Calendar installation and every QA `.pc` export were removed after verification.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10. No emulator was used.

- Code 11 installed in place over code 10; the original first-install timestamp and local data remained present.
- SQLite migrated from version 9 to 10. Final settings are Light, Ocean, fallback cycle length 28, period length 5, automatic overrides, and legacy Simple mode off.
- Home shows cycle day 19, the next period in 12 days, luteal phase, fertility and ovulation values, relief care, and cycle analysis.
- Calendar target navigation, horizontal month swipe, adjacent-month opening, and full-height day scrolling passed with fresh UIAutomator bounds.
- A physical-device 800dp override rendered app content exactly 600dp wide and centered. Home and the full calendar remained readable. Display size and density were reset to physical `1080x2340` and `480`.
- Cold launch returned a live PID and the expected `MainActivity`.

## Icon and Play assets

- Built-in ImageGen produced the transparent master. It uses a white rounded calendar, two rounded binding tabs, one red drop, and no arrows or text.
- Android adaptive foreground: 1254x1254 RGBA, transparent corner pixels, centered through a 94dp inset layer. Huawei App info renders the full calendar and drop without clipping.
- ImageGen master: `docs/play-store/assets/icon-master-imagegen.png`, 453,008 bytes.
- Play icon: 512x512 opaque PNG, SHA-256 `65D60DDE4A53EAC89E2B799DAE4268E1EE24DF2B8A525469EC292EECBE3A9870`.
- Feature graphic: 1024x500 opaque PNG, SHA-256 `4E9CC067F1A1CE19E35E1D2807F2E24595F2E1DEADF13C258FDC1BFE6AB29A77`.

## Stored screenshots

- `docs/qa/screenshots/code11-phone-home.png`
- `docs/qa/screenshots/code11-phone-calendar-target.png`
- `docs/qa/screenshots/code11-tablet-home.png`
- `docs/qa/screenshots/code11-icon-app-info.png`
