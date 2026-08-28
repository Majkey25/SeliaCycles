# Google Play declarations

These answers describe the redesigned closed-beta build. Recheck them after each dependency or feature change.

## Data safety

- Data collected when optional Google sync is enabled: **User IDs**, **email address**, **name**, and **health information** consisting of menstrual dates and flow.
- Purpose: **App functionality**, **account management**, **security**, and **user-requested partner sharing**.
- Data excluded from cloud sync: notes, mood, symptoms, weight, temperature, sleep, intimacy, and imported source details.
- Data is encrypted in transit. Firebase project administrators can technically access stored data; it is not used for ads, analytics, profiling, or sale.
- Ads: **No**.
- Account creation: **Optional Google/Firebase account**.
- App access restrictions: **No**.
- User data deletion: local deletion in Settings; separate **Delete cloud copy** for Firebase data, invitations, and partner access.

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

> Reads menstrual period and flow history from Health Connect into your calendar. It stays local unless you separately enable Google cloud sync, which includes period dates and flow.

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
