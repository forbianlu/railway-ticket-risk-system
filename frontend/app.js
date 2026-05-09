const API_BASE = "http://localhost:8080/api";

const state = {
  stations: [],
  auth: loadStoredAuth(),
};

const elements = {
  apiStatus: document.querySelector("#api-status"),
  apiStatusText: document.querySelector("#api-status-text"),
  totalOrders: document.querySelector("#metric-total-orders"),
  paidOrders: document.querySelector("#metric-paid-orders"),
  refundedOrders: document.querySelector("#metric-refunded-orders"),
  openRisks: document.querySelector("#metric-open-risks"),
  popularTrains: document.querySelector("#popular-trains"),
  fromStation: document.querySelector("#from-station"),
  toStation: document.querySelector("#to-station"),
  travelDate: document.querySelector("#travel-date"),
  trainResults: document.querySelector("#train-results"),
  orderResults: document.querySelector("#order-results"),
  orderUserId: document.querySelector("#order-user-id"),
  riskList: document.querySelector("#risk-list"),
  logList: document.querySelector("#log-list"),
  authRole: document.querySelector("#auth-role"),
  authUser: document.querySelector("#auth-user"),
  loginForm: document.querySelector("#login-form"),
  loginUsername: document.querySelector("#login-username"),
  loginPassword: document.querySelector("#login-password"),
  logoutButton: document.querySelector("#logout-button"),
  toast: document.querySelector("#toast"),
};

document.querySelector("#refresh-dashboard").addEventListener("click", refreshAll);
document.querySelector("#search-form").addEventListener("submit", event => {
  event.preventDefault();
  searchTrains();
});
document.querySelector("#load-orders").addEventListener("click", loadOrders);
elements.loginForm.addEventListener("submit", event => {
  event.preventDefault();
  login();
});
elements.logoutButton.addEventListener("click", logout);

init();

async function init() {
  applyCaptureMode();
  renderAuthState();
  elements.travelDate.value = new Date().toISOString().slice(0, 10);
  await checkHealth();
  await loadStations();
  await refreshAll();
  scrollToInitialHash();
}

async function login() {
  try {
    const auth = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: elements.loginUsername.value,
        password: elements.loginPassword.value,
      }),
    }, false);
    state.auth = auth;
    localStorage.setItem("railway-auth", JSON.stringify(auth));
    renderAuthState();
    showToast("登录成功，当前角色：" + roleText(auth.role));
    await refreshAll();
  } catch (error) {
    showToast(error.message || "登录失败");
  }
}

function logout() {
  state.auth = null;
  localStorage.removeItem("railway-auth");
  renderAuthState();
  showToast("已退出登录");
  refreshAll();
}

function renderAuthState() {
  if (state.auth && state.auth.token) {
    document.body.dataset.authenticated = "true";
    elements.authRole.textContent = roleText(state.auth.role);
    elements.authUser.textContent = state.auth.displayName || state.auth.username;
    return;
  }
  document.body.dataset.authenticated = "false";
  elements.authRole.textContent = "未登录";
  elements.authUser.textContent = "访客模式";
}

async function checkHealth() {
  try {
    const health = await request("/health");
    elements.apiStatus.className = "status-dot ok";
    elements.apiStatusText.textContent = `${health.service} 已连接`;
  } catch (error) {
    elements.apiStatus.className = "status-dot fail";
    elements.apiStatusText.textContent = "后端未连接";
    showToast("请先启动 Spring Boot 后端，再刷新页面");
  }
}

async function loadStations() {
  try {
    state.stations = await request("/stations");
    renderStationOptions();
  } catch (error) {
    state.stations = [
      { code: "BJP", name: "北京南" },
      { code: "SHH", name: "上海虹桥" },
      { code: "GZQ", name: "广州南" },
      { code: "WHN", name: "武汉" },
    ];
    renderStationOptions();
  }
}

function renderStationOptions() {
  const options = state.stations
    .map(station => `<option value="${station.code}">${station.name}</option>`)
    .join("");
  elements.fromStation.innerHTML = options;
  elements.toStation.innerHTML = options;
  elements.fromStation.value = "BJP";
  elements.toStation.value = "SHH";
}

async function refreshAll() {
  await Promise.all([
    loadDashboard(),
    searchTrains(),
    loadOrders(),
    loadRisks(),
    loadLogs(),
  ]);
}

async function loadDashboard() {
  try {
    const summary = await request("/dashboard/summary");
    elements.totalOrders.textContent = summary.totalOrders;
    elements.paidOrders.textContent = summary.paidOrders;
    elements.refundedOrders.textContent = summary.refundedOrders;
    elements.openRisks.textContent = summary.openRiskEvents;
    renderPopularTrains(summary.popularTrains || []);
  } catch (error) {
    renderPopularTrains([]);
  }
}

function renderPopularTrains(items) {
  if (items.length === 0) {
    elements.popularTrains.innerHTML = emptyItem("暂无订单聚合数据");
    return;
  }
  elements.popularTrains.innerHTML = items
    .map(item => `
      <div class="event-item">
        <strong>${item.trainNo}</strong>
        <span>订单数量：${item.orderCount}</span>
      </div>
    `)
    .join("");
}

async function searchTrains() {
  const from = elements.fromStation.value;
  const to = elements.toStation.value;
  const date = elements.travelDate.value;
  if (!from || !to || !date) {
    return;
  }

  try {
    const trains = await request(`/trains/search?from=${from}&to=${to}&date=${date}`);
    renderTrains(trains);
  } catch (error) {
    elements.trainResults.innerHTML = tableEmpty(7, "无法获取车次数据");
  }
}

function renderTrains(trains) {
  if (trains.length === 0) {
    elements.trainResults.innerHTML = tableEmpty(7, "当前条件没有可售车次");
    return;
  }
  elements.trainResults.innerHTML = trains
    .map(train => `
      <tr>
        <td>${train.trainNo}</td>
        <td>${train.departureStation} -> ${train.arrivalStation}</td>
        <td>${formatTime(train.departureTime)} - ${formatTime(train.arrivalTime)}</td>
        <td>${seatTypeText(train.seatType)}</td>
        <td>${train.remainingSeats}</td>
        <td>¥${train.price}</td>
        <td><button class="secondary-button" type="button" data-buy="${train.trainId}" data-inventory="${train.inventoryId}">购票</button></td>
      </tr>
    `)
    .join("");

  document.querySelectorAll("[data-buy]").forEach(button => {
    button.addEventListener("click", () => createOrder(button.dataset.buy, button.dataset.inventory));
  });
}

async function createOrder(trainId, inventoryId) {
  const userId = Number(elements.orderUserId.value || 1001);
  const passengerName = window.prompt("请输入乘客姓名", "张三");
  if (!passengerName) {
    return;
  }
  const passengerIdCard = window.prompt("请输入证件号", "110101200001010011");
  if (!passengerIdCard) {
    return;
  }

  try {
    await request("/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId,
        requestId: generateRequestId(),
        trainId: Number(trainId),
        inventoryId: Number(inventoryId),
        passengerName,
        passengerIdCard,
      }),
    });
    showToast("购票成功，已完成余票扣减和风控校验");
    await refreshAll();
  } catch (error) {
    showToast(error.message || "购票失败");
  }
}

async function loadOrders() {
  const userId = elements.orderUserId.value;
  const path = userId ? `/orders?userId=${userId}` : "/orders";
  try {
    const orders = await request(path);
    renderOrders(orders);
  } catch (error) {
    elements.orderResults.innerHTML = tableEmpty(8, "无法获取订单数据");
  }
}

function renderOrders(orders) {
  if (orders.length === 0) {
    elements.orderResults.innerHTML = tableEmpty(8, "暂无订单");
    return;
  }

  elements.orderResults.innerHTML = orders
    .map(order => `
      <tr>
        <td>${order.orderNo}</td>
        <td>${order.userId}</td>
        <td>${order.trainNo}</td>
        <td>${order.passengerName}</td>
        <td>${order.travelDate}</td>
        <td>¥${order.amount}</td>
        <td><span class="status ${order.status === "REFUNDED" ? "refunded" : ""}">${statusText(order.status)}</span></td>
        <td>${order.status === "PAID" ? `<button class="danger-button" type="button" data-refund="${order.id}">退票</button>` : "-"}</td>
      </tr>
    `)
    .join("");

  document.querySelectorAll("[data-refund]").forEach(button => {
    button.addEventListener("click", () => refundOrder(button.dataset.refund));
  });
}

async function refundOrder(orderId) {
  try {
    await request(`/orders/${orderId}/refund`, { method: "POST" });
    showToast("退票成功，库存已释放");
    await refreshAll();
  } catch (error) {
    showToast(error.message || "退票失败");
  }
}

async function loadRisks() {
  try {
    const risks = await request("/risks");
    if (risks.length === 0) {
      elements.riskList.innerHTML = emptyItem("暂无风险事件");
      return;
    }
    elements.riskList.innerHTML = risks
      .map(risk => `
        <div class="event-item">
          <div class="event-header">
            <strong>${riskLevelText(risk.riskLevel)} · ${riskTypeText(risk.riskType)}</strong>
            <span class="handled-pill ${risk.handled ? "done" : "open"}">${risk.handled ? "已处理" : "待处理"}</span>
          </div>
          <span>用户：${risk.userId} / 订单：${risk.orderNo || "-"}</span>
          <span>${risk.reason}</span>
          <div class="risk-actions">
            ${risk.handled || !canHandleRisk() ? "" : `<button class="secondary-button compact-button" type="button" data-handle-risk="${risk.id}">标记已处理</button>`}
          </div>
        </div>
      `)
      .join("");

    document.querySelectorAll("[data-handle-risk]").forEach(button => {
      button.addEventListener("click", () => handleRisk(button.dataset.handleRisk));
    });
  } catch (error) {
    elements.riskList.innerHTML = emptyItem("无法获取风险事件");
  }
}

async function handleRisk(riskId) {
  try {
    await request(`/risks/${riskId}/handle`, { method: "POST" });
    showToast("风险事件已处理");
    await refreshAll();
  } catch (error) {
    showToast(error.message || "处理风险事件失败");
  }
}

async function loadLogs() {
  try {
    const logs = await request("/logs");
    if (logs.length === 0) {
      elements.logList.innerHTML = emptyItem("暂无审计日志");
      return;
    }
    elements.logList.innerHTML = logs
      .map(log => `
        <div class="event-item">
          <strong>${log.action} / ${log.targetType}</strong>
          <span>${log.operator} · ${log.createdAt}</span>
          <span>${log.detail}</span>
        </div>
      `)
      .join("");
  } catch (error) {
    elements.logList.innerHTML = emptyItem(error.message || "无法获取审计日志");
  }
}

async function request(path, options = {}, withAuth = true) {
  const requestOptions = { ...options };
  requestOptions.headers = { ...(options.headers || {}) };
  if (withAuth && state.auth && state.auth.token) {
    requestOptions.headers.Authorization = `Bearer ${state.auth.token}`;
  }
  const response = await fetch(`${API_BASE}${path}`, requestOptions);
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message || "请求失败");
  }
  return data;
}

function loadStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem("railway-auth"));
  } catch (error) {
    return null;
  }
}

function canHandleRisk() {
  return state.auth && ["ADMIN", "RISK_OFFICER"].includes(state.auth.role);
}

function tableEmpty(colspan, message) {
  return `<tr><td colspan="${colspan}">${message}</td></tr>`;
}

function emptyItem(message) {
  return `<div class="event-item"><span>${message}</span></div>`;
}

function formatTime(value) {
  return String(value || "").slice(0, 5);
}

function seatTypeText(value) {
  const map = {
    SECOND_CLASS: "二等座",
    FIRST_CLASS: "一等座",
    BUSINESS_CLASS: "商务座",
  };
  return map[value] || value;
}

function statusText(value) {
  const map = {
    PAID: "已支付",
    REFUNDED: "已退票",
    CANCELLED: "已取消",
  };
  return map[value] || value;
}

function riskTypeText(value) {
  const map = {
    RAPID_PURCHASE: "短时多次购票",
    FREQUENT_REFUND: "频繁退票",
    HIGH_AMOUNT: "高金额订单",
  };
  return map[value] || value;
}

function riskLevelText(value) {
  const map = {
    LOW: "低风险",
    MEDIUM: "中风险",
    HIGH: "高风险",
  };
  return map[value] || value;
}

function roleText(value) {
  const map = {
    ADMIN: "系统管理员",
    OPERATOR: "运营人员",
    RISK_OFFICER: "风控专员",
  };
  return map[value] || value;
}

function generateRequestId() {
  if (window.crypto && window.crypto.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `REQ-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add("show");
  window.setTimeout(() => {
    elements.toast.classList.remove("show");
  }, 2600);
}

function scrollToInitialHash() {
  if (!window.location.hash) {
    return;
  }
  const target = document.querySelector(window.location.hash);
  if (target) {
    target.scrollIntoView({ block: "start" });
  }
}

function applyCaptureMode() {
  const capture = new URLSearchParams(window.location.search).get("capture");
  if (["dashboard", "orders", "risks"].includes(capture)) {
    document.body.dataset.capture = capture;
  }
}
