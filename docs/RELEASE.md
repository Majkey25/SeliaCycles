# Release Selia Cycles

## Verify the source

Run all local gates before you build a release:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleQa :app:assembleQaAndroidTest --console=plain
```

Reserve the connected test phone before any ADB command. Install only the `.qa` app and `.qa.test` APK, then run the complete instrumentation suite on its explicit serial:

```powershell
adb -s <reserved-serial> install -r app/build/outputs/apk/qa/app-qa.apk
adb -s <reserved-serial> install -r app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
adb -s <reserved-serial> shell am instrument -w -r com.majkeylab.seliacycles.qa.test/androidx.test.runner.AndroidJUnitRunner
```

Record the APK hashes and instrumentation result before publishing. Remove only the QA packages and release the phone afterwards. Never seed or clear the production app. Pull requests run JVM tests, lint, and builds; emulator QA is an optional manual workflow, disabled by default. A skipped emulator job does not replace physical release verification.

## Sign the app bundle

Keep the upload keystore and its properties file outside the repository. The properties file must contain `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.

```powershell
.\gradlew.bat :app:bundleRelease --console=plain -PdistributionChannel=play -PseliaCyclesKeystoreProperties=C:\secure\selia-cycles-keystore.properties
```

Verify `app/build/outputs/bundle/release/app-release.aab` with `jarsigner -verify` before upload.

Release builds default to the Google Play channel and contain no external donation action. For a GitHub release APK, run `:app:assembleRelease -PdistributionChannel=github` with the same signing properties. Do not attach the Play AAB as the GitHub installation APK. Bundling the GitHub channel fails rather than producing a donation-enabled Play bundle.

Debug and QA builds keep the GitHub support button unless `-PdistributionChannel=play` is explicit. Build and run `LegalLinksTest` on both channels to check that legal links remain available and donations appear only outside Play.

## Publish a closed test

1. Create the app in Google Play Console with package `com.majkeylab.seliacycles`.
2. Enable Play App Signing.
3. Keep the category **Health & Fitness** and the existing **18 and over** audience. Any audience change requires reviewing the store declarations and applicable child-safety requirements first.
4. Complete the Data safety, Health apps, app access, ads, content rating, and privacy policy forms. Use [the recorded answers](play-store/DATA_SAFETY.md).
5. Upload the signed AAB to **Closed testing**.
6. Add a tester email list or Google Group and a feedback email.
7. Review warnings. Roll out only when every required form is complete.

The package name cannot change after the first uploaded bundle. Every later upload needs a new, higher `versionCode`; an uploaded code cannot be reused.
