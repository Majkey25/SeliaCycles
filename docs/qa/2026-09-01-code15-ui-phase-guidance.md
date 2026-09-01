# Selia Cycles 0.9.0-beta.7 UI and phase guidance acceptance

Date: 2026-09-01

## Automated verification

- Test-first checks covered predicted-period emphasis and phase-specific relief-care recommendations.
- Clean unit tests, lint, minified release APK, and signed AAB completed successfully.
- Unit tests: 88 run, 0 failures, 0 errors, 0 skipped.
- Version: code 15, `0.9.0-beta.7`.

## Calendar UI

- Recorded bleeding remained strong red.
- Predicted bleeding used the same red hue at 28 percent opacity.
- Fertile and ovulation layers used quieter 20 percent emphasis and no longer dominated the period.
- September showed predicted bleeding on 2026-09-12 through 2026-09-16, fertility on 2026-09-23 through 2026-09-29, and ovulation on 2026-09-28.
- August showed recorded bleeding on 2026-08-13 through 2026-08-17 with the saved estimate retained as a small contrasting marker.
- The expanded legend showed one full-width row for recorded bleeding, predicted bleeding, fertility, ovulation, and user entries. Scrolling reached the complete entry explanation without flicker.

## Phase and relief care

- The short phase summary kept physical signs, mood, and energy visible.
- Read more exposed Czech sections for emotions and relationships, food/movement/rest, personal patterns, and the medical disclaimer.
- A selected predicted menstrual day opened menstrual guidance and menstrual relief care, not today's luteal recommendations.
- Menstrual relief care started with warmth, gentle movement, massage, and hydration. Existing timers and safety text remained functional.
- Daily status no longer showed unrelated future ovulation and fertility dates.

## Settings UI

- System, Light, and Dark modes fit in one compact three-column row with clear icons and labels.
- Settings category rows used less vertical space while retaining at least 44 dp icons and large row touch targets.
- Trying to conceive used a heart icon instead of a stroller.
- Cycle and Profile remained one page and scrolled normally.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10.

- A minified code 15 build updated the existing installation without clearing local data.
- The original first-install timestamp remained 2026-08-28 14:15:59.
- Home, calendar, prior-month overlap, expanded legend, day details, Read more, relief care, appearance, and combined cycle/profile settings were exercised.
- Logcat contained no fatal exception after the tested flows.

## Medical wording sources

- ACOG: Premenstrual Syndrome and Dysmenorrhea guidance for exercise, sleep, relaxation, heat, and diet cautions.
- NHS: period-pain guidance for warmth, massage, and gentle exercise.
- Office on Women's Health: cycle-related emotion and energy changes and the value of personal symptom tracking.
- The app states that phase descriptions vary, do not predict personality, do not confirm ovulation, and do not replace medical care.

## Release artifacts

- Signed APK: 4,133,940 bytes, SHA-256 `2FBB2E6945DE45B264E63325EC1F59F5F203BCF5B9475521C6B3602B4EA40243`.
- Signed AAB: 5,347,658 bytes, SHA-256 `9BAFBFFC5594D7D8B28D3E17BA95535C93667E10B64B88B44644D3D99FDA17A0`.
- Signing certificate SHA-256: `a4f07e70cc0de4224411fdcdeb81e3e11d5b4ba949230cf108f3763a39fd4a1e`.
- The AAB contains ReTrace mapping and R8 metadata.
