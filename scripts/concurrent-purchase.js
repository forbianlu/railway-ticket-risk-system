const API_BASE = process.env.API_BASE || "http://localhost:8080/api";
const REQUESTS = Number(process.env.REQUESTS || process.argv[2] || 30);
const FROM = process.env.FROM || "BJP";
const TO = process.env.TO || "SHH";
const DATE = process.env.DATE || new Date().toISOString().slice(0, 10);

async function main() {
  const target = await resolveTargetInventory();
  const before = await findInventory(target.inventoryId);
  const startedAt = Date.now();

  const responses = await Promise.all(
    Array.from({ length: REQUESTS }, (_, index) => createOrder(target, index))
  );

  const elapsedMs = Date.now() - startedAt;
  const after = await findInventory(target.inventoryId);
  const summary = responses.reduce((acc, item) => {
    const key = String(item.status);
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});

  console.log("Concurrent purchase stress result");
  console.log(`API: ${API_BASE}`);
  console.log(`Target: trainId=${target.trainId}, inventoryId=${target.inventoryId}`);
  console.log(`Requests: ${REQUESTS}, elapsedMs=${elapsedMs}`);
  console.log(`Remaining seats: ${before.remainingSeats} -> ${after.remainingSeats}`);
  console.log("HTTP status counts:", summary);
}

async function resolveTargetInventory() {
  if (process.env.TRAIN_ID && process.env.INVENTORY_ID) {
    return {
      trainId: Number(process.env.TRAIN_ID),
      inventoryId: Number(process.env.INVENTORY_ID),
    };
  }

  const trains = await request(`/trains/search?from=${FROM}&to=${TO}&date=${DATE}`);
  if (!trains.length) {
    throw new Error(`No train inventory found for ${FROM} -> ${TO} on ${DATE}`);
  }
  const target = trains.find(item => item.seatType === "SECOND_CLASS") || trains[0];
  return {
    trainId: target.trainId,
    inventoryId: target.inventoryId,
  };
}

async function createOrder(target, index) {
  const userId = 800000 + index;
  const response = await fetch(`${API_BASE}/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId,
      requestId: `stress-${Date.now()}-${index}`,
      trainId: target.trainId,
      inventoryId: target.inventoryId,
      passengerName: `LoadUser${index}`,
      passengerIdCard: `11010120000303${String(userId).slice(-4)}`,
    }),
  });
  return {
    status: response.status,
    body: await safeJson(response),
  };
}

async function findInventory(inventoryId) {
  const trains = await request(`/trains/search?from=${FROM}&to=${TO}&date=${DATE}`);
  const inventory = trains.find(item => item.inventoryId === inventoryId);
  if (!inventory) {
    throw new Error(`Inventory ${inventoryId} not found in search result`);
  }
  return inventory;
}

async function request(path) {
  const response = await fetch(`${API_BASE}${path}`);
  const body = await safeJson(response);
  if (!response.ok) {
    throw new Error(body.message || `Request failed with ${response.status}`);
  }
  return body;
}

async function safeJson(response) {
  const text = await response.text();
  if (!text) {
    return {};
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    return { raw: text };
  }
}

main().catch(error => {
  console.error(error.message);
  process.exit(1);
});
