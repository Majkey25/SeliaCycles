# Selia Cycles Google account and partner sync

Date: August 28, 2026

## Product boundary

- Sync is optional. The calendar remains fully usable offline without an account.
- Google sign-in uses Android Credential Manager and Firebase Authentication.
- Cloud Firestore stores only data the owner explicitly enables for sync.
- Existing local records remain authoritative until the first successful sync.

## Partner access

- The owner creates a high-entropy, expiring invitation code.
- A signed-in partner enters the code and receives reader access.
- The partner can select the owner's calendar as a read-only overlay.
- Partners cannot create, update, or delete owner records.
- The owner can revoke access immediately from Settings.

## Data and privacy

- Calendar documents contain an owner UID and bounded reader UID list.
- Day documents use ISO dates and validated cycle fields.
- Notes, weight, temperature, intimacy, symptoms, and tests are excluded from partner sharing by default.
- The owner may enable additional categories individually after a clear disclosure.
- Sign-out removes local cloud session state but does not delete local calendar data.

## Firestore security

- Owners alone can write calendar metadata and day documents.
- Readers can only read a calendar whose metadata contains their authenticated UID.
- Invitations deny list queries and allow a single authenticated acceptance transaction.
- Rules validate allowed keys, data types, date bounds, reader limits, and immutable ownership.
- Emulator tests cover owner writes, reader reads, reader write denial, unrelated-user denial, expired invitations, and revocation.

## Delivery boundary

- The repository contains client code, Firestore rules, rule tests, and setup documentation.
- Live Google sign-in requires a verified Firebase project, SHA fingerprints, Google provider, Firestore database, and `google-services.json`.
- Creating OAuth credentials or cloud permissions is a separate external action and must be verified before release.
