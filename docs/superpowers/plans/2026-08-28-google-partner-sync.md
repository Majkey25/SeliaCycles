# Google Partner Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional Google sign-in and a secure read-only partner calendar while preserving complete offline use.

**Architecture:** Use Credential Manager with Firebase Authentication and Cloud Firestore. Local SQLite remains the source of truth; an opt-in sync repository mirrors owner-approved fields. Firestore roles and expiring invitation documents enforce owner-write/partner-read behavior.

**Tech Stack:** Kotlin, Android Credential Manager, Firebase Auth, Cloud Firestore, Firebase Emulator Suite, Firestore Security Rules.

---

### Task 1: Verified Firebase project boundary

**Files:**
- Modify: `app/build.gradle.kts`
- Add: `app/google-services.json` only from the verified Firebase console
- Create: `docs/firebase-setup.md`

- [ ] Confirm the exact external action before creating the Firebase project, OAuth clients, or permissions.
- [ ] Register `com.majkeylab.seliacycles` with debug and release SHA fingerprints.
- [ ] Enable Google Authentication and create Firestore without Analytics.
- [ ] Add pinned Firebase BoM, Auth, Firestore, Credential Manager, Play Services auth, and Google ID dependencies from current official docs.
- [ ] Verify `processDebugGoogleServices` and keep secrets out of Git output.

### Task 2: Firestore model and deny-by-default rules

**Files:**
- Create: `firebase.json`
- Create: `firestore.rules`
- Create: `firestore.indexes.json`
- Create: `functions-or-rules-tests/firestore.rules.test.*`

- [ ] Define `calendars/{ownerUid}`, `calendars/{ownerUid}/days/{isoDate}`, and `invitations/{token}`.
- [ ] Deny unauthenticated access and all collection listing for invitations.
- [ ] Allow owner writes, bounded reader reads, and one transactional invite acceptance.
- [ ] Test owner write, reader read, reader write denial, unrelated-user denial, expiry, revocation, immutable owner, allowed keys, and reader limit in the emulator.
- [ ] Deploy rules only after emulator tests pass.

### Task 3: Google sign-in

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/GoogleAccountManager.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Add Credential Manager sign-in with the generated `default_web_client_id`.
- [ ] Exchange Google ID tokens for Firebase credentials.
- [ ] Add explicit signed-out, signing-in, signed-in, and error states.
- [ ] Clear Credential Manager state on sign-out without deleting local calendar data.
- [ ] Live verify sign-in, cancellation, sign-out, and relaunch persistence.

### Task 4: Offline-first owner sync

**Files:**
- Create: `app/src/main/java/com/majkeylab/seliacycles/CalendarSyncRepository.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CycleStore.kt`

- [ ] Write tests for typed serialization, allowed-field filtering, conflict ordering, and bounded listeners.
- [ ] Upload only after an explicit sync opt-in disclosure.
- [ ] Keep notes, measurements, intimacy, symptoms, and tests excluded by default.
- [ ] Reconcile remote owner data by updated timestamp; keep SQLite usable offline.
- [ ] Stop listeners on sign-out and ViewModel cleanup.

### Task 5: Invite, read-only partner overlay, and revocation

**Files:**
- Modify: `app/src/main/java/com/majkeylab/seliacycles/CalendarSyncRepository.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/MainViewModel.kt`
- Modify: `app/src/main/java/com/majkeylab/seliacycles/SeliaCyclesApp.kt`

- [ ] Generate a cryptographically random 128-bit URL-safe invitation token with a 24-hour expiry.
- [ ] Accept the token in one Firestore transaction and add only the authenticated partner UID.
- [ ] Let the partner select the owner calendar as a read-only overlay.
- [ ] Hide every editing action while an owner overlay is selected.
- [ ] Let the owner revoke a reader and verify that the listener immediately loses access.
- [ ] Live verify two Google accounts before Closed testing.

### Task 6: Privacy, Play declarations, and release

**Files:**
- Modify: `docs/privacy-policy.html`
- Modify: `README.md`
- Modify: `docs/qa/2026-08-28-acceptance.md`

- [ ] Update the privacy policy and in-app disclosure for account identifiers and synced health data.
- [ ] Update Play Data Safety and Health declarations to match the verified implementation.
- [ ] Run unit, lint, debug, signed bundle, Firestore rule, and two-account physical-device gates.
- [ ] Publish only the new verified AAB to Closed testing.
