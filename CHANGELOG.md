# Changelog

## 0.9.0-beta.3 - 2026-08-31

### Added

- Local My Calendar-compatible `.pc` export with full-fidelity Selia data for lossless re-import.
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
- Local My Calendar `.pc` import with preview and non-destructive merge.
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
- Manual backup/restore, My Calendar import, and Health Connect import.

## 0.1.0-beta.1 - 2026-08-28

### Added

- Offline daily period, flow, symptom, mood, and note tracking.
- Calendar estimates, history, Czech and English languages, themes, and reminders.
- Encrypted manual backup, Android Auto Backup, and atomic restore.
- Read-only Health Connect menstruation import with explicit consent.
- Local data deletion, privacy policy, Google Play declarations, and CI.
