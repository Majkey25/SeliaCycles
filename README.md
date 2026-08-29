# Selia Cycles

[![Android CI](https://github.com/Majkey25/SeliaCycles/actions/workflows/android.yml/badge.svg)](https://github.com/Majkey25/SeliaCycles/actions/workflows/android.yml)
[![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/10)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

<p align="center">
  <img src="docs/play-store/assets/icon-512.png" width="180" alt="Selia Cycles calendar and drop icon">
</p>

Selia Cycles is a private period calendar for Android. It records bleeding, flow, symptoms, mood, notes, weight, basal temperature, sleep, and intimacy without an account, ads, analytics, or network permission.

## Features

- Calendar-first Material 3 UI with one clear daily action.
- Robust personal estimates that normalize missed tracking cycles and reject isolated outliers.
- Recorded or estimated windows for this month and next month.
- Real bleeding starts immediately re-anchor future estimates.
- Saved monthly estimates stay visible beside recorded reality.
- Estimated ovulation, fertile window, cycle phase, and personal mood trends with clear evidence limits.
- Connected period and fertility spans in the month calendar.
- Optional mirror to an existing Google Calendar, Outlook, Exchange, or local Android calendar.
- English, Czech, Slovak, German, Polish, and Spanish.
- System, light, and dark themes.
- Optional local period reminders.
- Android device-to-device transfer during new-device setup.
- One-action deletion of all local app data.

Existing local records remain in the SQLite database across normal app updates. Selia Cycles has no manual import/export, Selia cloud, or partner account. If calendar mirroring is enabled, short cycle labels are copied through Android to the selected provider; notes and raw health details are never mirrored.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease --console=plain
```

The default release bundle is unsigned. Publication uses an external upload keystore that is never committed. See [release signing and publication](docs/RELEASE.md).

## Privacy and medical scope

See the [privacy policy](PRIVACY.md) and [Google Play declarations](docs/play-store/DATA_SAFETY.md).

Predictions are calendar estimates. They do not diagnose a condition, confirm ovulation, or provide contraception.

## Support

Use [GitHub Issues](https://github.com/Majkey25/SeliaCycles/issues) for bugs. For privacy questions, email [majkeylab@gmail.com](mailto:majkeylab@gmail.com).

## License

Copyright © 2026 Majkey25. Licensed under the [Apache License 2.0](LICENSE).
