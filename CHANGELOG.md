# Changelog

## 0.9.0-beta.10 - 2026-09-04

### Fixed

- Historical days no longer offer the quick period end action, which could unintentionally shorten an earlier period. Exact past dates remain editable through the period-day editor.
- The daily information action now uses a clear plus icon when adding information and a pencil when editing it.

## 0.9.0-beta.9 - 2026-09-01

### Added

- A period-day editor can add, remove, shorten, extend, split, or clear exact bleeding days without deleting other daily information.

### Fixed

- Adding, editing, or deleting symptoms, mood, pain, notes, and measurements no longer starts or removes a period.

### Changed

- The day overview now separates `Edit period` from `Add/Edit information`.
- Calendar information uses a short blue underline instead of a dot. Menstruation alone never creates the underline, and the old period-overlap underline is gone.

## 0.9.0-beta.8 - 2026-09-01

### Added

- Calendar filters for up to three locally used symptoms or experiences without adding icons to day cells.
- Personal symptom-by-phase patterns after at least three observations across two completed cycles.

### Fixed

- A newly recorded period no longer revives a stale saved estimate as the next live period, fertility window, ovulation date, or mirrored event.
- Calendar mirroring no longer rewrites provider events whose dates, labels, visibility, and status are already current.

### Changed

- Calendar collections and localized day descriptions avoid repeated work during recomposition.
- Legacy Android backups allow reproductive data only for device-to-device transfer, and the Gradle wrapper now verifies its distribution checksum.

## 0.9.0-beta.7 - 2026-09-01

### Added

- Optional phase details now cover emotions, relationships, social energy, food, movement, rest, and personal patterns with medical disclaimers.
- Relief care now recommends a smaller set of activities for the selected phase.

### Changed

- Predicted periods use a quiet red tint while recorded bleeding stays prominent.
- Fertility and ovulation highlights are less dominant, and the calendar legend uses full-width rows.
- System, light, and dark modes now fit in one compact row.
- Phase cards use the full content width, settings categories are tighter, and the trying-to-conceive goal no longer uses a stroller icon.

## 0.9.0-beta.6 - 2026-09-01

### Fixed

- Current calendar, Today, fertility, and calendar sync now use one coherent live prediction instead of mixing it with a saved monthly baseline.
- The month overview shows the full predicted period range and labels a differing saved start window separately.
- Fertility windows no longer overlap a period because of a stale current-month snapshot.

### Changed

- Combined Cycle and tracking with Profile into one Cycle and profile settings page.

## 0.9.0-beta.5 - 2026-09-01

### Changed

- Renamed the local backup section to Import and export and removed third-party brand wording from user-facing copy.
- Reverified a real 154,036-byte `.pc` backup through the device import preview without merging it.

## 0.9.0-beta.4 - 2026-09-01

### Changed

- Release builds now use R8 code/resource optimization and include deobfuscation metadata.
- Release bundles request all native debug metadata available from dependencies for Play crash diagnostics.

## 0.9.0-beta.3 - 2026-08-31

### Added

- Local compatible `.pc` export with full-fidelity Selia data for lossless re-import.
- Compact linked Today dashboard and expanded guided relief care.

### Fixed

- Manual cycle and period lengths now immediately control future estimates.
- Starting a period records only the confirmed day; ending it fills the completed span.
- Future-period cards now open the matching date and phase cards open focused guidance.
- New installs use the Light theme and Ocean palette.
- ImageGen-designed launcher artwork now uses a white rounded calendar and red drop on a black circular mark with transparent adaptive-icon margins.

## 0.9.0-beta.2 - 2026-08-31

### Fixed

- Launcher and Google Play icons now preserve the supplied three-arrow, double-ring geometry with an inverted white/black palette, unchanged red drop, and separate safe crops for Play and adaptive launcher masks.
- Czech phase guidance now uses natural phrasing for wanting more contact with people instead of an awkward comparative adjective.

## 0.9.0-beta.1 - 2026-08-30

### Added

- Semantic icons for navigation, settings, profile choices, trackers, calendar states, and history.
- Cautious phase guidance, optional self-care timers, and a six-cycle history chart.
- Full-history saved estimates, adolescent profile ages, and per-cycle period/fertility detail.
- Aggregate saved-prediction accuracy with average error and in-range count.
- Gentle warmth and massage routines with explicit safety wording.
- Ready-made HSV color picker, custom-palette presets, and optional exact hex entry.
- Home-screen visibility controls for phase guidance, self-care, and expanded cycle details.
- Device, sun, and moon icons for theme mode; the editable custom palette now has a clear pencil action and explanation.
- Optional Simple mode that keeps period prediction while hiding fertility and ovulation details without deleting data.
- A confirmed Remove period action that clears one connected period while preserving notes and other daily values.
- Hydration, quiet rest, and foot-massage care routines with distinct icons and safety wording.

### Changed

- Home now prioritizes cycle day, next period, fertile window, ovulation, and the Start/End action.
- Calendar supports horizontal month swipes, shows muted adjacent-month dates, and opens an adjacent day after switching to its full month.
- Calendar keeps period and fertility tracks independent during overlaps and uses consistent customizable color roles.
- Tablet content is centered at a readable width instead of stretching edge to edge.
- Calendar legend is fully hidden below one explanation control until requested.
- Recorded menstruation stays prominent red and optional user-entry markers stay blue across preset themes; both remain editable in the custom palette.
- Default color roles now use strong red for menstruation, gold for estimated ovulation, and teal for the fertile window; custom palettes keep the same editable roles.
- Phase guidance names the current phase and separates physical signs from emotions and energy.
- Launcher icon uses a white adaptive background with a black cycle mark and red drop.
- Google Play icon, feature graphic, and screenshots now match the current app.
- Pregnancy and menopause use the neutral daily-log action instead of offering to start a period.
- Cycle analysis now uses one clean line graph instead of overlapping bars and points.
- Day, editor, and self-care sheets open full height with explicit close controls and scroll independently from sheet gestures.
- Period and ovulation icons now use simpler water-drop and sun symbols.

## 0.8.0-beta.1 - 2026-08-30

### Fixed

- Day overview shows prediction-versus-reality only on relevant period days.
- Future recorded periods no longer appear as completed history.

### Changed

- Calendar legend now appears before the month grid and the add button no longer covers calendar content.
- History separates completed starts from future recorded entries.

## 0.7.0-beta.1 - 2026-08-30

### Fixed

- A future recorded period remains the next cycle boundary instead of pushing fertility one cycle ahead.
- Next-month prediction baselines remain visible after recorded reality is added.

### Changed

- Today now leads with one Start/End action and an always-visible next period, fertile window, and ovulation timeline.
- Calendar shows a compact month overview above the grid and keeps connected fertile and ovulation days visible.
- Day overview removes repeated guidance and keeps optional details secondary.

## 0.6.0-beta.1 - 2026-08-29

### Added

- One-tap period start and end; start fills the learned or configured usual length.
- Local `.pc` import with preview and non-destructive merge.
- Profile goal, life situation, body context, and configurable luteal phase.
- Forest, Sunset, Lilac, and custom color palettes.

### Changed

- A day opens with status, saved estimate comparison, fertility, recorded values, and legend before optional detailed editing.
- Pregnancy, menopause, hormonal contraception, and perimenopause settings suppress estimates that are not appropriate for that situation.
- Bottom-sheet gestures no longer compete with the day-content scroller.

## 0.4.0-beta.1 - 2026-08-29

### Added

- Saved or reconstructed monthly estimates shown beside recorded reality.
- Estimated ovulation, fertile window, cycle phase, and evidence-gated personal mood trend.
- Connected calendar spans with visible recorded/estimated overlap.
- Optional native calendar mirror for installed Google Calendar, Outlook, Exchange, or local providers.
- Optional plain-language Partner view without a partner account.

### Changed

- Calendar mirroring exposes short cycle labels only; private notes, symptoms, measurements, intimacy, and raw moods remain local.

## 0.3.0-beta.1 - 2026-08-28

### Added

- Robust local prediction that handles missed tracking cycles and isolated outliers.
- Recorded or estimated windows for the current and next month.
- Direct Android device-to-device transfer without normal cloud backup.

### Removed

- Google/Firebase account sync and partner calendars.
- Manual backup/restore, `.pc` import, and Health Connect import.

## 0.1.0-beta.1 - 2026-08-28

### Added

- Offline daily period, flow, symptom, mood, and note tracking.
- Calendar estimates, history, Czech and English languages, themes, and reminders.
- Encrypted manual backup, Android Auto Backup, and atomic restore.
- Read-only Health Connect menstruation import with explicit consent.
- Local data deletion, privacy policy, Google Play declarations, and CI.
