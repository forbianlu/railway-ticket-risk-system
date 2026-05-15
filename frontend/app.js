const API_BASE = "http://localhost:8080/api";

const state = {
  stations: [],
  auth: loadStoredAuth(),
  orderPage: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
    first: true,
    last: true,
  },
  paymentPage: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
    first: true,
    last: true,
  },
};

const elements = {
  apiStatus: document.querySelector("#api-status"),
  apiStatusText: document.querySelector("#api-status-text"),
  totalOrders: document.querySelector("#metric-total-orders"),
  pendingOrders: document.querySelector("#metric-pending-orders"),
  paidOrders: document.querySelector("#metric-paid-orders"),
  closedOrders: document.querySelector("#metric-closed-orders"),
  refundedOrders: document.querySelector("#metric-refunded-orders"),
  refundRate: document.querySelector("#metric-refund-rate"),
  riskRate: document.querySelector("#metric-risk-rate"),
  openRisks: document.querySelector("#metric-open-risks"),
  popularTrains: document.querySelector("#popular-trains"),
  fromStation: document.querySelector("#from-station"),
  toStation: document.querySelector("#to-station"),
  travelDate: document.querySelector("#travel-date"),
  trainResults: document.querySelector("#train-results"),
  orderResults: document.querySelector("#order-results"),
  orderUserId: document.querySelector("#order-user-id"),
  orderStatus: document.querySelector("#order-status"),
  orderNo: document.querySelector("#order-no"),
  orderFromDate: document.querySelector("#order-from-date"),
  orderToDate: document.querySelector("#order-to-date"),
  orderPageInfo: document.querySelector("#order-page-info"),
  prevOrders: document.querySelector("#prev-orders"),
  nextOrders: document.querySelector("#next-orders"),
  paymentOrderId: document.querySelector("#payment-order-id"),
  paymentStatus: document.querySelector("#payment-status"),
  paymentNoFilter: document.querySelector("#payment-no-filter"),
  paymentResults: document.querySelector("#payment-results"),
  paymentPageInfo: document.querySelector("#payment-page-info"),
  prevPayments: document.querySelector("#prev-payments"),
  nextPayments: document.querySelector("#next-payments"),
  riskStatus: document.querySelector("#risk-status"),
  riskScene: document.querySelector("#risk-scene"),
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
document.querySelector("#reset-orders").addEventListener("click", resetOrderFilters);
elements.prevOrders.addEventListener("click", () => changeOrderPage(-1));
elements.nextOrders.addEventListener("click", () => changeOrderPage(1));
document.querySelector("#create-payment").addEventListener("click", createPaymentFromInput);
document.querySelector("#load-payments").addEventListener("click", loadPayments);
elements.prevPayments.addEventListener("click", () => changePaymentPage(-1));
elements.nextPayments.addEventListener("click", () => changePaymentPage(1));
document.querySelector("#load-risks").addEventListener("click", loadRisks);
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
    loadPayments(),
    loadRisks(),
    loadLogs(),
  ]);
}

async function loadDashboard() {
  try {
    const summary = await request("/dashboard/summary");
    elements.totalOrders.textContent = summary.totalOrderCount ?? summary.totalOrders;
    elements.pendingOrders.textContent = summary.pendingPaymentOrderCount ?? 0;
    elements.paidOrders.textContent = summary.paidOrderCount ?? summary.paidOrders;
    elements.closedOrders.textContent = summary.closedOrderCount ?? 0;
    elements.refundedOrders.textContent = summary.refundedOrderCount ?? summary.refundedOrders;
    elements.refundRate.textContent = formatPercent(summary.refundRate);
    elements.riskRate.textContent = formatPercent(summary.riskRate);
    elements.openRisks.textContent = summary.unhandledRiskCount ?? summary.openRiskEvents;
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
    showToast("订单已创建，库存已锁定，请在 15 分钟内支付");
    await refreshAll();
  } catch (error) {
    showToast(error.message || "购票失败");
  }
}

async function loadOrders() {
  state.orderPage.page = 0;
  await loadOrdersPage();
}

async function loadOrdersPage() {
  const path = buildOrderQueryPath();
  try {
    const page = await request(path);
    state.orderPage = {
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
      first: page.first,
      last: page.last,
    };
    renderOrders(page.content || []);
    renderOrderPagination();
  } catch (error) {
    elements.orderResults.innerHTML = tableEmpty(8, "无法获取订单数据");
    renderOrderPagination();
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
        <td><span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span></td>
        <td>${renderOrderActions(order)}</td>
      </tr>
    `)
    .join("");

  document.querySelectorAll("[data-pay]").forEach(button => {
    button.addEventListener("click", () => payOrder(button.dataset.pay));
  });
  document.querySelectorAll("[data-create-payment]").forEach(button => {
    button.addEventListener("click", () => createPayment(button.dataset.createPayment));
  });
  document.querySelectorAll("[data-close-order]").forEach(button => {
    button.addEventListener("click", () => closeOrder(button.dataset.closeOrder));
  });
  document.querySelectorAll("[data-refund]").forEach(button => {
    button.addEventListener("click", () => refundOrder(button.dataset.refund));
  });
}

function buildOrderQueryPath() {
  const params = new URLSearchParams();
  appendParam(params, "userId", elements.orderUserId.value);
  appendParam(params, "status", elements.orderStatus.value);
  appendParam(params, "orderNo", elements.orderNo.value);
  appendParam(params, "fromDate", elements.orderFromDate.value);
  appendParam(params, "toDate", elements.orderToDate.value);
  params.set("page", String(state.orderPage.page));
  params.set("size", String(state.orderPage.size));
  return `/orders?${params.toString()}`;
}

function appendParam(params, key, value) {
  const normalized = String(value || "").trim();
  if (normalized) {
    params.set(key, normalized);
  }
}

function renderOrderPagination() {
  const totalPages = Math.max(1, state.orderPage.totalPages || 0);
  const currentPage = Math.min((state.orderPage.page || 0) + 1, totalPages);
  elements.orderPageInfo.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${state.orderPage.totalElements || 0} 条`;
  elements.prevOrders.disabled = Boolean(state.orderPage.first);
  elements.nextOrders.disabled = Boolean(state.orderPage.last);
}

async function changeOrderPage(offset) {
  const nextPage = Math.max(0, state.orderPage.page + offset);
  if (nextPage === state.orderPage.page) {
    return;
  }
  state.orderPage.page = nextPage;
  await loadOrdersPage();
}

async function resetOrderFilters() {
  elements.orderUserId.value = "1001";
  elements.orderStatus.value = "";
  elements.orderNo.value = "";
  elements.orderFromDate.value = "";
  elements.orderToDate.value = "";
  state.orderPage.page = 0;
  await loadOrdersPage();
}

function renderOrderActions(order) {
  if (order.status === "PENDING_PAYMENT") {
    return `
      <div class="inline-actions">
        <button class="secondary-button compact-button" type="button" data-create-payment="${order.id}">流水</button>
        <button class="secondary-button compact-button" type="button" data-pay="${order.id}">支付</button>
        <button class="danger-button compact-button" type="button" data-close-order="${order.id}">关闭</button>
      </div>
    `;
  }
  if (order.status === "PAID") {
    return `<button class="danger-button" type="button" data-refund="${order.id}">退票</button>`;
  }
  return "-";
}

async function payOrder(orderId) {
  try {
    const order = await request(`/orders/${orderId}/pay`, { method: "POST" });
    showToast(order.status === "PAID" ? "支付成功，已触发风控校验" : "订单已超时关闭，库存已释放");
    await refreshAll();
  } catch (error) {
    showToast(error.message || "支付失败");
  }
}

async function closeOrder(orderId) {
  try {
    await request(`/orders/${orderId}/close`, { method: "POST" });
    showToast("订单已关闭，库存已释放");
    await refreshAll();
  } catch (error) {
    showToast(error.message || "关闭订单失败");
  }
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

async function createPaymentFromInput() {
  const orderId = elements.paymentOrderId.value;
  if (!orderId) {
    showToast("请输入待支付订单 ID");
    return;
  }
  await createPayment(orderId);
}

async function createPayment(orderId) {
  try {
    const payment = await request("/payments", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        orderId: Number(orderId),
        requestId: generatePaymentRequestId(orderId),
      }),
    });
    elements.paymentOrderId.value = payment.orderId;
    showToast("支付流水已创建：" + payment.paymentNo);
    state.paymentPage.page = 0;
    await loadPaymentsPage();
  } catch (error) {
    showToast(error.message || "创建支付流水失败");
  }
}

async function loadPayments() {
  state.paymentPage.page = 0;
  await loadPaymentsPage();
}

async function loadPaymentsPage() {
  try {
    const page = await request(buildPaymentQueryPath());
    state.paymentPage = {
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
      first: page.first,
      last: page.last,
    };
    renderPayments(page.content || []);
    renderPaymentPagination();
  } catch (error) {
    elements.paymentResults.innerHTML = tableEmpty(9, "无法获取支付流水");
    renderPaymentPagination();
  }
}

function buildPaymentQueryPath() {
  const params = new URLSearchParams();
  appendParam(params, "orderId", elements.paymentOrderId.value);
  appendParam(params, "status", elements.paymentStatus.value);
  appendParam(params, "paymentNo", elements.paymentNoFilter.value);
  params.set("page", String(state.paymentPage.page));
  params.set("size", String(state.paymentPage.size));
  return `/payments?${params.toString()}`;
}

function renderPayments(payments) {
  if (payments.length === 0) {
    elements.paymentResults.innerHTML = tableEmpty(9, "暂无支付流水");
    return;
  }
  elements.paymentResults.innerHTML = payments
    .map(payment => `
      <tr>
        <td>${payment.paymentNo}</td>
        <td>${payment.orderNo}<br><span class="muted-text">#${payment.orderId}</span></td>
        <td>${payment.userId}</td>
        <td>¥${payment.amount}</td>
        <td><span class="status ${paymentStatusClass(payment.status)}">${paymentStatusText(payment.status)}</span></td>
        <td>${payment.channel}</td>
        <td>${formatDateTime(payment.createdAt)}</td>
        <td>${formatDateTime(payment.paidAt) || "-"}</td>
        <td>${renderPaymentActions(payment)}</td>
      </tr>
    `)
    .join("");

  document.querySelectorAll("[data-payment-success]").forEach(button => {
    button.addEventListener("click", () => callbackPayment(button.dataset.paymentSuccess, true));
  });
  document.querySelectorAll("[data-payment-fail]").forEach(button => {
    button.addEventListener("click", () => callbackPayment(button.dataset.paymentFail, false));
  });
}

function renderPaymentActions(payment) {
  if (payment.status !== "PENDING") {
    return "-";
  }
  return `
    <div class="inline-actions">
      <button class="secondary-button compact-button" type="button" data-payment-success="${payment.paymentNo}">成功回调</button>
      <button class="danger-button compact-button" type="button" data-payment-fail="${payment.paymentNo}">失败回调</button>
    </div>
  `;
}

async function callbackPayment(paymentNo, success) {
  try {
    const payment = await request("/payments/callback", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        paymentNo,
        callbackRequestId: generateCallbackRequestId(paymentNo, success),
        success,
        message: success ? "mock payment success" : "mock payment failed",
      }),
    });
    showToast(success ? "支付成功回调已处理" : "支付失败回调已处理");
    elements.paymentOrderId.value = payment.orderId;
    await refreshAll();
  } catch (error) {
    showToast(error.message || "支付回调失败");
  }
}

function renderPaymentPagination() {
  const totalPages = Math.max(1, state.paymentPage.totalPages || 0);
  const currentPage = Math.min((state.paymentPage.page || 0) + 1, totalPages);
  elements.paymentPageInfo.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${state.paymentPage.totalElements || 0} 条`;
  elements.prevPayments.disabled = Boolean(state.paymentPage.first);
  elements.nextPayments.disabled = Boolean(state.paymentPage.last);
}

async function changePaymentPage(offset) {
  const nextPage = Math.max(0, state.paymentPage.page + offset);
  if (nextPage === state.paymentPage.page) {
    return;
  }
  state.paymentPage.page = nextPage;
  await loadPaymentsPage();
}

async function loadRisks() {
  try {
    const risks = await request(buildRiskQueryPath());
    if (risks.length === 0) {
      elements.riskList.innerHTML = emptyItem("暂无风险事件");
      return;
    }
    elements.riskList.innerHTML = risks
      .map(risk => `
        <div class="event-item">
          <div class="event-header">
            <strong>${riskLevelText(risk.riskLevel)} · ${riskTypeText(risk.riskType)}</strong>
            <span class="handled-pill ${riskStatusClass(risk.status)}">${riskStatusText(risk.status)}</span>
          </div>
          <span>用户：${risk.userId} / 订单：${risk.orderNo || "-"} / 场景：${riskSceneText(risk.scene)}</span>
          <span>${escapeHtml(risk.reason)}</span>
          <span>处理人：${risk.handledBy || "-"} / 处理时间：${formatDateTime(risk.handledAt) || "-"}</span>
          <span>备注：${risk.handleRemark ? escapeHtml(risk.handleRemark) : "-"}</span>
          <div class="risk-actions">
            ${renderRiskActionControls(risk)}
            <button class="secondary-button compact-button" type="button" data-risk-history="${risk.id}">处置历史</button>
          </div>
          <div id="risk-history-${risk.id}" class="risk-history"></div>
        </div>
      `)
      .join("");

    document.querySelectorAll("[data-handle-risk]").forEach(button => {
      button.addEventListener("click", () => handleRisk(button.dataset.handleRisk));
    });
    document.querySelectorAll("[data-risk-history]").forEach(button => {
      button.addEventListener("click", () => loadRiskHistory(button.dataset.riskHistory));
    });
  } catch (error) {
    elements.riskList.innerHTML = emptyItem("无法获取风险事件");
  }
}

function buildRiskQueryPath() {
  const params = new URLSearchParams();
  if (elements.riskStatus.value) {
    params.set("status", elements.riskStatus.value);
  }
  if (elements.riskScene.value) {
    params.set("scene", elements.riskScene.value);
  }
  const query = params.toString();
  return query ? `/risks?${query}` : "/risks";
}

function renderRiskActionControls(risk) {
  const options = riskTargetStatuses(risk.status);
  if (!canHandleRisk() || options.length === 0) {
    return "";
  }
  const optionHtml = options
    .map(status => `<option value="${status}">${riskStatusText(status)}</option>`)
    .join("");
  return `
    <select class="risk-status-select" data-risk-target-status="${risk.id}" aria-label="处置结果">
      ${optionHtml}
    </select>
    <input class="risk-remark-input" data-risk-remark="${risk.id}" type="text" maxlength="500" placeholder="处置备注">
    <button class="secondary-button compact-button" type="button" data-handle-risk="${risk.id}">提交处置</button>
  `;
}

function riskTargetStatuses(status) {
  if (status === "PENDING" || !status) {
    return ["CONFIRMED", "FALSE_POSITIVE", "CLOSED"];
  }
  if (status === "CONFIRMED" || status === "FALSE_POSITIVE") {
    return ["CLOSED"];
  }
  return [];
}

async function handleRisk(riskId) {
  try {
    const statusSelect = document.querySelector(`[data-risk-target-status="${riskId}"]`);
    const remarkInput = document.querySelector(`[data-risk-remark="${riskId}"]`);
    await request(`/risks/${riskId}/handle`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        status: statusSelect ? statusSelect.value : "CLOSED",
        remark: remarkInput ? remarkInput.value : "",
      }),
    });
    showToast("风险事件已处理");
    await refreshAll();
  } catch (error) {
    showToast(error.message || "处理风险事件失败");
  }
}

async function loadRiskHistory(riskId) {
  const container = document.querySelector(`#risk-history-${riskId}`);
  if (!container) {
    return;
  }
  try {
    const records = await request(`/risks/${riskId}/handle-records`);
    if (records.length === 0) {
      container.innerHTML = `<span class="muted-text">暂无处置历史</span>`;
      return;
    }
    container.innerHTML = records
      .map(record => `
        <div class="history-row">
          <strong>${riskStatusText(record.fromStatus)} → ${riskStatusText(record.toStatus)}</strong>
          <span>${record.operatorName || "-"} / ${formatDateTime(record.operatedAt) || "-"}</span>
          <span>${record.remark ? escapeHtml(record.remark) : "无备注"}</span>
        </div>
      `)
      .join("");
  } catch (error) {
    container.innerHTML = `<span class="muted-text">无法获取处置历史</span>`;
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

function formatDateTime(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "";
}

function formatPercent(value) {
  const number = Number(value || 0);
  return `${(number * 100).toFixed(1)}%`;
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
    PENDING_PAYMENT: "待支付",
    PAID: "已支付",
    REFUNDED: "已退票",
    CLOSED: "已关闭",
    CANCELLED: "已取消",
  };
  return map[value] || value;
}

function orderStatusClass(value) {
  const map = {
    PENDING_PAYMENT: "pending",
    REFUNDED: "refunded",
    CLOSED: "closed",
    CANCELLED: "closed",
  };
  return map[value] || "";
}

function paymentStatusText(value) {
  const map = {
    PENDING: "待支付",
    SUCCESS: "支付成功",
    FAILED: "支付失败",
  };
  return map[value] || value;
}

function paymentStatusClass(value) {
  const map = {
    PENDING: "pending",
    SUCCESS: "",
    FAILED: "closed",
  };
  return map[value] || "";
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

function riskStatusText(value) {
  const map = {
    PENDING: "待处理",
    CONFIRMED: "已确认风险",
    FALSE_POSITIVE: "误报",
    CLOSED: "已关闭",
  };
  return map[value] || value || "待处理";
}

function riskStatusClass(value) {
  const map = {
    PENDING: "open",
    CONFIRMED: "confirmed",
    FALSE_POSITIVE: "false-positive",
    CLOSED: "done",
  };
  return map[value] || "open";
}

function riskSceneText(value) {
  const map = {
    ORDER_CREATED: "支付成功",
    ORDER_REFUNDED: "退票后",
  };
  return map[value] || "-";
}

function roleText(value) {
  const map = {
    ADMIN: "系统管理员",
    OPERATOR: "运营人员",
    RISK_OFFICER: "风控专员",
  };
  return map[value] || value;
}

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function generateRequestId() {
  if (window.crypto && window.crypto.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `REQ-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function generatePaymentRequestId(orderId) {
  return `PAYREQ-${orderId}-${generateRequestId()}`;
}

function generateCallbackRequestId(paymentNo, success) {
  return `CALLBACK-${success ? "SUCCESS" : "FAILED"}-${paymentNo}-${generateRequestId()}`;
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
