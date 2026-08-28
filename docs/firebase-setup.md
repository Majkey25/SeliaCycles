# Firebase setup for Selia Cycles

Google account and partner sync are optional. Builds without Firebase configuration remain fully functional offline and show that sync is unavailable.

## Required external configuration

1. Create a dedicated Firebase project without Google Analytics.
2. Register Android package `com.majkeylab.seliacycles`.
3. Add the debug and Play upload/app-signing SHA-1 and SHA-256 certificate fingerprints.
4. Enable Firebase Authentication with the Google provider.
5. Create the default Cloud Firestore database.
6. Download the Android `google-services.json` to `app/google-services.json`.
7. Pass the web OAuth client ID at build time:

```text
-PseliaCyclesGoogleWebClientId=<verified web client ID>
```

The JSON file and client ID are local release inputs. They are not committed.

## Rules verification

Firestore rules use a demo-only emulator project and cannot contact production:

```text
cd firebase-tests
npm ci --ignore-scripts
npm audit --omit=dev
npm test
```

Firebase CLI 15 requires Java 21 for the emulator. The Android build remains on Java 17.

Deploy `firestore.rules` and `firestore.indexes.json` only after all rule tests pass. Then verify with two real Google accounts:

- owner sync and invitation creation;
- partner invitation acceptance and read-only calendar view;
- partner write denial;
- owner revocation;
- cloud-copy deletion;
- sign-out without local calendar loss.
