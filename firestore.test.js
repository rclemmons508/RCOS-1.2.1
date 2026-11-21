const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_1";
const BOB_UID = "bob_intruder_2";
const BIZ_ID = "biz_rc_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

async function seedBusinessForAlice() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("businesses").doc(BIZ_ID).set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      name: "RC Solutions",
      industry: "general_small_business",
      services: ["Service Call", "Site Estimate"],
      autonomyMode: "supervised",
      answeringMode: "ai",
      aiReceptionistEnabled: true,
      dollarThreshold: 250.0,
      onboardingCompleted: true,
      subscriptionTier: "Pro Plan",
      createdAt: pastDate(20000),
      updatedAt: pastDate(20000),
    });
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write businesses or subcollections", async () => {
  await seedBusinessForAlice();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("businesses").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("businesses").doc(BIZ_ID).get());
  await assertFails(unauthDb.collection("businesses").doc(BIZ_ID).collection("tasks").doc("t1").set({
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    title: "Unauthorized task",
    bucket: "today",
    category: "call",
    completed: false,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  }));
});

test("Authenticated owner can create and partially update BusinessAccount; ghost fields and @DocumentId rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_1").set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      name: "RC Solutions",
      industry: "landscaping",
      services: ["Lawn Care"],
      autonomyMode: "supervised",
      answeringMode: "ai",
      aiReceptionistEnabled: true,
      dollarThreshold: 250.0,
      onboardingCompleted: true,
      subscriptionTier: "Pro Plan",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update matching RcosRepository.updateBusinessFields
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_1").update({
      aiReceptionistEnabled: false,
      answeringMode: "manual",
      updatedAt: pastDate(1000),
    })
  );

  // Reject ghost field & @DocumentId 'id'
  await assertFails(
    aliceDb.collection("businesses").doc("biz_2").set({
      id: "biz_2",
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
      name: "Bad Doc",
      industry: "landscaping",
      autonomyMode: "supervised",
      dollarThreshold: 250.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject immutable ownerId hijack
  await assertFails(
    aliceDb.collection("businesses").doc("biz_1").update({
      ownerId: BOB_UID,
      updatedAt: pastDate(500),
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, or mutate Alice's business or subcollections", async () => {
  await seedBusinessForAlice();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertFails(bobDb.collection("businesses").doc(BIZ_ID).get());
  await assertFails(bobDb.collection("businesses").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(bobDb.collection("businesses").get());
  await assertFails(
    bobDb.collection("businesses").doc(BIZ_ID).update({
      name: "Hijacked Co",
      updatedAt: pastDate(500),
    })
  );
});

test("Kotlin-aligned queries & CRUD for jobs, customers, tasks, calls, actions, auditLogs", async () => {
  await seedBusinessForAlice();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bizRef = aliceDb.collection("businesses").doc(BIZ_ID);
  const ts = pastDate(8000);

  // Scoped memberUids queries matching RcosRepository.observe*
  await assertSucceeds(aliceDb.collection("businesses").where("memberUids", "array-contains", ALICE_UID).get());
  await assertSucceeds(bizRef.collection("jobs").where("memberUids", "array-contains", ALICE_UID).get());
  await assertSucceeds(bizRef.collection("customers").where("memberUids", "array-contains", ALICE_UID).get());
  await assertSucceeds(bizRef.collection("tasks").where("memberUids", "array-contains", ALICE_UID).get());
  await assertSucceeds(bizRef.collection("calls").where("memberUids", "array-contains", ALICE_UID).get());
  await assertSucceeds(bizRef.collection("actions").where("memberUids", "array-contains", ALICE_UID).get());

  // Unfiltered list queries must fail
  await assertFails(bizRef.collection("jobs").get());

  // Create Task & toggle completed
  await assertSucceeds(bizRef.collection("tasks").doc("task_1").set({
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    title: "Call back new lead",
    bucket: "today",
    category: "call",
    completed: false,
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(bizRef.collection("tasks").doc("task_1").update({
    completed: true,
    updatedAt: pastDate(1000),
  }));

  // Create Job & complete it, then verify terminal state lock blocks further updates
  await assertSucceeds(bizRef.collection("jobs").doc("job_1").set({
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    title: "Spring Service",
    clientName: "Johnson Residence",
    status: "dispatched",
    priority: "medium",
    estimatedValue: 350.0,
    scheduledDate: "2026-11-22",
    scheduledTime: "9:00 AM",
    category: "Maintenance",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(bizRef.collection("jobs").doc("job_1").update({
    status: "completed",
    updatedAt: pastDate(1000),
  }));
  await assertFails(bizRef.collection("jobs").doc("job_1").update({
    status: "in_progress",
    updatedAt: pastDate(500),
  }));

  // Create Action & AuditLog
  await assertSucceeds(bizRef.collection("actions").doc("act_1").set({
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    employeeId: "sales",
    actionType: "generate_quote",
    title: "Quote: Johnson Residence",
    status: "awaiting_approval",
    riskCategory: "commitment_outbound",
    requiresApproval: true,
    dollarAmount: 420.0,
    paymentStatus: "unpaid",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(bizRef.collection("actions").doc("act_1").update({
    status: "approved",
    decidedBy: "Alice",
    updatedAt: pastDate(1000),
  }));
  await assertSucceeds(bizRef.collection("auditLogs").doc("log_1").set({
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    actionId: "act_1",
    userId: ALICE_UID,
    actionType: "generate_quote",
    decision: "approved",
    summary: "Approved: Quote: Johnson Residence",
    createdAt: ts,
  }));
});
