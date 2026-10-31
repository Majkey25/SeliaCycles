# Play quality check | 31 October 2026

Scope: Selia Cycles, `com.majkeylab.seliacycles`. The complete checkout remains in its original directory. The interrupted project copy was not resumed.

## Console observations

Checked on 30 October and refreshed on 31 October 2026 in the app's own Play Console:

* Policy status: no issues found. App content declarations require no action.
* Android vitals: crash, ANR, memory and rendering values unavailable. Missing data does not mean zero failures.
* Automatic protection: 1/1 active.
* Store protection: 7/7 active, including device integrity checks enabled on 30 October.
* Play Integrity API is not integrated. Its verdicts require app and server integration. No server, network permission or new telemetry was added to this local app.
* Billing protections do not apply to this free app without purchases or subscriptions.

Browser control recovered after the owner restarted Codex. Version 36 was submitted and is now available to selected Alpha testers. The production access application was accepted on 31 October at 13:05 Europe/Budapest and awaits Google's decision. Submission is not production approval.

## Remaining Console warnings

Version 36 reports edge-to-edge compatibility warnings. Its deprecated window calls retrace to AndroidX Activity's `EdgeToEdgeApi26`, `EdgeToEdgeApi28`, `EdgeToEdgeApi29` and `EdgeToEdgeApi35`.

Version 37 uses the current [WindowCompat.enableEdgeToEdge](https://developer.android.com/reference/androidx/core/view/WindowCompat) API. Theme-aware system bar icons and navigation bar contrast remain enabled. The resolved Core 1.18.0 implementation still calls legacy window APIs for compatibility, so this migration alone does not prove Google's static warning will disappear. Android 15/16 device behavior remains unverified.

The native-symbol warning remains unresolved. The bundled `androidx.graphics:graphics-path:1.0.1` AAR supplies stripped libraries without `.symtab` or debug sections. Gradle already requests `ndk.debugSymbolLevel = "FULL"`; it cannot recover removed dependency symbols. Genuine symbols must match the shipped binaries. No placeholder archive was uploaded. See [Android's native debug symbol guidance](https://developer.android.com/build/include-native-symbols).

## Security inspection

A focused read-only review checked widget ownership and profile routing, explicit import confirmation, bounded archive/database parsing, and device-transfer backup restrictions. No confirmed high or medium severity defect was found in those paths. Existing import, profile, widget and calendar cleanup tests exercised their regression cases. This was not a penetration test or a real device-to-device backup transport test. No server, permissions or telemetry were added to improve a Console badge.

## Release optimization

Code and resource shrinking were already enabled with `proguard-android-optimize.txt`. No broad keep rules or disabled obfuscation were found. Enabled the optimized resource shrinking pipeline recommended for AGP 8.13.

The baseline Play-channel bundle built from version 35 contained 3,469,448 bytes of DEX. Its R8 mapping renamed 4,465 of 4,537 classes, or 98.41%. This class-name count is not Google's obfuscation coverage metric. The warning quoted for version 47 cannot be attributed to this app from the available evidence.

Google's [technical quality requirements](https://support.google.com/googleplay/android-developer/answer/17492799) introduce memory and code optimization enforcement from February 2027. The DEX threshold for ordinary apps applies above 10 MB. A local memory sample cannot establish the 28-day production P90 metric.

## Policy update

Reviewed the [29 October announcement](https://support.google.com/googleplay/android-developer/answer/18100766) against the merged manifest and existing functionality:

| Policy | Selia Cycles |
| --- | --- |
| Local network permission | Not requested. No local network feature. |
| Manage contacts permission | Not requested. No contacts feature. |
| AppFunctions | Not implemented. |
| Eligibility-based subscription discounts | No subscriptions. |
| External digital payments | Play builds exclude donation links and use isolated legal pages. Existing protection retained. |

## UI fixes

* Keyboard resizing enabled for the main, internal profile and widget configuration activities. Main content consumes keyboard padding.
* Recorded and predicted bands keep their colour across a month boundary in the app and widget. Adjacent date text remains distinct.
* Saved original forecasts are opt in through the calendar explanation. Current predictions remain visible, including overdue predictions across a month boundary. Saved records and snapshots are retained.
* Settings preserve their scroll position across tab changes and recreation.
* Month overview expansion belongs to one month, so swiping cannot expand the next month and then collapse it after settling.

Reference: [R8 configuration for AGP 8.13](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization).

## Verification

* 214 JVM tests passed. Debug lint completed with no errors and 11 warnings about available updates and existing resource/icon notices.
* Huawei YAL-L21, Android 10 / API 29: 96 full QA tests passed in 165.541 seconds. The focused 28-test run and final two keyboard tests also passed. Screenshots confirmed the field and Save action remained above the open keyboard. Settings restoration, calendar band colours, profiles, widgets, imports and save failures were exercised.
* QA used an isolated `.qa` package and synthetic records. Personal app data was not accessed or overwritten.
* One QA cold-start sample was 1,735 ms. Calendar PSS was 113,323 KiB, with 135 KiB swap PSS. These are debugging-build samples, not production RSS or P90 compliance measurements.
* Version 36 / `0.9.0-beta.28`: signed Play AAB is 5,617,282 bytes, with 3,471,508 bytes of DEX and embedded R8 mapping. R8 renamed 4,464 of 4,535 mapped classes, or 98.43%. APK signature verification and 16 KB ZIP alignment checks passed. Both 64-bit native libraries have 16 KB ELF LOAD alignment.
* AAB SHA-256: `66c4ce1ed6279cf3135e51db0a9216300eaca63d409ae0f61ba8d6d795f3da90`.
* APK SHA-256: `3059531a299a24ca11996c2c695b6f7a111c6b0a78e305363a6c7d44d6bb8ef5`.
* Opus 5.5 provided a read-only review. Its confirmed settings-scroll and shared-month-state findings were fixed and checked locally. The lead separately corrected and tested the overdue forecast boundary case. Model advice was not treated as runtime proof.

## Version 37 verification

* 214 JVM tests passed. Debug lint completed with 0 errors and 11 existing warnings. QA APK, QA instrumentation APK, signed release APK and Play AAB built successfully.
* Huawei YAL-L21, Android 10 / API 29: the full 96-test run completed in 152.622 seconds with 95 passing and one stopped by an already blocked QA reminder channel. Toggling that channel made it user-locked, preventing the test from lowering its importance. After resetting only the disposable `.qa` package, both reminder tests passed. The other 95 tests were not rerun after that reset.
* Keyboard screenshots were inspected. Appearance fields and the daily note Save action remained above the open keyboard. Only synthetic QA data was used; the personal app package was not changed. The phone was explicitly released to the next project after testing.
* Signed Play AAB: 5,612,128 bytes; DEX: 3,469,456 bytes; embedded R8 mapping: 45,976,532 bytes. Old Activity edge-to-edge implementation classes are absent from the release mapping. This is not proof that all deprecated framework calls disappeared.
* APK signature and 16 KB ZIP alignment checks passed. Upload certificate SHA-256 remains `a4f07e70cc0de4224411fdcdeb81e3e11d5b4ba949230cf108f3763a39fd4a1e`.
* AAB SHA-256: `a3b115fafe92647b67cc664661badcc995b43393824a7a8ed129b60304f1a591`.
* APK SHA-256: `a2173be3b5d29ac81c4058ee58c9860b269d64405b8df6411e1bc22326e18916`.
* Play accepted the version 37 upload and submission to closed Alpha. Publishing shows "Probíhá kontrola změn" with quick checks still running. It is not yet approved or available. Release validation showed one non-blocking warning for missing native debug symbols and no blocking errors. This validation screen does not replace the later dashboard compatibility analysis.

Minified releases were built and statically inspected. Physical UI tests used the QA variant. No emulator, production rollout or 28-day vitals measurement was performed in this run.
