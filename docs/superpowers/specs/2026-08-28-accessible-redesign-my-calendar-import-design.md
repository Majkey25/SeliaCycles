# Selia Cycles accessible redesign and My Calendar import

Date: August 28, 2026

## Product

- Audience: people who want fast private cycle tracking without setup friction.
- Primary action: add or edit a daily entry.
- Trust requirements: local-only data, explicit import preview, atomic merge, no silent replacement.

## Visual direction

- Use a restrained rose-to-violet cycle gradient as the dominant hero surface.
- Keep the rest of each screen quiet, high-contrast, and spacious.
- Use one large `+` action instead of competing buttons.
- Avoid decorative card grids, glass effects, and low-contrast gradient fog.
- Preserve Material 3 accessibility, 48 dp targets, system dark mode, and reduced motion.

## Navigation and screens

### Today

- Show cycle day, next estimated period, and confidence notice in one gradient hero.
- Show a seven-day strip with clear recorded and estimated states.
- Keep one large add/edit action visible.
- Show today's saved summary beneath the primary action when present.

### Daily log

- Open the chosen date in a full-height sheet.
- Combine bleeding and flow into one direct choice: none, light, medium, or heavy.
- Keep mood, symptoms, and note in a single vertical flow.
- Keep Save visible at the bottom and allow explicit record deletion only for existing entries.

### Calendar and history

- Preserve direct day editing from the calendar.
- Use the same visual states as the seven-day strip.
- Keep history focused on cycle metrics and period starts.

### Settings

- Replace the long control wall with category rows for cycle, appearance and language, reminders, data and import, and privacy.
- Open one category at a time in a focused detail sheet.
- Offer System, English, Czech, Slovak, German, Polish, and Spanish language choices.
- Keep optional profile and measurement controls under a separate Tracking category.

### Optional daily details

- Keep the default log limited to bleeding, mood, symptoms, and note.
- Put weight, basal temperature, sleep, intimacy, cervical fluid, and test results under an explicit More details control.
- Support metric and imperial display while storing canonical metric values.
- Do not treat weight or other health measurements as a deterministic predictor input.

### Adaptive prediction

- Learn only from the user's recorded period starts and durations.
- Weight recent complete cycles more heavily and reject invalid intervals.
- Show a wider estimate window when recent cycle lengths vary.
- Never present ovulation, fertility, or pregnancy estimates as medical facts or contraception.

## My Calendar import

- Add a dedicated Android file-picker action for `.pc` backups.
- Read the Java object stream and embedded ZIP without executing any content.
- Enforce bounded file, entry, record, note, and date limits.
- Accept the supplied My Calendar generation metadata and verified `cloud.db` schema.
- Parse period dates, duration, notes, weight, temperature, mood, symptoms, sleep, intimacy, cervical observations, and test results when their source encoding is supported.
- Preserve unsupported source fields in an internal import-details value and report them in the preview instead of silently discarding them.
- Show a preview with record count and date range before import.
- Merge by date. Preserve existing mood, symptoms, notes, and explicit flow values.
- Reject unsupported, malformed, duplicated, or oversized input before any database mutation.

## Error handling

- Distinguish unsupported format, damaged backup, no period records, and successful import.
- Keep current data unchanged on every failure.
- Report the number of imported or merged days after success.

## Verification

- Unit tests cover the supplied `.pc` structure, truncation, malformed entries, bounds, and merge precedence.
- Existing backup and prediction tests stay green.
- Lint and debug/release builds pass.
- Physical phone verification covers quick logging, calendar editing, language/theme switching, import preview, confirmed merge, persistence, and malformed-file rejection.
