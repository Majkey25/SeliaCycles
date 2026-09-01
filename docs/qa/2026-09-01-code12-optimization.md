# Selia Cycles 0.9.0-beta.4 optimization acceptance

Date: 2026-09-01

## Build

- Version: code 12, `0.9.0-beta.4`.
- Release uses R8 code optimization, resource shrinking, and `FULL` native debug metadata when dependencies provide it.
- `testDebugUnitTest`, `lintDebug`, `assembleRelease`, and `bundleRelease` completed successfully.
- Unit tests: 85 run, 0 failures, 0 errors, 0 skipped.
- Signed AAB: 5,333,962 bytes, SHA-256 `984C661CE6556CFC91D4F12506DE42A23781ED589F3D1C07D53FD15192D37FAF`.
- The code 11 AAB was 13,981,279 bytes. R8 and resource shrinking reduced the code 12 AAB by 61.8%.

## Play diagnostics

- The AAB contains `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` and `BUNDLE-METADATA/com.android.tools/r8.json`.
- The retained `mapping.txt` is 40,684,121 bytes.
- The only native dependency is `androidx.graphics:graphics-path:1.0.1`, pulled by Compose UI.
- Its four `libandroidx.graphics.path.so` files are already stripped upstream and contain no `.symtab` or debug sections. Gradle therefore reports `mergeReleaseNativeDebugMetadata NO-SOURCE`; meaningful native symbols cannot be generated locally from this dependency.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10.

- A minified release build signed with the existing QA key updated code 11 to code 12 without clearing app data; the original first-install timestamp remained unchanged.
- Cold launch returned a live PID and rendered the retained Czech Light/Ocean dashboard.
- Home preserved cycle day, next-period, fertility-window, ovulation, and phase information.
- Calendar opened September 2026 with adjacent-month days, predicted period, fertile-window, and ovulation accessibility labels.
- Settings scrolled normally and the Data and transfer page exposed local `.pc` import and export actions.
- Logcat contained no fatal exception, class-loading failure, or WorkManager instantiation error.
