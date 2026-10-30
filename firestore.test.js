const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write memories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("memories").get()
  );
  const now = new Date(Date.now() - 1000);
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      category: "FACT",
      content: "Secret fact",
      source: "User",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and read their own valid memory fact", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1");
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      category: "PREFERENCE",
      content: "User prefers concise dark mode responses",
      source: "User",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(docRef.get());
});

test("Authenticated user: cannot read or write another user's memories", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("memories")
      .doc("mem_alice")
      .set({
        userId: ALICE_UID,
        category: "FACT",
        content: "Alice private memory",
        source: "User",
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_alice").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_bob_inject").set({
      userId: BOB_UID,
      category: "FACT",
      content: "Injected memory",
      source: "User",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Shadow field injection or invalid category is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_bad");
  const now = new Date(Date.now() - 1000);
  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      category: "FACT",
      content: "Valid content",
      source: "User",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    docRef.set({
      userId: ALICE_UID,
      category: "INVALID_CATEGORY",
      content: "Valid content",
      source: "User",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and list their own cloud chat messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const msgRef = aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1");
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "assistant",
      content: "Opening YouTube.",
      providerName: "Local Command Engine",
      modelName: "AppLauncherService",
      routeCategory: "LOCAL ENGINE",
      latencyMs: 3,
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});
