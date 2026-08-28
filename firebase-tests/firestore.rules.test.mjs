import { after, before, beforeEach, test } from "node:test";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  runTransaction,
  setDoc,
  updateDoc,
  query,
  where,
} from "firebase/firestore";

const projectId = "demo-selia-cycles";
const inviteToken = "AbCdEfGhIjKlMnOpQrStUv";
const expiredToken = "ZyXwVuTsRqPoNmLkJiHgFe";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId,
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});

beforeEach(async () => testEnv.clearFirestore());
after(async () => testEnv.cleanup());

const ownerCalendar = () => ({
  ownerUid: "owner",
  ownerName: "Owner",
  createdAt: Timestamp.fromMillis(1),
  updatedAt: Timestamp.fromMillis(1),
});

const periodDay = () => ({
  day: "2026-08-28",
  bleeding: true,
  flow: "MEDIUM",
  updatedAt: Timestamp.fromMillis(1),
});

async function seedCalendar({ membership = false } = {}) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const database = context.firestore();
    await setDoc(doc(database, "calendars/owner"), ownerCalendar());
    await setDoc(doc(database, "calendars/owner/days/2026-08-28"), periodDay());
    if (membership) {
      await setDoc(doc(database, "memberships/owner_reader"), {
        ownerUid: "owner",
        readerUid: "reader",
        inviteToken,
        createdAt: Timestamp.fromMillis(1),
      });
    }
  });
}

test("owner can write calendar and day", async () => {
  const database = testEnv.authenticatedContext("owner").firestore();

  await assertSucceeds(setDoc(doc(database, "calendars/owner"), ownerCalendar()));
  await assertSucceeds(setDoc(doc(database, "calendars/owner/days/2026-08-28"), periodDay()));
});

test("owner cannot upload inconsistent or private day fields", async () => {
  const database = testEnv.authenticatedContext("owner").firestore();
  await assertSucceeds(setDoc(doc(database, "calendars/owner"), ownerCalendar()));

  await assertFails(setDoc(doc(database, "calendars/owner/days/2026-08-28"), {
    ...periodDay(),
    bleeding: false,
    flow: "HEAVY",
  }));
  await assertFails(setDoc(doc(database, "calendars/owner/days/2026-08-28"), {
    ...periodDay(),
    note: "must stay local",
  }));
});

test("owner cannot rewrite calendar identity metadata", async () => {
  const database = testEnv.authenticatedContext("owner").firestore();
  await assertSucceeds(setDoc(doc(database, "calendars/owner"), ownerCalendar()));

  await assertFails(updateDoc(doc(database, "calendars/owner"), { createdAt: Timestamp.fromMillis(2) }));
  await assertFails(updateDoc(doc(database, "calendars/owner"), { ownerUid: "other" }));
});

test("unauthenticated and unrelated users cannot read", async () => {
  await seedCalendar();

  await assertFails(getDoc(doc(testEnv.unauthenticatedContext().firestore(), "calendars/owner")));
  await assertFails(getDoc(doc(testEnv.authenticatedContext("stranger").firestore(), "calendars/owner")));
});

test("partner membership is read only", async () => {
  await seedCalendar({ membership: true });
  const reader = testEnv.authenticatedContext("reader").firestore();

  await assertSucceeds(getDoc(doc(reader, "calendars/owner")));
  await assertSucceeds(getDoc(doc(reader, "calendars/owner/days/2026-08-28")));
  await assertFails(updateDoc(doc(reader, "calendars/owner/days/2026-08-28"), { flow: "HEAVY" }));
});

test("partner claims one valid invitation transactionally", async () => {
  await seedCalendar();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), `invitations/${inviteToken}`), {
      ownerUid: "owner",
      calendarId: "owner",
      createdAt: Timestamp.fromMillis(Date.now()),
      expiresAt: Timestamp.fromMillis(Date.now() + 60_000),
      acceptedBy: null,
    });
  });
  const reader = testEnv.authenticatedContext("reader").firestore();

  await assertSucceeds(runTransaction(reader, async (transaction) => {
    const invite = doc(reader, `invitations/${inviteToken}`);
    transaction.update(invite, { acceptedBy: "reader" });
    transaction.set(doc(reader, "memberships/owner_reader"), {
      ownerUid: "owner",
      readerUid: "reader",
      inviteToken,
      createdAt: Timestamp.fromMillis(Date.now()),
    });
  }));
  await assertSucceeds(getDoc(doc(reader, "calendars/owner")));
});

test("expired invitation and invitation listing are denied", async () => {
  await seedCalendar();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), `invitations/${expiredToken}`), {
      ownerUid: "owner",
      calendarId: "owner",
      createdAt: Timestamp.fromMillis(1),
      expiresAt: Timestamp.fromMillis(2),
      acceptedBy: null,
    });
  });
  const reader = testEnv.authenticatedContext("reader").firestore();

  await assertFails(updateDoc(doc(reader, `invitations/${expiredToken}`), { acceptedBy: "reader" }));
  await assertFails(getDocs(collection(reader, "invitations")));
});

test("owner can list only their invitations for cleanup", async () => {
  await seedCalendar();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), `invitations/${inviteToken}`), {
      ownerUid: "owner",
      calendarId: "owner",
      createdAt: Timestamp.fromMillis(Date.now()),
      expiresAt: Timestamp.fromMillis(Date.now() + 60_000),
      acceptedBy: null,
    });
  });
  const owner = testEnv.authenticatedContext("owner").firestore();
  const reader = testEnv.authenticatedContext("reader").firestore();
  const ownerInvites = query(collection(owner, "invitations"), where("ownerUid", "==", "owner"));
  const readerInvites = query(collection(reader, "invitations"), where("ownerUid", "==", "owner"));

  await assertSucceeds(getDocs(ownerInvites));
  await assertFails(getDocs(readerInvites));
});

test("owner revocation immediately removes partner access", async () => {
  await seedCalendar({ membership: true });
  const owner = testEnv.authenticatedContext("owner").firestore();
  const reader = testEnv.authenticatedContext("reader").firestore();

  await assertSucceeds(deleteDoc(doc(owner, "memberships/owner_reader")));
  await assertFails(getDoc(doc(reader, "calendars/owner/days/2026-08-28")));
});
