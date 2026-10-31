# Play quality check | 31 October 2026

Scope: Selia Cycles, `com.majkeylab.seliacycles`. The complete checkout remains in its original directory. The interrupted project copy was not resumed.

## Console observations

Checked on 30 October 2026 in the app's own Play Console:

* Policy status: no issues found.
* Android vitals: crash, ANR, memory and rendering values unavailable. Missing data does not mean zero failures.
* Automatic protection: active, one available service enabled.
* Store protection: signing, Play Protect, SDK scanning, review spam and discovery protections active. Device integrity checks were enabled and Console confirmed the change was saved.
* Play Integrity API is not integrated. Its verdicts require app and server integration. No server, network permission or new telemetry was added to this local app.
* Billing protections do not apply to this free app without purchases or subscriptions.

Local browser control was disabled in Work settings on 31 October. Later Console state and release submission could not be checked through that browser.

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

* 214 JVM tests passed. Debug lint completed with no errors and 11 version/dependency warnings.
* Huawei YAL-L21, Android 10 / API 29: 96 full QA tests passed in 165.541 seconds. The focused 28-test run and final two keyboard tests also passed. Screenshots confirmed the field and Save action remained above the open keyboard. Settings restoration, calendar band colours, profiles, widgets, imports and save failures were exercised.
* QA used an isolated `.qa` package and synthetic records. Personal app data was not accessed or overwritten.
* One QA cold-start sample was 1,735 ms. Calendar PSS was 113,323 KiB, with 135 KiB swap PSS. These are debugging-build samples, not production RSS or P90 compliance measurements.
* Version 36 / `0.9.0-beta.28`: signed Play AAB is 5,617,282 bytes, with 3,471,508 bytes of DEX and embedded R8 mapping. R8 renamed 4,464 of 4,535 mapped classes, or 98.43%. APK signature verification and 16 KB ZIP alignment checks passed. Both 64-bit native libraries have 16 KB ELF LOAD alignment.
* AAB SHA-256: `66c4ce1ed6279cf3135e51db0a9216300eaca63d409ae0f61ba8d6d795f3da90`.
* APK SHA-256: `3059531a299a24ca11996c2c695b6f7a111c6b0a78e305363a6c7d44d6bb8ef5`.
* Opus 5.5 provided a read-only review. Its confirmed settings-scroll and shared-month-state findings were fixed and checked locally. The lead separately corrected and tested the overdue forecast boundary case. Model advice was not treated as runtime proof.

The minified release was built and statically inspected. Physical UI tests used the QA variant. No emulator, production Play deployment or 28-day vitals measurement was performed in this run.
