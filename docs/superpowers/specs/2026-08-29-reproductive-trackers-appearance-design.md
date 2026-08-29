# Reproductive Trackers and Appearance Design

## Goal

Extend the existing local daily log with the reproductive signals used by leading cycle trackers, add a clear daily fertility status, and provide theme previews with color palettes without making the primary logging flow harder.

## Evidence and scope

Public feature pages for Samsung Health, Flo, Clue, Ovia, and Period Calendar consistently expose period flow, spotting, symptoms, mood, cervical mucus, sexual activity, ovulation tests, pregnancy tests, basal temperature, sleep, weight, medication, and notes. Selia already covers most of these. The missing reproductive core is spotting, cervical mucus, test results, pain intensity, energy, stress, activity, and medication status.

ACOG describes calendar dates, cervical mucus, basal body temperature, and urine LH tests as distinct fertility-awareness inputs. Basal temperature rises after ovulation; a positive LH test suggests ovulation may follow but does not prove it. Selia therefore stores these as recorded signals and keeps the calendar result explicitly labeled as an estimate.

## Data model

`DayLog` gains:

- `spotting: Boolean`
- `cervicalMucus: CervicalMucus?` with dry, sticky, creamy, watery, egg-white, and unusual values
- `ovulationTest: TestResult?` and `pregnancyTest: TestResult?` with negative, positive, and invalid values
- `painLevel: Int?` constrained to 0 through 10
- `energy: Level?`, `stress: Level?`, and `activity: ActivityLevel?`
- `medication: MedicationStatus?` with taken and missed values

Spotting is not bleeding and never becomes a cycle start. Existing bleeding remains the only predictor input. Empty-record detection includes every new field. SQLite schema version 4 adds nullable columns and a non-null boolean spotting column. Existing rows keep their meaning.

`AppSettings` gains `palette: AppPalette`, defaulting to Selia. Palettes are Selia, Rose, and Ocean. Theme mode remains System, Light, or Dark.

## UI

The first part of the daily sheet stays unchanged: flow, mood, symptoms, note. Reproductive signals and wellness trackers appear under the existing More details control, grouped with icons. Choice chips remain mutually exclusive and can be tapped again to clear an optional value. Pain uses the existing integer stepper pattern.

Today shows one compact fertility status: estimated fertile window, estimated ovulation day, outside estimated fertile window, or unavailable. It may show recorded LH or cervical-mucus evidence for today. It never reports confirmed ovulation or a pregnancy probability.

Appearance settings replace text-only theme chips with preview tiles. A mode preview shows light/dark surface contrast. Palette previews show representative primary, period, and fertility colors. Selecting a preview applies and persists immediately. Language remains on the same page.

## Privacy and safety

All fields remain in the app-private SQLite database and Android device-transfer backup. No network permission, account, analytics, or new dependency is added. Calendar mirroring and Partner view continue to omit raw daily health data, tests, notes, intimacy, and medication.

## Verification

Unit tests cover defaults, validation, empty-record behavior, spotting exclusion from prediction, fertility status boundaries, and palette resolution. Physical Huawei QA covers a new record, an edge value, clearing optional values, persistence across process restart, appearance previews, light/dark rendering, and an unchanged period workflow. Database upgrade is verified by updating the installed code-4 app without clearing data.

## Sources

- https://www.samsung.com/cz/support/apps-services/sledovani-menstruacniho-cyklu-s-aplikaci-samsung-health/
- https://help.flo.health/hc/en-us/sections/360002040591-Calendar-Symptoms-Logging
- https://support.helloclue.com/hc/en-us/sections/4417167065620-Tracking
- https://ovuline.helpshift.com/hc/en/3-ovia/faq/923-how-should-i-track-my-fertility/?p=android&s=general-1479147734
- https://play.google.com/store/apps/details?id=com.popularapp.periodcalendar
- https://www.acog.org/womens-health/faqs/fertility-awareness-based-methods-of-family-planning
