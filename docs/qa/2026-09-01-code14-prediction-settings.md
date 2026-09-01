# Selia Cycles 0.9.0-beta.6 prediction and settings acceptance

Date: 2026-09-01

## Root cause and fix

- The live calendar combined a saved September forecast with the current 30-day prediction sequence. That produced period starts on 2026-09-26 and 2026-10-12 and an overlapping fertility window.
- Calendar, Today, fertility, and calendar export now share one current prediction sequence. Saved forecasts remain available for history and accuracy comparison, but no longer replace the current sequence.
- The month overview shows the full current predicted period and presents a different saved start window separately.
- Cycle and Profile settings now share one `Cycle and profile` page. The separate Profile tile was removed.

## Automated verification

- Regression test first reproduced the stale-current-snapshot failure: expected 2026-09-12, received 2026-09-26.
- The regression test now verifies coherent 2026-09-12 and 2026-10-12 period starts and non-overlapping fertility windows.
- Clean unit tests, lint, minified release APK, and signed AAB completed successfully.
- Unit tests: 86 run, 0 failures, 0 errors, 0 skipped.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10.

- Minified code 14 updated the existing installation without clearing local data.
- Home showed the next period on 2026-09-12 and upcoming fertility on 2026-09-23 through 2026-09-29 with ovulation on 2026-09-28.
- September calendar showed the predicted period on 2026-09-12 through 2026-09-16 and no period/fertility overlap.
- Month overview showed the current predicted period on 2026-09-12 through 2026-09-16, saved start window on 2026-09-25 through 2026-09-27, fertility on 2026-09-23 through 2026-09-29, and ovulation on 2026-09-28.
- Settings root showed one `Cyklus a profil` tile and no separate `Profil` tile.
- The combined page scrolled from cycle prediction controls to tracking goal, life situation, age, height, weight, and luteal phase controls.
- Logcat contained no fatal exception.

## Release artifacts

- Version: code 14, `0.9.0-beta.6`.
- Signed APK SHA-256: `FEC1C3CA3FAD95972707013BAAA2407DAD8D349DB36E5FC36A551A01A4C0F96E`.
- Signed AAB SHA-256: `9C3BBD22A0C8A8415273E81270D16100169B6515A1F1DFB26B88B631AA9E6B28`.
- Signing certificate SHA-256: `a4f07e70cc0de4224411fdcdeb81e3e11d5b4ba949230cf108f3763a39fd4a1e`.
- The AAB contains ReTrace mapping and R8 metadata.
