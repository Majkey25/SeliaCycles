# Code 17 Google Play screenshots

Date: 2026-09-01

## Reference

The published SeliaScan default listing uses eight raw phone screenshots at 1080 × 1920 pixels. It does not add a marketing frame or generated device shell. Selia Cycles now follows the same format.

## Local set

- Locale: `en-US`.
- Count: 8.
- Format: PNG.
- Dimensions: 1080 × 1920 for every image.
- Content: Today, Calendar, exact period editor, day overview, History, daily information, phase guidance, and Settings.
- Source: real Selia Cycles UI. No generated or reconstructed app screen is included.
- File sizes and SHA-256 hashes are recorded in `docs/play-store/assets/screenshots-manifest.json`.

## Play Console

- The default listing already contained four phone screenshots.
- Four new code 17 screenshots were uploaded to the Play asset library: Calendar, exact period editor, day overview, and daily information.
- Google Play accepted every new file as PNG, 9:16, 1080 × 1920.
- The four assets are selected and ready for the `Add` action. The listing has not yet been saved or submitted.

## Device cleanup

- Temporary display override was reset from 1080 × 1920 to the physical 1080 × 2340.
- App language was restored from English to Czech.
- Invalid captures taken while another test app owned the foreground were discarded and were not uploaded.
