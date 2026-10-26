const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student";
const BOB_UID = "bob_teacher";

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

test("Unauthenticated user: cannot read tasks or schools", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
  await assertFails(unauthDb.collection("schools").get());
});

test("Authenticated user: can read schools", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("schools").doc("sch_1").set({
      name: "SMA 1 Maju",
      inviteCode: "SMA1-MAJU",
      isActive: true,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("schools").doc("sch_1").get());
});

test("Authenticated user: can create a task with createdBy matching their uid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_1").set({
      schoolId: "sch_1",
      createdBy: ALICE_UID,
      title: "PR Matematika",
      priority: "HIGH",
      scope: "STUDENT_TASK",
      dueDate: new Date().toISOString(),
    })
  );
});

test("Authenticated user: cannot create a task forged with another user uid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("tasks").doc("task_2").set({
      schoolId: "sch_1",
      createdBy: BOB_UID, // Forgery
      title: "PR Palsu",
      priority: "LOW",
      scope: "STUDENT_TASK",
      dueDate: new Date().toISOString(),
    })
  );
});

test("Authenticated user: can create and send a nudge", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("nudges").doc("nudge_1").set({
      schoolId: "sch_1",
      fromUserId: ALICE_UID,
      toUserId: BOB_UID,
      taskId: "task_1",
      templateText: "Ayo tugasnya dikerjakan!",
      dateKey: "2026-10-23",
    })
  );
});
