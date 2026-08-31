# Selia Cycles 0.9.0-beta.2 acceptance

Date: 2026-08-31

## Build evidence

- Command: `gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease bundleRelease --console=plain -PseliaCyclesKeystoreProperties=<approved-local-properties>`.
- Result: `BUILD SUCCESSFUL`; 119 tasks, 117 executed and 2 up-to-date.
- Unit tests: 78 run, 0 failures, 0 errors, 0 skipped.
- Android Lint: 0 errors and 8 toolchain, dependency-update, or resource-folder warnings.
- Release AAB: `jar verified`.
- Upload certificate SHA-256: `A4:F0:7E:70:CC:0D:E4:22:44:11:FD:CD:EB:81:E3:E1:1D:5B:4B:A9:49:23:0C:F1:08:F3:76:3A:39:FD:4A:1E`.
- Signed AAB SHA-256: `A6EC75D924F4105E47733742687B3C17176396B0F2B9E85A82B78CFF8A97171B`.

## Behavior evidence

- Saved estimates cover the complete recorded history. Future estimates remain dynamic.
- Calendar period and fertility tracks remain independent during overlaps; ovulation remains a color overlay without a day icon.
- Recorded menstruation stays strong red across preset themes while ovulation and the fertile window remain separate; the same roles are editable in the custom palette.
- Adjacent-month dates fill every calendar week with reduced emphasis. Opening one switches the pager to that month before showing the day overview.
- The calendar legend is fully collapsed by default. Optional user-entry markers are blue and their fourth custom-palette color is stored locally.
- Recorded menstruation can be edited or removed directly from its day. Removing one connected period preserves notes and unrelated daily values.
- Simple mode keeps period estimates but hides fertility, ovulation, related legend entries, and fertility-sign inputs without deleting data.
- Phase guidance explicitly names the menstrual, follicular, estimated fertile/ovulation, or luteal phase and separates physical signs from emotions and energy with semantic icons.
- History shows recent cycle length, recorded period duration, estimated fertile dates, estimated ovulation, average saved-prediction error, and the number of starts inside the original saved range.
- Saved estimates match a recorded start up to 14 days away even when prediction and reality fall in adjacent months.
- Pregnancy and menopause use the neutral daily-log action instead of offering to start or end a period.
- Profiles accept age 8 through 100. Early-history guidance explains that the first cycles may be irregular.
- Day overview, editor, and self-care sheets open full height and keep their own scrolling separate from sheet gestures.
- Cycle analysis uses one line graph with aligned exact values instead of overlapping bars and points.
- Partner view is disabled until a writable calendar is selected. Google Calendar or Outlook controls view-only sharing permissions.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10. No emulator was used.

- Installed code 9 in place with `adb install -r`; existing cycle data remained present before the icon and Czech-copy code 10 rebuild.
- The device copy used the standard debug certificate (`768843C2E67E38838C8BD7751443A4CFA5B21DBFE6DB077F93677100DBCCCDA6`). The production release certificate intentionally differs, so its incompatible update was rejected without uninstalling or losing data. The matching debug code 10 update then installed successfully.
- Package state reports version code 10 and `0.9.0-beta.2`; the retained local data still renders cycle day 19, the next period, fertile window, ovulation estimate, and follicular phase.
- The live Czech follicular guidance now reads `Můžeš mít větší chuť být mezi lidmi...`; no `společenskější` wording remains.
- Huawei launcher search renders the supplied double-ring, three-arrow mark in black with the centered red drop on white; the adaptive safe crop keeps every arrow visible.
- Cold launch returned a live PID and no matching Selia `FATAL EXCEPTION` entry.
- Czech Home showed the explicit `Konec menstruace` action and the current cycle overview.
- Czech History showed `Průměrná odchylka: 5 dní` and `V uloženém rozmezí: 2 z 6` for the retained local data.
- August 2026 showed muted July 27–31 and September 1–6 dates. Opening July 31 showed that day, and closing the sheet left the pager on July 2026.
- The collapsed calendar showed only `Vysvětlit barvy`; expansion restored all five detailed legend items.
- Database version 7 preserved the existing cycle data and added the editable `Barva vlastního záznamu` default `#1565C0`.
- Database version 8 preserved the existing cycle data and added Simple mode disabled by default.
- The August 28 day opens full height with visible `Odstranit menstruaci`, `Upravit záznam`, and a confirmation that preserves other values. The destructive action was cancelled during live QA.
- Repeated fast content swipes kept the full-height day sheet open; close/edit actions no longer depend on a drag handle.
- Simple mode removed fertile and ovulation rows from Home and the expanded calendar legend, then full mode was restored and both estimates returned.
- Self-care exposes eight distinct routines, including hydration, quiet rest, and foot massage.
- Partner view is enabled on the selected writable calendar. The provider contains 229 Selia-owned events, and a sampled event uses provider-default access (`accessLevel=0`, free availability); Google/Outlook sharing permissions remain the read-only enforcement boundary.
- Appearance uses device, sun, and moon icons for System, Light, and Dark. The Custom palette uses a pencil icon and states that all four calendar colors are editable.
- The display override used only for Play screenshots was reset to the physical `1080x2340` size.
- A physical-Huawei 800dp layout override rendered content at `[200,108][1400,2400]`, exactly 600dp wide and centered; it was then reset.
- App language was restored to Czech and the Ocean palette was restored after screenshots.

## Security and performance review

- The merged release manifest has no Internet or network-state permission. Exported AndroidX components are protected by system permissions.
- My Calendar input is bounded by file, entry, extracted-size, row, note, detail, and record limits before merge.
- Calendar refresh now queries only events whose custom package and URI belong to Selia; it no longer scans every device calendar event.
- Repository secret-name scan found only signing-property names and documentation, with no key or keystore file in the repository.
- Microsoft Defender custom scans completed for the release APK and AAB without a scan error.
- One invalid `gfxinfo` attempt was discarded because another app took foreground. A later foreground-verified debug run kept the full-height day sheet open through eight fast swipes: 357 rendered frames, 11 janky frames (3.08%), p95 11 ms, p99 19 ms, 0 missed-vsync, and 1 frame-deadline miss.
- The same single-run debug snapshot used 142,103 KB total PSS with one Activity and no WebView. The 354 high-input-latency counter is treated as an ADB-injection artifact, not an app timing claim.

## Google Play assets

- Icon: 512x512 RGB, SHA-256 `DFA301830F0537736540551451D35999B4BE1C695FAC6ECDE5629F004DE24544`.
- Feature graphic: 1024x500 RGB, SHA-256 `FD6227DBD0D2ACD932F6AFCCC0A713ACD017A24F5B1C8E29F132D457252C1EB5`.
- Settings screenshot remains 1080x1920 RGB.
- Home: 1080x1920 RGB, SHA-256 `365E99344495456279D8FA4BF66695C624D72C99FC68B6B4D6BE5EFAF57C62B1`.
- Calendar: 1080x2160 RGB, SHA-256 `432F3624AA5DD069DF6DBD50F3C29F10AAA01A13DC07996ED9C57388BB590A22`.
- History: 1080x1920 RGB, SHA-256 `CE2333C65B22C2059FBFF29B30F6AFDE0D0211AFDD42E5A6AD9882552F539635`.
- Phase guidance: 1080x1920 RGB, SHA-256 `BC59B6C72097890DDCA07D18C7C98683F98A9E983B5F5953A30F68B376268263`.

## External state

- The complete redesign, corrected launcher/copy, and aligned Play feature graphic are committed and pushed on `main` through `f8087bd184b459743823d534175fd77bd7786362`. Android CI passed for the code 10 commit `154da6b9dbaf09d0d2fbea8fa2eb6d86fa838a36`; the later commit changes only Play artwork and this evidence file.
- The code 9 bundle was removed from the unsubmitted draft. The prepared Alpha release contains only code 10 (`0.9.0-beta.2`).
- Google Play accepted and submitted three changes for review: the code 10 closed-test release at 100%, the corrected 512x512 icon, and the aligned 1024x500 feature graphic. Fast checks completed and the authoritative status is `Probíhá kontrola změn`.
- Play reports only advisory missing-mapping and native-symbol warnings. R8 minification is disabled, so no mapping file exists; the bundled AndroidX native library provides no symbol archive to upload.
- Managed publishing is disabled, so the Alpha release will become available to selected testers after Google approves it. Play states that review is usually completed within seven days but can take longer.
- No separate physical tablet was connected; large-screen acceptance uses the physical Huawei at a verified 800dp override.
- Performance evidence is a single debug-device run; a release/profileable repeated trace remains optional before production rollout.
