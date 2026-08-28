# Selia Cycles privacy policy

Effective: August 28, 2026

Selia Cycles is published by MajkeyLab. Contact `majkeylab@gmail.com` about this policy.

## Data stored by the app

Selia Cycles stores menstrual dates, flow, symptoms, mood, notes, weight, basal temperature, sleep, intimacy, reminder settings, and display settings. The app keeps this data in its private storage on your Android device.

Selia Cycles has no ads, analytics, or telemetry. Google sign-in and cloud sync are optional. Without cloud sync, cycle data remains on the device or in destinations explicitly selected for backup.

## Optional Google account and cloud sync

If you sign in and enable sync, Firebase Authentication receives your Google account identifier, display name, and email address. Cloud Firestore stores your account identifier, display name, menstrual dates, and flow. Notes, mood, symptoms, weight, temperature, sleep, intimacy, and imported source details are excluded from the default cloud payload.

MajkeyLab administers the Firebase project and can technically access data stored there. It is used only to provide sync, partner access, security, support, and deletion. It is not used for ads, analytics, profiling, or sale. Firebase encrypts network traffic and applies the app's authenticated access rules.

Partner sharing is optional. A one-time 128-bit invitation lets the signed-in recipient view the owner's menstrual dates and flow. Partners cannot edit the owner's records. The owner can revoke access at any time.

## Health Connect

Health Connect import is optional. If you tap **Import data** and grant permission, Selia Cycles reads menstrual period and flow history into the calendar. The app does not read Health Connect in the background and does not write, change, or delete Health Connect records. Imported period dates and flow are included in cloud sync only if you separately enable sync.

You can revoke Health Connect access in Android settings at any time.

## Backups

Android Auto Backup may copy the app database to the Google or device backup account selected in Android settings. Android controls this service. MajkeyLab cannot access the backup.

Manual backup is optional and starts only when you tap **Create encrypted backup**. Selia Cycles encrypts the file with AES-GCM and a key derived from your password. You choose the destination in Android's system file picker. The destination provider, such as Google Drive, Samsung My Files, or OneDrive, applies its own privacy policy. MajkeyLab cannot read the file or recover its password.

## Sharing and sale

Selia Cycles does not sell personal data. Google/Firebase processes the optional account and synced fields described above. A partner receives only the read-only fields you explicitly share. Opening a medical source link sends that browser request to the selected website.

## Retention and deletion

Selia Cycles keeps local data until you delete it. Use **Settings > Delete all data**, clear app storage, or uninstall. Use **Settings > Google account and partner > Delete cloud copy** to remove your synced calendar, invitations, and partner access from Firebase. Manual backup files remain where you saved them until you delete them from that provider.

## Security

Android app-private storage protects local data. Android cloud backup requires client-side encryption support. Manual backups use PBKDF2-HMAC-SHA-256 and AES-256-GCM. No storage method eliminates all risk. Protect your device and backup password.

## Medical disclaimer

Selia Cycles provides personal tracking and calendar estimates. It does not diagnose a condition, confirm ovulation, or provide contraception.

## Policy changes

The repository records policy changes in version control. A material change will update the effective date.
