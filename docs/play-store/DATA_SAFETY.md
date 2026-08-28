# Google Play declarations

These answers describe version `0.1.0-beta.1`. Recheck them after each dependency or feature change.

## Data safety

- Does the app collect or share required user data types? **No**.
- Reason: cycle data and Health Connect data stay on device. Google Play excludes on-device processing and end-to-end encrypted transfers from collection. The developer has no backup key or server access.
- Ads: **No**.
- Account creation: **No**.
- App access restrictions: **No**.
- User data deletion: **Settings > Delete all data**, Android clear storage, or uninstall.

## Health apps declaration

- Health feature: **Period Tracking**.
- Medical device: **No**.
- Diagnosis or treatment: **No**.
- Research: **No**.

## Health Connect permissions

- `android.permission.health.READ_MENSTRUATION`: optional import of menstrual period and flow records into the local calendar.
- `android.permission.health.READ_HEALTH_DATA_HISTORY`: optional import of the user's older menstrual history.
- No write permission.
- No background health-data permission.

Prominent disclosure shown before the import button:

> Reads menstrual period and flow history from Health Connect to add it to your private calendar. Data is never sent to the developer.

## Store setup

- Category: **Health & Fitness**.
- Initial target audience: **18 and over**.
- Privacy policy URL: `https://majkey25.github.io/SeliaCycles/`.
- Contact email: `majkeylab@gmail.com`.
- Contains ads: **No**.

## Policy evidence

- [Google Play Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Google Play Health apps declaration](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types)
