# Google Play declarations

These answers describe version 0.9.0-beta.12.

## Data safety

- Data collected: **No**.
- Data shared: **No**.
- Ads: **No**.
- Account creation: **No**.
- App access restrictions: **No**.
- Local deletion: **Settings > Data and transfer > Delete all data**.
- Network permission: **No**.
- Android device-to-device transfer may copy private app data during new-device setup. Normal cloud backup is blocked.
- User-selected compatible `.pc` backups are read and merged locally. They are not uploaded.
- User-requested `.pc` exports are written only to the selected destination. They are not uploaded or encrypted.
- Optional Android calendar read/write permission lists writable calendars and mirrors short cycle labels to the calendar the user selects.
- Selia Cycles does not transmit calendar or cycle data. A selected calendar provider may synchronize the user-directed copies under its own terms.
- The calendar transfer is an explicit user-initiated action with an in-app disclosure before the runtime permission and target selection; Google Play lists this as an exception from the Data safety "shared" disclosure.

## Health apps declaration

- Health feature: **Period Tracking**, which Google defines to include ovulation tracking/prediction and fertility awareness.
- Reproductive signs and test results remain in app-private local storage and are not transmitted.
- Medical device: **No**.
- Diagnosis or treatment: **No**.
- Research: **No**.
- Health Connect permissions: **None**.

## Store setup

- Category: **Health & Fitness**.
- Target audience: **13–15, 16–17, and 18 and over**. The app does not target children under 13.
- Privacy policy URL: `https://majkey25.github.io/SeliaCycles/`.
- Contact email: `majkeylab@gmail.com`.
- Contains ads: **No**.

Recheck these answers after every dependency or feature change.
