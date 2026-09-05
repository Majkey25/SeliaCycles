# Selia Cycles

[![Android CI](https://github.com/Majkey25/SeliaCycles/actions/workflows/android.yml/badge.svg)](https://github.com/Majkey25/SeliaCycles/actions/workflows/android.yml)
[![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/10)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

<p align="center">
  <img src="docs/play-store/assets/icon-512.png" width="180" alt="Selia Cycles calendar and drop icon">
</p>

Selia Cycles is a private period calendar for Android. It records bleeding, spotting, flow, symptoms, mood, cervical mucus, ovulation and pregnancy tests, pain, energy, stress, activity, medication, weight, basal temperature, sleep, intimacy, and notes without an account, ads, analytics, or network permission.

## Features

- Calendar-first Material 3 UI with one-tap Period started / Period ended actions that record only confirmed days.
- Direct period-day editing can add, remove, shorten, extend, split, or clear exact bleeding days while preserving other daily information.
- Compact linked Today dashboard for the next period, fertile window, ovulation, phase guidance, History, and relief care.
- Swipeable month calendar with connected tracks, muted adjacent-month dates, and adjacent-day navigation into the correct full month.
- Calendar color key stays fully hidden below one explanation control until requested.
- Optional Calendar filters show the existing blue marker only on dates matching up to three locally used trackers.
- Clear customizable color roles: prominent red menstruation and a short blue underline only for optional user information, with ovulation and fertility kept visually separate.
- Next period, fertile window, and ovulation stay together in one visible cycle timeline.
- Optional phase guidance and local self-care timers with cautious medical wording.
- Phase guidance covers physical signs, emotions, and energy for menstrual, follicular, estimated ovulation/fertile, and luteal phases.
- Optional details add relationships, social energy, personal patterns, food, movement, and rest without treating phase stereotypes as facts.
- Cycle-length analysis plus recent period-duration, fertile-window, and estimated-ovulation timelines.
- Clean line-only cycle-length graph with exact values per recorded cycle.
- Saved-prediction accuracy summarizes average start-date error and results inside the original range.
- Personal symptom patterns appear only after repeated observations across completed cycles.
- Personal estimates based on recorded intervals, with isolated outliers excluded from typical length and retained in the uncertainty range. Long cycles are never silently divided into invented cycles.
- Recorded or estimated windows for this month and next month.
- Real bleeding starts immediately re-anchor future estimates.
- Saved monthly estimates stay visible beside recorded reality.
- Saved estimates extend across the complete recorded history; only future estimates keep changing.
- Estimated ovulation, fertile window, cycle phase, and personal mood trends with clear evidence limits.
- Daily fertility status plus recorded cervical-mucus and ovulation-test signals; no false ovulation confirmation.
- Complete reproductive and wellbeing trackers kept behind one optional More details control.
- Connected period and fertility spans in the month calendar.
- Optional mirror to an existing Google Calendar, Outlook, Exchange, or local Android calendar.
- English, Czech, Slovak, German, Polish, and Spanish.
- Combined cycle and profile settings for predictions, tracking goal, life situation, body context, and luteal phase.
- Clear device, sun, and moon theme controls plus six palettes, a pencil-marked custom option, a color picker, and optional exact hex values.
- Local `.pc` import and export with no upload; Selia exports preserve the complete local record for lossless re-import.
- Optional local period reminders.
- Android device-to-device transfer during new-device setup without ordinary cloud backup of reproductive data.
- One-action deletion of all local app data.
- Separate period and daily-information editors prevent optional tracking changes from altering menstruation dates.
- Expanded month overview with recorded-day counts, separate timeline rows for reality and estimates, all overlapping date ranges, and observed mood, energy, pain, and sleep summaries.
- Independent automatic/manual cycle and period lengths with a live settings preview.

Existing local records remain in the SQLite database across normal app updates. Selia Cycles has no Selia cloud or partner account. A selected `.pc` backup is read and merged locally. An exported `.pc` file is created only after the user chooses a destination and is not encrypted, so it should be stored securely. If calendar mirroring is enabled, short cycle labels are copied through Android to the selected provider; notes and raw health details are never mirrored.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease --console=plain
```

The default release bundle is unsigned. Publication uses an external upload keystore that is never committed. See [release signing and publication](docs/RELEASE.md).

## Device tests

`qa` uses `com.majkeylab.seliacycles.qa` and its own database. Instrumentation refuses to seed data outside that package. Reserve the shared physical phone before running:

```powershell
.\gradlew.bat assembleQa assembleQaAndroidTest --console=plain
adb -s BQLDU19927002646 install -r app/build/outputs/apk/qa/app-qa.apk
adb -s BQLDU19927002646 install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
adb -s BQLDU19927002646 shell am instrument -w -r -e class com.majkeylab.seliacycles.CycleAcceptanceTest com.majkeylab.seliacycles.qa.test/androidx.test.runner.AndroidJUnitRunner
```

The device suite covers independent automatic settings, period start/edit/removal and future recalculation, draft recreation, monthly navigation, and `.pc` re-import over existing snapshots without widening partner access.

## Privacy and medical scope

See the [privacy policy](PRIVACY.md) and [Google Play declarations](docs/play-store/DATA_SAFETY.md).
The public policy is published at <https://majkey25.github.io/SeliaCycles/>.

Predictions are calendar estimates. They do not diagnose a condition, confirm ovulation, or provide contraception.

## Support

Use [GitHub Issues](https://github.com/Majkey25/SeliaCycles/issues) for bugs. For privacy questions, email [majkeylab@gmail.com](mailto:majkeylab@gmail.com).

## License

Copyright © 2026 Majkey25. Licensed under the [Apache License 2.0](LICENSE).
