# Selia Cycles implementation plan

1. Scaffold pinned Android project. Write failing `CyclePredictorTest`. Verify RED with `:app:testDebugUnitTest`.
2. Add typed cycle model and minimal predictor. Verify GREEN.
3. Add native SQLite store, validated AES-GCM backup codec, Health Connect reader, and reminder worker.
4. Add Material 3 Today, Calendar, History, Settings, daily-log dialog, Czech/English resources, and permission rationale.
5. Run unit test, lint, assemble. Install only on an emulator. Verify happy, edge, failure, and nearby regression flows.
6. Create the public `Majkey25/SeliaCycles` repository with CI and GitHub Pages.
7. Upload the signed AAB to Google Play Closed testing and complete required declarations.

The user later authorized repository creation, commit, push, and Google Play Closed testing publication.
