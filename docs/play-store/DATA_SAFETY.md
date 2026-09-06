# Google Play declarations

These answers describe code 22, version 0.9.0-beta.14. Rechecked September 6, 2026.

## Data safety

- Data collected: **No**.
- Data shared: **No**.
- Ads: **No**.
- Account creation: **No**.
- App access restrictions: **No**.
- Local deletion: **Settings > Data and transfer > Clear this calendar** clears the selected profile. **Manage profiles > Delete profile** removes an additional profile. Android app-storage deletion removes all local profiles.
- Network permission: **No**.
- Android device-to-device transfer may copy private app data during new-device setup. Normal cloud backup is blocked.
- User-selected compatible `.pc` backups are read and merged locally. They are not uploaded.
- User-requested `.pc` exports are written only to the selected Android file-provider destination. They are not encrypted. A cloud-backed provider may upload the file under its own terms; Selia Cycles has no upload service.
- Clearing app data does not delete exported files or provider-held copies. Users must delete these separately.
- Optional Android calendar read/write permission lists writable calendars and mirrors short cycle labels to the calendar the user selects.
- Selia Cycles does not transmit calendar or cycle data. A selected calendar provider may synchronize the user-directed copies under its own terms.
- The calendar transfer is an explicit user-initiated action with an in-app disclosure before the runtime permission and target selection; Google Play lists this as an exception from the Data safety "shared" disclosure.
- External support, source, and privacy links open in the browser without attaching cycle data. Browser connections and any support correspondence are described in the privacy policy.

## Health apps declaration

- Health feature: **Period Tracking**, which Google defines to include ovulation tracking/prediction and fertility awareness.
- Reproductive signs and test results remain in app-private local storage and are not transmitted.
- Medical device: **No**.
- Diagnosis or treatment: **No**.
- Research: **No**.
- Health Connect permissions: **None**.
- Store descriptions explicitly state that the app is not a medical device and cannot diagnose, treat, cure, or prevent a medical condition, and direct users to a healthcare professional.

## Store setup

- Category: **Health & Fitness**.
- Target audience: **13–15, 16–17, and 18 and over**. The app does not target children under 13.
- Privacy policy URL: `https://majkey25.github.io/SeliaCycles/`.
- Contact email: `majkeylab@gmail.com`.
- Contains ads: **No**.

Recheck these answers after every dependency or feature change.

## September 6 verification

Play Console's saved form selects Period Tracking and no medical-device category. Data safety answers No to reportable collection or sharing; the privacy URL matches the app's link. No declaration required attention at the time of this check.

`aapt dump permissions` on the signed code 22 APK confirms no Internet, advertising-ID, or Health Connect permission. In addition to calendar and notification permissions, WorkManager contributes wake lock, boot-completed, and foreground-service permissions. AndroidX adds a signature-protected receiver permission.

The backup agent and both Android backup rule files permit device-to-device transfer and exclude ordinary cloud backup. Export uses Android's user-selected document provider. No billing, advertising, or analytics SDK is declared in the app dependencies. `THIRD_PARTY_NOTICES.md` records the direct component license families, and the AAB retains AndroidX license texts. This is a scoped source/artifact check, not a complete transitive-license audit or legal certification.

Sources: [Data safety definitions and user-directed transfer exception](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en), [Health Content and Services](https://support.google.com/googleplay/android-developer/answer/16679511?hl=en).
