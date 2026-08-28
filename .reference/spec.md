# Selia Cycles specification

## Objective

Build `com.majkeylab.seliacycles`, a private Android period tracker for fast daily logging, clear period predictions, local history, reminders, and honest data migration.

## Stack and commands

- Kotlin 2.3.21, Jetpack Compose Material 3, AndroidX, native SQLite, Health Connect 1.1.0.
- Android 10+, compile/target SDK 36, JDK 17, Gradle 8.13.
- Build: `gradlew.bat :app:assembleDebug --console=plain`
- Test: `gradlew.bat :app:testDebugUnitTest --console=plain`
- Lint: `gradlew.bat :app:lintDebug --console=plain`

## Boundaries

- Always: app-private storage, bounded validated imports, encrypted manual backups, explicit Health Connect consent, no diagnostic or contraceptive claims.
- Never: network access, embedded secrets, invented Samsung/My Calendar APIs, silent destructive restore.
- Samsung Health/My Calendar data import works only when a source exposes standard Health Connect menstrual records. Proprietary account backups are inaccessible to other apps.

## Success criteria

- User can log bleeding, flow, symptoms, mood, and a short note for any day.
- Calendar, history, and next-period estimate update from stored records.
- Settings cover theme, language, prediction defaults, reminders, backup/restore, Health Connect, privacy, and menstrual-health information.
- Google/device cloud restore uses Android Auto Backup. Manual backup uses a password-encrypted file through Android's document picker.
- Unit test, lint, debug build, install, and four emulator flows pass or exact blockers are recorded.
