# Selia Cycles UI and phase guidance design

Date: 2026-09-01

## Product fit

- Audience: people tracking a first or established menstrual cycle who need clear, calm guidance.
- Primary actions: understand today, start or end a period, and open a calendar day.
- Tone: professional, supportive, non-deterministic, and medically cautious.
- Trust rule: phase estimates may suggest possibilities but must never predict personality, confirm ovulation, or replace medical care.

## Options considered

1. Change only the predicted-period color. Smallest diff, but leaves the calendar legend, cramped phase card, oversized theme controls, and unrelated daily status details inconsistent.
2. Focused visual-system pass. Keep navigation and workflows, fix the shared color roles and the few components causing most confusion. Chosen.
3. Rewrite every screen. Highest regression risk and unnecessary because Home, History, navigation, and responsive width already work.

## Visual system

- Recorded period: strong configurable red.
- Predicted period: the same period color at low opacity, so it remains red but clearly estimated.
- Fertile window: a quieter low-opacity tertiary color; it must not dominate the period.
- Ovulation: a low-opacity primary highlight.
- User entry: small configurable blue marker.
- Calendar legend: one full-width row per meaning, hidden by default below the calendar.
- Theme mode: three equal compact icon buttons in one row for System, Light, and Dark.
- Settings root: retain current icons and categories; remove avoidable vertical bulk.

## Phase guidance

- Keep the current short summary visible.
- Rebuild its layout so the heading icon does not narrow every paragraph.
- Add an optional Read more section with:
  - emotions and relationships;
  - social energy and communication needs;
  - food, movement, hydration, sleep, and comfort ideas;
  - a reminder that the user's repeated observations matter more than generic phase descriptions.
- Use conditional language such as may and some people. Never say a user will feel or behave a certain way.
- Link to Relief care from every phase.

## Relief care

- Show the current phase and an evidence-based phase summary before timed activities.
- Recommend a short phase-appropriate subset of existing safe activities.
- Menstrual care may include heat, gentle movement, rest, breathing, hydration, and massage.
- Luteal care may include regular movement, sleep routine, relaxation, hydration, and gentle massage.
- Follicular and fertile care emphasizes normal balanced meals, hydration, and activity adjusted to actual energy; no unsupported cycle-sync diet or sunlight claims.
- Retain safety and medical disclaimers.

## Other UI corrections

- Daily status contains only information relevant to the selected day. Future fertile-window dates remain in Calendar and Today, not inside an unrelated day status card.
- Replace the stroller icon for Trying to conceive with a neutral heart icon.
- Preserve the existing 600 dp maximum content width for tablets.

## Evidence

- ACOG supports heat, exercise, sleep, relaxation, and selected diet changes for menstrual pain or PMS.
- NHS supports warmth, massage, and gentle exercise for period pain.
- Office on Women's Health notes that emotion and energy can vary across the cycle and recommends tracking personal patterns.
- The UI must state that responses vary and that estimates do not diagnose PMS or confirm ovulation.

## Verification

- Regression tests for predicted-period opacity and phase-specific care ordering.
- Full unit test, lint, minified release APK, and signed AAB build.
- Physical Huawei checks for Home, Calendar, expanded legend, day details, phase Read more, relief care, compact theme controls, Czech copy, scrolling, and fatal logcat errors.
