# Selia Cycles 0.9.0-beta.5 import and export acceptance

Date: 2026-09-01

## Build

- Version: code 13, `0.9.0-beta.5`.
- Clean tests, lint, minified release APK, and signed AAB completed successfully.
- Unit tests: 85 run, 0 failures, 0 errors, 0 skipped.
- Signed APK: 4,114,624 bytes, SHA-256 `593635CA851E325A8065D960959D8A5C7FFC582D16E4C7476C0C188AD0DF91B4`.
- Signed AAB: 5,333,864 bytes, SHA-256 `92794D2EDAC854D58B652641DDEB040C1608FFDAEE67088DEC000CCA6432E267`.
- The AAB contains ReTrace mapping and R8 metadata.

## User-facing copy

- The backup section is named Import and export in English, Czech, Slovak, German, Polish, and Spanish.
- Preview, success, empty, damaged, and unsupported-backup messages use neutral `.pc` wording.
- No third-party calendar brand remains in app resources, README, privacy text, changelog, or Play listing copy.

## Real `.pc` verification

- Source file: 154,036 bytes, SHA-256 `F5434C9EF5BA52F4F5684C1808D7E9F1F9B93F9E59CB7C85C09F663044513B87`.
- The container uses a Java object envelope followed by ZIP entries. Its authoritative `cloud.db` payload has a plain SQLite header, so database decryption is neither required nor attempted.
- The app validates the envelope, bounded ZIP contents, SQLite header, required tables and columns, numeric ranges, dates, period lengths, and note sizes before showing a preview.
- Signed nonzero period lengths are treated as records through their absolute value; zero rows remain placeholders.
- Physical Huawei preview found 1,352 recorded days from 2014-12-25 through 2026-07-12 and preserved 695 unmatched source details.
- The preview was cancelled. Home remained on cycle day 20 with the next period in 25 days, proving the preview did not merge data.
- The temporary device copy was removed after the check.

## Physical Huawei QA

Target: `BQLDU19927002646`, Huawei YAL-L21, Android 10.

- Minified code 13 updated code 12 without clearing local data or changing the original first-install timestamp.
- Cold launch returned a live PID.
- The Czech Data and transfer page showed Import a export, the neutral `.pc` explanation, Exportovat zálohu `.pc`, and Vybrat zálohu `.pc`.
- The real import preview showed Náhled importu and no third-party brand.
- Logcat contained no fatal exception or import failure.
