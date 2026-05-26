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
  refundPage: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
    first: true,
    last: true,
  },
  outboxPage: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
    first: true,
    last: true,
  },
  riskPage: {
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
  cacheMode: document.querySelector("#cache-mode"),
  cacheTtl: document.querySelector("#cache-ttl"),
  cacheHitCount: document.querySelector("#cache-hit-count"),
  cacheMissCount: document.querySelector("#cache-miss-count"),
  cacheEvictCount: document.querySelector("#cache-evict-count"),
  cacheFallback: document.querySelector("#cache-fallback"),
  rateLimitMode: document.querySelector("#rate-limit-mode"),
  rateLimitBlocked: document.querySelector("#rate-limit-blocked"),
  rateLimitRules: document.querySelector("#rate-limit-rules"),
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
  refundOrderId: document.querySelector("#refund-order-id"),
  refundStatus: document.querySelector("#refund-status"),
  refundNoFilter: document.querySelector("#refund-no-filter"),
  refundResults: document.querySelector("#refund-results"),
  refundPageInfo: document.querySelector("#refund-page-info"),
  prevRefunds: document.querySelector("#prev-refunds"),
  nextRefunds: document.querySelector("#next-refunds"),
  outboxStatus: document.querySelector("#outbox-status"),
  outboxEventType: document.querySelector("#outbox-event-type"),
  outboxResults: document.querySelector("#outbox-results"),
  outboxPageInfo: document.querySelector("#outbox-page-info"),
  prevOutbox: document.querySelector("#prev-outbox"),
  nextOutbox: document.querySelector("#next-outbox"),
  outboxSummaryTotal: document.querySelector("#outbox-summary-total"),
  outboxSummaryPending: document.querySelector("#outbox-summary-pending"),
  outboxSummaryProcessing: document.querySelector("#outbox-summary-processing"),
  outboxSummaryDone: document.querySelector("#outbox-summary-done"),
  outboxSummaryFailed: document.querySelector("#outbox-summary-failed"),
  outboxSummaryFailureRate: document.querySelector("#outbox-summary-failure-rate"),
  outboxSummaryBacklog: document.querySelector("#outbox-summary-backlog"),
  outboxTypeSummary: document.querySelector("#outbox-type-summary"),
  outboxStatusSummary: document.querySelector("#outbox-status-summary"),
  riskStatus: document.querySelector("#risk-status"),
  riskScene: document.querySelector("#risk-scene"),
  riskUserId: document.querySelector("#risk-user-id"),
  riskOrderNo: document.querySelector("#risk-order-no"),
  riskFromDate: document.querySelector("#risk-from-date"),
  riskToDate: document.querySelector("#risk-to-date"),
  riskPageInfo: document.querySelector("#risk-page-info"),
  prevRisks: document.querySelector("#prev-risks"),
  nextRisks: document.querySelector("#next-risks"),
  riskSummaryTotal: document.querySelector("#risk-summary-total"),
  riskSummaryPending: document.querySelector("#risk-summary-pending"),
  riskSummaryConfirmed: document.querySelector("#risk-summary-confirmed"),
  riskSummaryFalsePositive: document.querySelector("#risk-summary-false-positive"),
  riskSummaryClosed: document.querySelector("#risk-summary-closed"),
  riskSummaryCompletionRate: document.querySelector("#risk-summary-completion-rate"),
  riskSummaryFalsePositiveRate: document.querySelector("#risk-summary-false-positive-rate"),
  riskSummaryConfirmedRate: document.querySelector("#risk-summary-confirmed-rate"),
  riskStatusSummary: document.querySelector("#risk-status-summary"),
  riskSceneSummary: document.querySelector("#risk-scene-summary"),
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
document.querySelector("#load-refunds").addEventListener("click", loadRefunds);
document.querySelector("#reset-refunds").addEventListener("click", resetRefundFilters);
elements.prevRefunds.addEventListener("click", () => changeRefundPage(-1));
elements.nextRefunds.addEventListener("click", () => changeRefundPage(1));
document.querySelector("#load-outbox-events").addEventListener("click", loadOutboxEvents);
document.querySelector("#dispatch-outbox-events").addEventListener("click", dispatchOutboxEvents);
document.querySelector("#retry-failed-outbox-events").addEventListener("click", retryFailedOutboxEvents);
elements.prevOutbox.addEventListener("click", () => changeOutboxPage(-1));
elements.nextOutbox.addEventListener("click", () => changeOutboxPage(1));
document.querySelector("#load-risks").addEventListener("click", loadRisks);
document.querySelector("#reset-risks").addEventListener("click", resetRiskFilters);
elements.prevRisks.addEventListener("click", () => changeRiskPage(-1));
elements.nextRisks.addEventListener("click", () => changeRiskPage(1));
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
    loadSystemStats(),
    searchTrains(),
    loadOrders(),
    loadPayments(),
    loadRefunds(),
    loadOutboxSummary(),
    loadOutboxEvents(),
    loadRisks(),
    loadRiskSummary(),
    loadLogs(),
  ]);
}

async function loadSystemStats() {
  await Promise.all([
    loadCacheStats(),
    loadRateLimitStats(),
  ]);
}

async function loadCacheStats() {
  try {
    const stats = await request("/cache/train-search");
    elements.cacheMode.textContent = stats.cacheMode || stats.configuredMode || "-";
    elements.cacheTtl.textContent = stats.ttlSeconds ? `${stats.ttlSeconds}s` : "-";
    elements.cacheHitCount.textContent = stats.hitCount ?? 0;
    elements.cacheMissCount.textContent = stats.missCount ?? 0;
    elements.cacheEvictCount.textContent = stats.evictCount ?? 0;
    elements.cacheFallback.textContent = stats.localFallback ? "是" : "否";
  } catch (error) {
    elements.cacheMode.textContent = "-";
    elements.cacheTtl.textContent = "-";
    elements.cacheFallback.textContent = "需登录";
  }
}

async function loadRateLimitStats() {
  try {
    const stats = await request("/rate-limit/summary");
    elements.rateLimitMode.textContent = stats.mode || stats.configuredMode || "-";
    elements.rateLimitBlocked.textContent = stats.blockedCount ?? 0;
    renderRateLimitRules(stats.rules || {});
  } catch (error) {
    elements.rateLimitMode.textContent = "-";
    elements.rateLimitBlocked.textContent = "需登录";
    elements.rateLimitRules.innerHTML = emptyItem("需登录后查看限流规则");
  }
}

function renderRateLimitRules(rules) {
  const entries = Object.entries(rules);
  if (entries.length === 0) {
    elements.rateLimitRules.innerHTML = emptyItem("暂无限流规则配置");
    return;
  }
  elements.rateLimitRules.innerHTML = entries
    .map(([name, rule]) => `
      <div class="event-item">
        <strong>${escapeHtml(name)}</strong>
        <span>${rule.limit ?? "-"} 次 / ${rule.windowSeconds ?? "-"} 秒</span>
      </div>
    `)
    .join("");
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
  if (!elements.popularTrains) {
    return;
  }
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
    elements.refundOrderId.value = orderId;
    showToast("退票成功，库存已释放，退款流水已创建");
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
    const payment = await request("/payments/callback/mock", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        paymentNo,
        callbackRequestId: generateCallbackRequestId(paymentNo, success),
        channelPaymentNo: success ? generateChannelPaymentNo(paymentNo) : "",
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

async function loadRefunds() {
  state.refundPage.page = 0;
  await loadRefundsPage();
}

async function loadRefundsPage() {
  try {
    const page = await request(buildRefundQueryPath());
    state.refundPage = {
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
      first: page.first,
      last: page.last,
    };
    renderRefunds(page.content || []);
    renderRefundPagination();
  } catch (error) {
    elements.refundResults.innerHTML = tableEmpty(10, "无法获取退款流水");
    renderRefundPagination();
  }
}

function buildRefundQueryPath() {
  const params = new URLSearchParams();
  appendParam(params, "orderId", elements.refundOrderId.value);
  appendParam(params, "status", elements.refundStatus.value);
  appendParam(params, "refundNo", elements.refundNoFilter.value);
  params.set("page", String(state.refundPage.page));
  params.set("size", String(state.refundPage.size));
  return `/refunds?${params.toString()}`;
}

function renderRefunds(refunds) {
  if (refunds.length === 0) {
    elements.refundResults.innerHTML = tableEmpty(10, "暂无退款流水");
    return;
  }
  elements.refundResults.innerHTML = refunds
    .map(refund => `
      <tr>
        <td>${refund.refundNo}</td>
        <td>${refund.orderNo}<br><span class="muted-text">#${refund.orderId}</span></td>
        <td>${refund.paymentNo || "-"}</td>
        <td>${refund.userId}</td>
        <td>¥${refund.amount}</td>
        <td><span class="status ${refundStatusClass(refund.status)}">${refundStatusText(refund.status)}</span></td>
        <td>${refund.channelRefundNo || "-"}</td>
        <td>${formatDateTime(refund.createdAt)}</td>
        <td>${formatDateTime(refund.refundedAt) || "-"}</td>
        <td>${renderRefundActions(refund)}</td>
      </tr>
    `)
    .join("");

  document.querySelectorAll("[data-refund-success]").forEach(button => {
    button.addEventListener("click", () => callbackRefund(button.dataset.refundSuccess, true));
  });
  document.querySelectorAll("[data-refund-fail]").forEach(button => {
    button.addEventListener("click", () => callbackRefund(button.dataset.refundFail, false));
  });
}

function renderRefundActions(refund) {
  if (refund.status !== "PENDING") {
    return "-";
  }
  return `
    <div class="inline-actions">
      <button class="secondary-button compact-button" type="button" data-refund-success="${refund.refundNo}">成功回调</button>
      <button class="danger-button compact-button" type="button" data-refund-fail="${refund.refundNo}">失败回调</button>
    </div>
  `;
}

async function callbackRefund(refundNo, success) {
  try {
    const refund = await request("/refunds/callback/mock", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        refundNo,
        callbackRequestId: generateRefundCallbackRequestId(refundNo, success),
        channelRefundNo: success ? generateChannelRefundNo(refundNo) : "",
        success,
        message: success ? "mock refund success" : "mock refund failed",
      }),
    });
    showToast(success ? "退款成功回调已处理" : "退款失败回调已处理");
    elements.refundOrderId.value = refund.orderId;
    await refreshAll();
  } catch (error) {
    showToast(error.message || "退款回调失败");
  }
}

function renderRefundPagination() {
  const totalPages = Math.max(1, state.refundPage.totalPages || 0);
  const currentPage = Math.min((state.refundPage.page || 0) + 1, totalPages);
  elements.refundPageInfo.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${state.refundPage.totalElements || 0} 条`;
  elements.prevRefunds.disabled = Boolean(state.refundPage.first);
  elements.nextRefunds.disabled = Boolean(state.refundPage.last);
}

async function changeRefundPage(offset) {
  const nextPage = Math.max(0, state.refundPage.page + offset);
  if (nextPage === state.refundPage.page) {
    return;
  }
  state.refundPage.page = nextPage;
  await loadRefundsPage();
}

async function resetRefundFilters() {
  elements.refundOrderId.value = "";
  elements.refundStatus.value = "";
  elements.refundNoFilter.value = "";
  state.refundPage.page = 0;
  await loadRefundsPage();
}

async function loadOutboxEvents() {
  state.outboxPage.page = 0;
  await loadOutboxSummary();
  await loadOutboxEventsPage();
}

async function loadOutboxSummary() {
  try {
    const summary = await request("/outbox-events/summary");
    renderOutboxSummary(summary);
  } catch (error) {
    renderOutboxSummary(null);
  }
}

async function loadOutboxEventsPage() {
  try {
    const page = await request(buildOutboxQueryPath());
    state.outboxPage = {
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
      first: page.first,
      last: page.last,
    };
    renderOutboxEvents(page.content || []);
    renderOutboxPagination();
  } catch (error) {
    elements.outboxResults.innerHTML = tableEmpty(9, error.message || "无法获取事件数据");
    renderOutboxPagination();
  }
}

function buildOutboxQueryPath() {
  const params = new URLSearchParams();
  appendParam(params, "status", elements.outboxStatus.value);
  appendParam(params, "eventType", elements.outboxEventType.value);
  params.set("page", String(state.outboxPage.page));
  params.set("size", String(state.outboxPage.size));
  return `/outbox-events?${params.toString()}`;
}

function renderOutboxEvents(events) {
  if (events.length === 0) {
    elements.outboxResults.innerHTML = tableEmpty(9, "暂无 Outbox 事件");
    return;
  }
  elements.outboxResults.innerHTML = events
    .map(event => `
      <tr>
        <td><span class="muted-text">${event.eventId}</span></td>
        <td>${event.eventType}</td>
        <td>${event.aggregateType}<br><span class="muted-text">${event.aggregateId}</span></td>
        <td><span class="status ${outboxStatusClass(event.status)}">${outboxStatusText(event.status)}</span></td>
        <td>${event.retryCount}/${event.maxRetryCount}</td>
        <td>${formatDateTime(event.createdAt)}</td>
        <td>${formatDateTime(event.processedAt) || "-"}</td>
        <td>${event.lastError ? escapeHtml(event.lastError) : "-"}</td>
        <td>${event.status === "FAILED" ? `<button class="secondary-button compact-button" data-outbox-retry="${event.id}" type="button">重试</button>` : "-"}</td>
      </tr>
    `)
    .join("");
  elements.outboxResults.querySelectorAll("[data-outbox-retry]").forEach(button => {
    button.addEventListener("click", () => retryOutboxEvent(button.dataset.outboxRetry));
  });
}

async function dispatchOutboxEvents() {
  try {
    const response = await request("/outbox-events/dispatch", { method: "POST" });
    showToast(`已派发 ${response.processedCount || 0} 个事件`);
    await loadOutboxEventsPage();
    await loadOutboxSummary();
    await loadLogs();
  } catch (error) {
    showToast(error.message || "事件派发失败");
  }
}

async function retryOutboxEvent(id) {
  try {
    await request(`/outbox-events/${id}/retry`, { method: "POST" });
    showToast("事件已重新入队");
    await loadOutboxEventsPage();
    await loadOutboxSummary();
  } catch (error) {
    showToast(error.message || "事件重试失败");
  }
}

async function retryFailedOutboxEvents() {
  try {
    const response = await request("/outbox-events/retry-failed", { method: "POST" });
    showToast(`已重新入队 ${response.enqueuedCount || 0} 个失败事件`);
    await loadOutboxEventsPage();
    await loadOutboxSummary();
  } catch (error) {
    showToast(error.message || "批量重试失败");
  }
}

function renderOutboxSummary(summary) {
  const empty = {
    totalCount: 0,
    pendingCount: 0,
    processingCount: 0,
    doneCount: 0,
    failedCount: 0,
    failureRate: 0,
    backlogCount: 0,
    eventCountByType: {},
    eventCountByStatus: {},
  };
  const data = summary || empty;
  elements.outboxSummaryTotal.textContent = data.totalCount || 0;
  elements.outboxSummaryPending.textContent = data.pendingCount || 0;
  elements.outboxSummaryProcessing.textContent = data.processingCount || 0;
  elements.outboxSummaryDone.textContent = data.doneCount || 0;
  elements.outboxSummaryFailed.textContent = data.failedCount || 0;
  elements.outboxSummaryFailureRate.textContent = formatPercent(data.failureRate || 0);
  elements.outboxSummaryBacklog.textContent = data.backlogCount || 0;
  elements.outboxTypeSummary.innerHTML = renderSummaryMap(data.eventCountByType || {});
  elements.outboxStatusSummary.innerHTML = renderSummaryMap(data.eventCountByStatus || {}, outboxStatusText);
}

function renderSummaryMap(map, labelFormatter = value => value) {
  const entries = Object.entries(map);
  if (entries.length === 0) {
    return `<span class="muted-text">暂无数据</span>`;
  }
  return entries
    .map(([key, value]) => `<span><strong>${escapeHtml(labelFormatter(key))}</strong>${value}</span>`)
    .join("");
}

function renderOutboxPagination() {
  const totalPages = Math.max(1, state.outboxPage.totalPages || 0);
  const currentPage = Math.min((state.outboxPage.page || 0) + 1, totalPages);
  elements.outboxPageInfo.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${state.outboxPage.totalElements || 0} 条`;
  elements.prevOutbox.disabled = Boolean(state.outboxPage.first);
  elements.nextOutbox.disabled = Boolean(state.outboxPage.last);
}

async function changeOutboxPage(offset) {
  const nextPage = Math.max(0, state.outboxPage.page + offset);
  if (nextPage === state.outboxPage.page) {
    return;
  }
  state.outboxPage.page = nextPage;
  await loadOutboxEventsPage();
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
  state.riskPage.page = 0;
  await loadRisksPage();
}

async function loadRisksPage() {
  try {
    const page = await request(buildRiskQueryPath());
    state.riskPage = {
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
      first: page.first,
      last: page.last,
    };
    const risks = page.content || [];
    if (risks.length === 0) {
      elements.riskList.innerHTML = emptyItem("暂无风险事件");
      renderRiskPagination();
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
    renderRiskPagination();
  } catch (error) {
    elements.riskList.innerHTML = emptyItem("无法获取风险事件");
    renderRiskPagination();
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
  if (elements.riskUserId.value) {
    params.set("userId", elements.riskUserId.value);
  }
  if (elements.riskOrderNo.value.trim()) {
    params.set("orderNo", elements.riskOrderNo.value.trim());
  }
  if (elements.riskFromDate.value) {
    params.set("fromDate", elements.riskFromDate.value);
  }
  if (elements.riskToDate.value) {
    params.set("toDate", elements.riskToDate.value);
  }
  params.set("page", state.riskPage.page);
  params.set("size", state.riskPage.size);
  const query = params.toString();
  return query ? `/risks?${query}` : "/risks";
}

function renderRiskPagination() {
  const totalPages = Math.max(1, state.riskPage.totalPages || 0);
  const currentPage = Math.min((state.riskPage.page || 0) + 1, totalPages);
  elements.riskPageInfo.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${state.riskPage.totalElements || 0} 条`;
  elements.prevRisks.disabled = Boolean(state.riskPage.first);
  elements.nextRisks.disabled = Boolean(state.riskPage.last);
}

async function changeRiskPage(offset) {
  const nextPage = Math.max(0, state.riskPage.page + offset);
  if (nextPage === state.riskPage.page) {
    return;
  }
  state.riskPage.page = nextPage;
  await loadRisksPage();
}

async function resetRiskFilters() {
  elements.riskStatus.value = "";
  elements.riskScene.value = "";
  elements.riskUserId.value = "";
  elements.riskOrderNo.value = "";
  elements.riskFromDate.value = "";
  elements.riskToDate.value = "";
  state.riskPage.page = 0;
  await loadRisksPage();
}

async function loadRiskSummary() {
  try {
    const summary = await request("/risks/summary");
    elements.riskSummaryTotal.textContent = summary.totalRiskCount || 0;
    elements.riskSummaryPending.textContent = summary.pendingRiskCount || 0;
    elements.riskSummaryConfirmed.textContent = summary.confirmedRiskCount || 0;
    elements.riskSummaryFalsePositive.textContent = summary.falsePositiveRiskCount || 0;
    elements.riskSummaryClosed.textContent = summary.closedRiskCount || 0;
    elements.riskSummaryCompletionRate.textContent = formatPercent(summary.handlingCompletionRate);
    elements.riskSummaryFalsePositiveRate.textContent = formatPercent(summary.falsePositiveRate);
    elements.riskSummaryConfirmedRate.textContent = formatPercent(summary.confirmedRate);
    renderRiskBreakdown(elements.riskStatusSummary, summary.riskCountByStatus || {}, riskStatusText);
    renderRiskBreakdown(elements.riskSceneSummary, summary.riskCountByScene || {}, riskSceneText);
  } catch (error) {
    elements.riskStatusSummary.innerHTML = emptyItem("无法获取状态统计");
    elements.riskSceneSummary.innerHTML = emptyItem("无法获取场景统计");
  }
}

function renderRiskBreakdown(container, data, labelFormatter) {
  const entries = Object.entries(data);
  if (entries.length === 0) {
    container.innerHTML = emptyItem("暂无统计");
    return;
  }
  container.innerHTML = entries
    .map(([key, value]) => `
      <div class="event-item compact-event">
        <strong>${labelFormatter(key)}</strong>
        <span>${value || 0} 个事件</span>
      </div>
    `)
    .join("");
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

function refundStatusText(value) {
  const map = {
    PENDING: "退款处理中",
    SUCCESS: "退款成功",
    FAILED: "退款失败",
  };
  return map[value] || value;
}

function refundStatusClass(value) {
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

function outboxStatusText(value) {
  const map = {
    PENDING: "待处理",
    PROCESSING: "处理中",
    DONE: "处理完成",
    FAILED: "处理失败",
  };
  return map[value] || value;
}

function outboxStatusClass(value) {
  const map = {
    PENDING: "pending",
    PROCESSING: "pending",
    DONE: "",
    FAILED: "closed",
  };
  return map[value] || "";
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

function generateRefundCallbackRequestId(refundNo, success) {
  return `REFUND-CALLBACK-${success ? "SUCCESS" : "FAILED"}-${refundNo}-${generateRequestId()}`;
}

function generateChannelPaymentNo(paymentNo) {
  return `CH-${paymentNo}-${Date.now()}`;
}

function generateChannelRefundNo(refundNo) {
  return `CH-${refundNo}-${Date.now()}`;
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
