# Release Selia Cycles

## Verify the source

Run all local gates before you build a release:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

## Sign the app bundle

Keep the upload keystore and its properties file outside the repository. The properties file must contain `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.

```powershell
.\gradlew.bat :app:bundleRelease --console=plain -PseliaCyclesKeystoreProperties=C:\secure\selia-cycles-keystore.properties
```

Verify `app/build/outputs/bundle/release/app-release.aab` with `jarsigner -verify` before upload.

## Publish a closed test

1. Create the app in Google Play Console with package `com.majkeylab.seliacycles`.
2. Enable Play App Signing.
3. Set the category to **Health & Fitness** and select target groups **13–15**, **16–17**, and **18 and over**. Do not select an under-13 group.
4. Complete the Data safety, Health apps, app access, ads, content rating, and privacy policy forms. Use [the recorded answers](play-store/DATA_SAFETY.md).
5. Upload the signed AAB to **Closed testing**.
6. Add a tester email list or Google Group and a feedback email.
7. Review warnings. Roll out only when every required form is complete.

Package name and version code cannot be changed after the first uploaded bundle.
