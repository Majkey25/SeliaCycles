# Selia Cycles competitive parity and reliability design

Date: 2026-09-01

## Goal

Improve Selia with original, privacy-preserving versions of the strongest public competitor workflows while proving that new period data updates every future estimate coherently.

## Competitor findings

| Public product | Useful workflow | Selia decision |
| --- | --- | --- |
| Clue | Filter the calendar by up to three recorded experiences and show personal tracking patterns | Add local filters without icons inside day cells, plus symptom-by-phase insights |
| Samsung Health | Recalculate future predictions after new cycle data, with editable cycle and luteal settings | Add end-to-end regression scenarios and device verification |
| Period Calendar | Simple period start/end, broad trackers, reminders, local/email backup, anonymous use | Selia already covers the core; keep local backup and simple actions, do not add ads or accounts |
| Flo | Symptom insights, privacy mode, partner view, pregnancy and perimenopause modes | Selia is already account-free; keep calendar-based partner sharing and roadmap dedicated life-stage modes |
| Stardust | Phase education, personal symptom patterns, partner mode, wearable trends | Phase education exists; add local personal patterns and defer cloud partner/wearable infrastructure |

The implementation will not copy proprietary text, branding, artwork, layouts, or medical claims.

## Scope for this release

### Prediction acceptance

- A newly recorded actual period start reanchors all later period starts.
- Fertile windows, ovulation dates, Today, Calendar, month overview, reminders, and calendar mirroring use the same new sequence.
- Past and current saved estimates remain available for accuracy comparison but never freeze future predictions.
- Removing or editing a period recalculates the same downstream sequence.
- Manual cycle and period-length overrides remain authoritative.
- Future bleeding input remains rejected while other future notes remain safe.

### Calendar filters

- Add one Filter action to Calendar.
- Let the user select up to three trackers that already occur in local data.
- Cover individual symptoms plus spotting, mood, pain, energy, stress, intimacy, tests, and notes.
- Keep calendar cells icon-free. The existing blue entry dot appears only on matching dates while a filter is active.
- Show active-filter count and provide one Clear action.

### Personal patterns

- Add a History section that ranks recurring symptoms by observed cycle phase.
- Require at least three occurrences across at least two completed cycles before showing a pattern.
- Show symptom name, likely phase, sample count, and cycle count.
- State that association is personal history, not diagnosis or a guaranteed prediction.

## Performance

- Baseline one focused calendar-swipe flow with `gfxinfo` and `meminfo` on an available adb target.
- Cache calendar recorded, predicted, fertile, and ovulation sets by their real inputs.
- Create one localized date formatter and one `today` value per month page instead of per day cell.
- Re-measure the same flow. Keep only changes supported by frame, allocation, or clear repeated-work evidence.

## Security

### Assets

- Menstrual dates, symptoms, notes, body measurements, intimacy data, pregnancy/ovulation tests, and exported backups.

### Trust boundaries

- Imported `.pc` files.
- Android document provider used for export.
- Android Calendar provider.
- Android device-transfer backup.
- User-entered free text and measurements.

### Controls

- Preserve bounded archive, database, row, date, numeric, and text validation.
- Keep SQL writes through `ContentValues` and fixed selection arguments.
- Keep the app without network permission, ads, analytics, or account identifiers.
- Restrict Android 10 backup rules to device-to-device transfer instead of ordinary cloud backup.
- Keep raw notes and health details out of mirrored calendar events.
- Keep exported compatible `.pc` files explicitly marked as unencrypted.

## Deferred work

- Cloud accounts or a proprietary partner backend.
- Wear OS tiles or home-screen widgets.
- Dedicated pregnancy, medication schedule, contraception, PCOS, PMDD, and perimenopause products.
- Wearable temperature algorithms or fertility claims.
- Multiple profiles inside one database.

These are separate products or security boundaries and need their own design, consent, and acceptance work.

## Acceptance criteria

- Unit tests reproduce and pass period add, edit, remove, override, snapshot, and future-input scenarios.
- Calendar filter matching and three-filter limit pass unit tests.
- Personal symptom patterns pass positive, insufficient-data, and cross-cycle tests.
- Controlled UI data entry changes the following month's period, fertile window, and ovulation consistently.
- Full tests, lint, release build, import/export regression, security review, dependency report, and adb QA pass.
- Performance report records before/after evidence or reports the exact unavailable target blocker.
