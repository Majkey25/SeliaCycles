# Selia Cycles specification

## Objective

Build `com.majkeylab.seliacycles`, a private local-only Android period calendar with fast daily logging and honest personal estimates.

## Stack

- Kotlin 2.3.21, Jetpack Compose Material 3, AndroidX, native SQLite.
- Android 10+, compile/target SDK 36, JDK 17, Gradle 8.13.
- Six locales: English, Czech, Slovak, German, Polish, Spanish.

## Boundaries

- Keep cycle data in app-private storage.
- No Selia account, cloud service, analytics, ads, or network permission.
- Support local `.pc` import/export and optional selected-calendar mirroring. Exports are unencrypted; notes and raw health details never go to the calendar provider.
- Allow Android device-to-device transfer only.
- Keep the SQLite schema compatible with existing installed data.
- Estimates are not diagnosis, ovulation confirmation, or contraception.

## Success criteria

- Any day can record bleeding, flow, symptoms, mood, note, weight, basal temperature, sleep, and intimacy.
- Real bleeding starts immediately re-anchor the prediction.
- Local estimates preserve observed long intervals, handle isolated outliers, and show uncertainty without inventing unrecorded cycles.
- Today and the expanded month overview distinguish recorded days, current estimates, saved original estimates, and reconstructed history.
- Calendar marks recorded and future estimated days.
- Unit, lint, APK, AAB, physical-phone, and cold-start checks pass.
