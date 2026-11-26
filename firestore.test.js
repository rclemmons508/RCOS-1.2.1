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
const CAROL_UID = "carol_member_3";
const CAROL_EMAIL = "carol@example.com";
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

async function seedLiveBusinessForAlice() {
  // Live-schema business (old app / biz_1794932142514 shape): ownerId +
  // members roster + memberEmails + documentContactInfo, NO memberUids,
  // NO top-level contactEmail. Seeded with rules disabled.
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("businesses").doc(BIZ_ID).set({
      ownerId: ALICE_UID,
      members: [{ userId: "alice_local_1", email: "alice@example.com", name: "Alice", role: "owner" }],
      memberEmails: { "alice@example.com": "owner", "carol@example.com": "member" },
      documentContactInfo: { email: "alice@example.com", phone: "(470) 713-6068" },
      name: "RC Solutions",
      industry: "general_small_business",
      services: ["Service Call"],
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

  // Owner can save per-agent settings (agentSettings map, added with
  // the agent edit screen); a member cannot touch the business doc.
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_1").update({
      agentSettings: {
        sales: {
          permissions: ["draft_quotes", "send_messages"],
          autoActLimit: 250,
          escalateAbove: 1000,
          requireApprovalForMoney: true,
          customInstructions: "",
        },
      },
      updatedAt: pastDate(900),
    })
  );

  // 8 fixes Nov 26 (item 5) — owner can save the service catalog
  // (name + price per service) and switch the industry preset.
  await assertSucceeds(
    aliceDb.collection("businesses").doc("biz_1").update({
      industry: "cleaning",
      services: ["Standard Clean", "Deep Clean"],
      serviceCatalog: [
        { name: "Standard Clean", price: "$85" },
        { name: "Deep Clean", price: "from $180" },
      ],
      updatedAt: pastDate(800),
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

  // 8 fixes Nov 26 (item 3) — create a customer, then update its
  // notes (aiSummary) and owner-defined custom fields (label + value).
  await assertSucceeds(bizRef.collection("customers").doc("cust_1").set({
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    name: "Johnson Residence",
    phone: "555-0100",
    status: "active",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(bizRef.collection("customers").doc("cust_1").update({
    aiSummary: "Prefers texting, not calls.",
    customFields: [
      { label: "Gate code", value: "0420" },
      { label: "Preferred contact", value: "Text, not calls" },
    ],
    updatedAt: pastDate(1000),
  }));
  // A ghost field on the customer update is still rejected.
  await assertFails(bizRef.collection("customers").doc("cust_1").update({
    favoriteColor: "green",
    updatedAt: pastDate(500),
  }));
});

test("Phase 1: live-schema business (ownerId + members, no memberUids) is readable by its owner", async () => {
  await seedLiveBusinessForAlice();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("businesses").doc(BIZ_ID).get());
  await assertSucceeds(aliceDb.collection("businesses").where("ownerId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("businesses").doc(BIZ_ID).get());

  // Owner can update company contact fields on the legacy-shaped doc.
  await assertSucceeds(
    aliceDb.collection("businesses").doc(BIZ_ID).update({ contactPhone: "(470) 713-6068", updatedAt: pastDate(1000) })
  );
  // A stranger cannot.
  await assertFails(
    bobDb.collection("businesses").doc(BIZ_ID).update({ contactPhone: "000", updatedAt: pastDate(500) })
  );
});

test("Phase 1: member via memberEmails can read the business; profiles are per-user only", async () => {
  await seedLiveBusinessForAlice();
  const carolDb = testEnv.authenticatedContext(CAROL_UID, { email: CAROL_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(carolDb.collection("businesses").doc(BIZ_ID).get());

  // Per-user profiles: own doc only.
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      phone: "(470) 713-6068",
      contactEmail: "alice@example.com",
      createdAt: pastDate(8000),
      updatedAt: pastDate(8000),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(carolDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    carolDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Hijacked",
      createdAt: pastDate(8000),
      updatedAt: pastDate(8000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(CAROL_UID).set({
      uid: CAROL_UID,
      displayName: "Hijacked",
      createdAt: pastDate(8000),
      updatedAt: pastDate(8000),
    })
  );
});

test("Phase 1: invoices & quotes CRUD for members, owner-only delete", async () => {
  await seedBusinessForAlice();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(8000);
  const invoice = {
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    title: "Invoice: Johnson Residence",
    clientName: "Johnson Residence",
    amount: 420.0,
    status: "unpaid",
    createdAt: ts,
    updatedAt: ts,
  };
  const quote = {
    businessId: BIZ_ID,
    ownerId: ALICE_UID,
    memberUids: [ALICE_UID],
    title: "Quote: Johnson Residence",
    clientName: "Johnson Residence",
    amount: 420.0,
    status: "awaiting_approval",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("businesses").doc(BIZ_ID).collection("invoices").doc("inv_1").set(invoice));
  await assertSucceeds(aliceDb.collection("businesses").doc(BIZ_ID).collection("quotes").doc("q_1").set(quote));
  await assertSucceeds(
    aliceDb.collection("businesses").doc(BIZ_ID).collection("invoices").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("businesses").doc(BIZ_ID).collection("invoices").doc("inv_1").update({ status: "paid", updatedAt: pastDate(1000) })
  );
  await assertSucceeds(
    aliceDb.collection("businesses").doc(BIZ_ID).collection("quotes").doc("q_1").update({ status: "approved", jobId: "job_1", updatedAt: pastDate(1000) })
  );
  await assertFails(bobDb.collection("businesses").doc(BIZ_ID).collection("invoices").doc("inv_1").get());
  await assertFails(
    bobDb.collection("businesses").doc(BIZ_ID).collection("invoices").doc("inv_1").update({ status: "paid", updatedAt: pastDate(500) })
  );
  await assertFails(bobDb.collection("businesses").doc(BIZ_ID).collection("invoices").doc("inv_1").delete());
});
