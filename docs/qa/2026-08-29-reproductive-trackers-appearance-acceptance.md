# Selia Cycles 0.5.0-beta.1 acceptance

Date: 2026-08-29

## Build evidence

- Command: `gradlew.bat testDebugUnitTest lintDebug assembleDebug bundleRelease --console=plain -PseliaCyclesKeystoreProperties=<approved-local-properties>`.
- Result: `BUILD SUCCESSFUL`.
- Unit tests: 33 run, 0 failures, 0 errors, 0 skipped.
- Android Lint: 0 errors and 7 existing toolchain/update warnings.
- No dependency or permission was added. The manifest still has no `INTERNET` permission.
- Signed release AAB SHA-256: `E4F3DE6933292E506792DDA07C2676F034E345A697852808D887D3DB4A567DE2`.
- Debug APK SHA-256: `CF9A6386BCCBEC06E93584A676FCEE4BA1F6AA3F39EDCD500EA916CD6EE3B126`.
- AAB upload-certificate SHA-256: `A4:F0:7E:70:CC:0D:E4:22:44:11:FD:CD:EB:81:E3:E1:1D:5B:4B:A9:49:23:0C:F1:08:F3:76:3A:39:FD:4A:1E`, matching Google Play Console.

## Model and privacy evidence

- Spotting remains separate from period bleeding and is excluded from cycle-start prediction input.
- Pain accepts the inclusive 0–10 range and rejects values outside it.
- Cervical mucus, ovulation tests, pregnancy tests, energy, stress, activity, and medication status keep explicit nullable types.
- Calendar-mirror regression tests include the new reproductive fields and still produce only bounded cycle labels.
- Raw tracker data, notes, intimacy, measurements, and test results are not mirrored to calendar providers.
- SQLite version 4 uses additive columns with safe defaults for existing code-4 data.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10. Every ADB command was pinned to this serial. No emulator was used.

- Installed code 5 in place with `adb install -r`; existing 28 recorded period starts and the saved August prediction baseline remained visible.
- The app cold-started successfully after the SQLite 3-to-4 migration; AndroidRuntime showed no Selia crash.
- Created a temporary non-bleeding August 31 record with spotting, egg-white cervical mucus, positive ovulation test, negative pregnancy test, pain 10, high energy, low stress, moderate activity, and medication taken.
- All values survived a force-stop and cold restart.
- The next-period estimate remained September 25–27 and the recorded start remained August 28, proving the spotting-only record did not re-anchor the cycle.
- Tap-to-clear behavior was exercised on an optional chip.
- Deleted the temporary August 31 record. Reopening that day showed a new empty log with no Delete record action.

## Appearance QA

- System, Light, and Dark mode previews and Selia, Rose, and Ocean palette previews rendered without clipping.
- Dark plus Ocean persisted through a cold restart.
- Live QA exposed insufficient white-text contrast when the hero reused light dark-theme Material colors.
- The root cause was fixed with palette-specific saturated hero gradients and a test requiring at least 4.5:1 white-text contrast at every gradient endpoint.
- The corrected Dark Ocean hero was visually verified on the Huawei.
- Final device settings were restored to System mode and the Selia palette.

## Screenshots

- [Appearance previews](screenshots/phone-appearance-previews.png)
- [Reproductive trackers](screenshots/phone-reproductive-trackers.png)

## Release state

- Version code: 5.
- Version name: `0.5.0-beta.1`.
- Store listing and Closed Alpha release notes describe the new local trackers, daily fertility estimate, and appearance previews.
- Commit `74b368f` was pushed to `main`; GitHub Android CI run `33253905286` passed.
- Google Play accepted the signed code-5 AAB as `5 (0.5.0-beta.1)`, API 29+, target SDK 36.
- Release name: `Selia Cycles 0.5.0-beta.1 — Trackers`.
- Closed Alpha retains the existing 177 countries/regions and tester configuration; code 4 remains available while code 5 is reviewed.
- Device coverage is unchanged: 0 phones, tablets, TVs, cars, Chromebooks, or Android XR devices were lost.
- The same two non-blocking warnings remain: no deobfuscation file because minification is disabled, and no native symbols for a transitive library.
- The default English full description was updated. Google Play currently has no Czech store-listing locale configured; the prepared Czech copy remains in `docs/play-store/STORE_LISTING.md`.
- Google Play Publishing Overview confirmed that exactly 2 changes were submitted: Closed Alpha code 5 and the English full-description update.
- Submission status: `Probíhá kontrola změn` (changes under review).
- Controlled publishing is off, so Google will automatically publish approved changes to Closed Alpha.
