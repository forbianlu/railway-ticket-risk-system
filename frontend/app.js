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
  ticketChangePage: {
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
  notificationPage: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
    first: true,
    last: true,
  },
  globalSearch: {
    loading: false,
    lastKeyword: "",
  },
  riskPage: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
    first: true,
    last: true,
  },
  trainByInventory: {},
  selectedTrain: null,
  logs: [],
  visibleLogCount: 10,
  openRiskHistoryId: null,
  navObserver: null,
  activeSectionId: "",
  authExpiredNotified: false,
};

const HOT_ROUTES = [
  { from: "BJP", to: "SHH", label: "G101 北京南 → 上海虹桥" },
  { from: "SHH", to: "BJP", label: "G102 上海虹桥 → 北京南" },
  { from: "GZQ", to: "WHN", label: "G606 广州南 → 武汉" },
  { from: "WHN", to: "CDD", label: "D2201 武汉 → 成都东" },
  { from: "HFG", to: "SHH", label: "D931 合肥南 → 上海虹桥" },
];

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
  showAvailableTrains: document.querySelector("#show-available-trains"),
  showHotRoutes: document.querySelector("#show-hot-routes"),
  hotRouteShortcuts: document.querySelector("#hot-route-shortcuts"),
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
  ticketChangeStatus: document.querySelector("#ticket-change-status"),
  ticketChangeNo: document.querySelector("#ticket-change-no"),
  ticketChangeUserId: document.querySelector("#ticket-change-user-id"),
  ticketChangeResults: document.querySelector("#ticket-change-results"),
  ticketChangePageInfo: document.querySelector("#ticket-change-page-info"),
  prevTicketChanges: document.querySelector("#prev-ticket-changes"),
  nextTicketChanges: document.querySelector("#next-ticket-changes"),
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
  globalSearchKeyword: document.querySelector("#global-search-keyword"),
  globalSearchTypes: document.querySelector("#global-search-types"),
  globalSearchLimit: document.querySelector("#global-search-limit"),
  globalSearchTrace: document.querySelector("#global-search-trace"),
  globalSearchInfo: document.querySelector("#global-search-info"),
  globalSearchResults: document.querySelector("#global-search-results"),
  notificationStatus: document.querySelector("#notification-status"),
  notificationType: document.querySelector("#notification-type"),
  notificationOrderNo: document.querySelector("#notification-order-no"),
  notificationUserId: document.querySelector("#notification-user-id"),
  notificationResults: document.querySelector("#notification-results"),
  notificationPageInfo: document.querySelector("#notification-page-info"),
  prevNotifications: document.querySelector("#prev-notifications"),
  nextNotifications: document.querySelector("#next-notifications"),
  notificationSummaryTotal: document.querySelector("#notification-summary-total"),
  notificationSummaryUnread: document.querySelector("#notification-summary-unread"),
  notificationSummaryRead: document.querySelector("#notification-summary-read"),
  notificationSummaryLatest: document.querySelector("#notification-summary-latest"),
  notificationTypeSummary: document.querySelector("#notification-type-summary"),
  notificationStatusSummary: document.querySelector("#notification-status-summary"),
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
  showMoreLogs: document.querySelector("#show-more-logs"),
  collapseLogs: document.querySelector("#collapse-logs"),
  logDisplayInfo: document.querySelector("#log-display-info"),
  authRole: document.querySelector("#auth-role"),
  authUser: document.querySelector("#auth-user"),
  loginForm: document.querySelector("#login-form"),
  loginUsername: document.querySelector("#login-username"),
  loginPassword: document.querySelector("#login-password"),
  logoutButton: document.querySelector("#logout-button"),
  buyModal: document.querySelector("#buy-modal"),
  buyModalClose: document.querySelector("#buy-modal-close"),
  buyModalCancel: document.querySelector("#buy-modal-cancel"),
  buyForm: document.querySelector("#buy-form"),
  buySummary: document.querySelector("#buy-summary"),
  buyPassengerName: document.querySelector("#buy-passenger-name"),
  buyPassengerIdCard: document.querySelector("#buy-passenger-id-card"),
  buyError: document.querySelector("#buy-error"),
  buyConfirm: document.querySelector("#buy-confirm"),
  toast: document.querySelector("#toast"),
};

document.querySelector("#refresh-dashboard").addEventListener("click", refreshAll);
document.querySelector("#search-form").addEventListener("submit", event => {
  event.preventDefault();
  searchTrains();
});
elements.showAvailableTrains.addEventListener("click", loadAvailableTrains);
elements.showHotRoutes.addEventListener("click", () => {
  revealHotRoutes();
  showToast("已显示热门线路快捷入口");
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
document.querySelector("#load-ticket-changes").addEventListener("click", loadTicketChanges);
document.querySelector("#reset-ticket-changes").addEventListener("click", resetTicketChangeFilters);
elements.prevTicketChanges.addEventListener("click", () => changeTicketChangePage(-1));
elements.nextTicketChanges.addEventListener("click", () => changeTicketChangePage(1));
document.querySelector("#load-outbox-events").addEventListener("click", loadOutboxEvents);
document.querySelector("#dispatch-outbox-events").addEventListener("click", dispatchOutboxEvents);
document.querySelector("#retry-failed-outbox-events").addEventListener("click", retryFailedOutboxEvents);
elements.prevOutbox.addEventListener("click", () => changeOutboxPage(-1));
elements.nextOutbox.addEventListener("click", () => changeOutboxPage(1));
document.querySelector("#run-global-search").addEventListener("click", runGlobalSearch);
document.querySelector("#clear-global-search").addEventListener("click", clearGlobalSearch);
elements.globalSearchKeyword.addEventListener("keydown", event => {
  if (event.key === "Enter") {
    event.preventDefault();
    runGlobalSearch();
  }
});
document.querySelector("#load-notifications").addEventListener("click", loadNotifications);
elements.prevNotifications.addEventListener("click", () => changeNotificationPage(-1));
elements.nextNotifications.addEventListener("click", () => changeNotificationPage(1));
document.querySelector("#load-risks").addEventListener("click", loadRisks);
document.querySelector("#reset-risks").addEventListener("click", resetRiskFilters);
elements.prevRisks.addEventListener("click", () => changeRiskPage(-1));
elements.nextRisks.addEventListener("click", () => changeRiskPage(1));
elements.showMoreLogs.addEventListener("click", showMoreLogs);
elements.collapseLogs.addEventListener("click", collapseLogs);
elements.loginForm.addEventListener("submit", event => {
  event.preventDefault();
  login();
});
elements.logoutButton.addEventListener("click", logout);
elements.buyForm.addEventListener("submit", event => {
  event.preventDefault();
  submitPurchase();
});
elements.buyModalCancel.addEventListener("click", closePurchaseModal);
elements.buyModalClose.addEventListener("click", closePurchaseModal);
elements.buyModal.addEventListener("click", event => {
  if (event.target === elements.buyModal) {
    closePurchaseModal();
  }
});
window.addEventListener("keydown", event => {
  if (event.key === "Escape" && elements.buyModal.classList.contains("show")) {
    closePurchaseModal();
  }
});
window.addEventListener("hashchange", updateActiveNav);

init();

async function init() {
  applyCaptureMode();
  setupNavigation();
  setupScrollSpy();
  setupDashboardDrilldowns();
  renderAuthState();
  elements.travelDate.value = new Date().toISOString().slice(0, 10);
  await checkHealth();
  await loadStations();
  renderHotRoutes(false);
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
    state.authExpiredNotified = false;
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
  state.authExpiredNotified = false;
  clearStoredAuth();
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
    const health = await request("/health", {}, false);
    elements.apiStatus.className = "status-dot ok";
    elements.apiStatusText.textContent = `${health.service} 已连接`;
  } catch (error) {
    elements.apiStatus.className = "status-dot fail";
    elements.apiStatusText.textContent = "后端未连接";
    showToast(error.message || "无法连接后端服务，请确认 Spring Boot 后端已启动");
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

function renderHotRoutes(expanded) {
  if (!elements.hotRouteShortcuts) {
    return;
  }
  elements.hotRouteShortcuts.classList.toggle("expanded", Boolean(expanded));
  elements.hotRouteShortcuts.innerHTML = HOT_ROUTES
    .map(route => `
      <button class="route-chip" type="button" data-hot-from="${route.from}" data-hot-to="${route.to}">
        ${escapeHtml(route.label)}
      </button>
    `)
    .join("");
  elements.hotRouteShortcuts.querySelectorAll("[data-hot-from]").forEach(button => {
    button.addEventListener("click", () => applyHotRoute(button.dataset.hotFrom, button.dataset.hotTo));
  });
}

function revealHotRoutes() {
  renderHotRoutes(true);
  elements.hotRouteShortcuts.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

async function applyHotRoute(from, to) {
  elements.fromStation.value = from;
  elements.toStation.value = to;
  if (!elements.travelDate.value) {
    elements.travelDate.value = new Date().toISOString().slice(0, 10);
  }
  await searchTrains();
  showToast("已切换到热门线路并完成查询");
}

async function refreshAll() {
  await Promise.all([
    loadDashboard(),
    loadSystemStats(),
    searchTrains(),
    loadOrders(),
    loadPayments(),
    loadRefunds(),
    loadTicketChanges(),
    loadOutboxSummary(),
    loadOutboxEvents(),
    loadNotificationSummary(),
    loadNotifications(),
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
  const maxCount = Math.max(...items.map(item => Number(item.orderCount || 0)), 1);
  elements.popularTrains.innerHTML = items
    .map(item => `
      <div class="event-item progress-row">
        <div class="event-header">
          <strong>${item.trainNo}</strong>
          <span class="status">${item.orderCount} 单</span>
        </div>
        <div class="bar-track" aria-hidden="true">
          <span style="width: ${Math.max(8, (Number(item.orderCount || 0) / maxCount) * 100)}%"></span>
        </div>
      </div>
    `)
    .join("");
}

async function searchTrains() {
  const from = elements.fromStation.value;
  const to = elements.toStation.value;
  const date = elements.travelDate.value;
  if (!from || !to || !date) {
    renderTrainEmpty("请选择出发站、到达站和乘车日期后查询，也可以直接查看全部可购车次。");
    return;
  }

  try {
    const trains = await request(`/trains/search?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&date=${encodeURIComponent(date)}`);
    renderTrains(trains);
  } catch (error) {
    renderTrainEmpty(error.message || "无法获取车次数据");
  }
}

async function loadAvailableTrains() {
  try {
    const params = new URLSearchParams();
    if (elements.travelDate.value) {
      params.set("travelDate", elements.travelDate.value);
    }
    params.set("page", "0");
    params.set("size", "80");
    const trains = await request(`/trains/available?${params.toString()}`);
    renderTrains(trains, "全部可购车次");
    showToast("已加载全部可购车次");
  } catch (error) {
    renderTrainEmpty(error.message || "无法获取可购车次");
  }
}

function renderTrains(trains) {
  if (trains.length === 0) {
    renderTrainEmpty("当前线路暂无可售车次，可查看全部可购车次或选择热门线路。");
    return;
  }
  state.trainByInventory = {};
  trains.forEach(train => {
    state.trainByInventory[String(train.inventoryId)] = train;
  });
  elements.trainResults.innerHTML = trains
    .map(train => `
      <tr>
        <td><strong class="train-no">${train.trainNo}</strong></td>
        <td><span class="route">${train.departureStation} -> ${train.arrivalStation}</span></td>
        <td>${formatTime(train.departureTime)} - ${formatTime(train.arrivalTime)}</td>
        <td>${seatTypeText(train.seatType)}</td>
        <td><span class="${Number(train.remainingSeats || 0) <= 5 ? "inventory-low" : "inventory-ok"}">${train.remainingSeats}</span></td>
        <td><span class="money">¥${train.price}</span></td>
        <td><button class="secondary-button" type="button" data-buy="${train.trainId}" data-inventory="${train.inventoryId}">购票</button></td>
      </tr>
    `)
    .join("");

  document.querySelectorAll("[data-buy]").forEach(button => {
    button.addEventListener("click", () => openPurchaseModal(button.dataset.inventory));
  });
}

function renderTrainEmpty(message) {
  elements.trainResults.innerHTML = `
    <tr>
      <td colspan="7">
        <div class="empty-action">
          <strong>${escapeHtml(message)}</strong>
          <span>建议查看全部可购车次，或使用热门线路快捷入口快速定位有库存的车次。</span>
          <div class="inline-actions">
            <button class="secondary-button compact-button" type="button" data-empty-action="available">查看全部可购车次</button>
            <button class="ghost-button compact-button" type="button" data-empty-action="hot-routes">查看热门线路</button>
          </div>
        </div>
      </td>
    </tr>
  `;
  const availableButton = elements.trainResults.querySelector('[data-empty-action="available"]');
  const hotRoutesButton = elements.trainResults.querySelector('[data-empty-action="hot-routes"]');
  availableButton.addEventListener("click", loadAvailableTrains);
  hotRoutesButton.addEventListener("click", revealHotRoutes);
}

function openPurchaseModal(inventoryId) {
  const train = state.trainByInventory[String(inventoryId)];
  if (!train) {
    showToast("未找到当前车次库存，请重新查询");
    return;
  }
  state.selectedTrain = train;
  elements.buySummary.innerHTML = `
    <div class="buy-route">
      <strong>${escapeHtml(train.trainNo)}</strong>
      <span>${escapeHtml(train.departureStation)} → ${escapeHtml(train.arrivalStation)}</span>
    </div>
    <div class="buy-detail-grid">
      <div><span>乘车日期</span><strong>${formatDate(train.travelDate)}</strong></div>
      <div><span>发车时间</span><strong>${formatTime(train.departureTime)}</strong></div>
      <div><span>到达时间</span><strong>${formatTime(train.arrivalTime)}</strong></div>
      <div><span>席别</span><strong>${seatTypeText(train.seatType)}</strong></div>
      <div><span>票价</span><strong class="money">¥${train.price}</strong></div>
      <div><span>剩余票数</span><strong>${train.remainingSeats}</strong></div>
    </div>
  `;
  elements.buyPassengerName.value = "";
  elements.buyPassengerIdCard.value = "";
  elements.buyError.textContent = "";
  elements.buyConfirm.disabled = false;
  elements.buyConfirm.textContent = "确认下单";
  elements.buyModal.classList.add("show");
  elements.buyModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("modal-open");
  window.setTimeout(() => elements.buyPassengerName.focus(), 80);
}

function closePurchaseModal() {
  elements.buyModal.classList.remove("show");
  elements.buyModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("modal-open");
  state.selectedTrain = null;
}

async function submitPurchase() {
  const train = state.selectedTrain;
  if (!train) {
    return;
  }
  const passengerName = elements.buyPassengerName.value.trim();
  const passengerIdCard = elements.buyPassengerIdCard.value.trim();
  if (!passengerName || !passengerIdCard) {
    elements.buyError.textContent = "请完整填写乘客姓名和证件号";
    return;
  }

  elements.buyConfirm.disabled = true;
  elements.buyConfirm.textContent = "下单中...";
  elements.buyError.textContent = "";

  try {
    await request("/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId: Number(elements.orderUserId.value || 1001),
        requestId: generateRequestId(),
        trainId: Number(train.trainId),
        inventoryId: Number(train.inventoryId),
        passengerName,
        passengerIdCard,
      }),
    });
    closePurchaseModal();
    showToast("订单已创建，库存已锁定，请在 15 分钟内支付");
    await Promise.all([loadDashboard(), searchTrains(), loadOrders(), loadPayments()]);
  } catch (error) {
    elements.buyError.textContent = error.message || "购票失败，请稍后重试";
    elements.buyConfirm.disabled = false;
    elements.buyConfirm.textContent = "确认下单";
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
        <td><strong>${order.orderNo}</strong></td>
        <td>${order.userId}</td>
        <td><span class="train-no">${order.trainNo}</span></td>
        <td>${order.passengerName}</td>
        <td>${order.travelDate}</td>
        <td><span class="money">¥${order.amount}</span></td>
        <td><span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span></td>
        <td>${renderOrderActions(order)}</td>
      </tr>
    `)
    .join("");
  decorateAdminOrderDetailButtons(orders);

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

function decorateAdminOrderDetailButtons(orders) {
  const rows = elements.orderResults.querySelectorAll("tr");
  rows.forEach((row, index) => {
    const order = orders[index];
    const actions = row.querySelector("td:last-child .inline-actions") || row.querySelector("td:last-child");
    if (!order || !actions || actions.querySelector("[data-admin-order-detail]")) {
      return;
    }
    const button = document.createElement("button");
    button.className = "secondary-button compact-button";
    button.type = "button";
    button.textContent = "详情";
    button.dataset.adminOrderDetail = String(order.id);
    button.addEventListener("click", () => openAdminOrderDetail(order.id));
    actions.insertBefore(button, actions.firstChild);
  });
}

async function openAdminOrderDetail(orderId) {
  try {
    const detail = await request(`/orders/${orderId}/detail`);
    showAdminOrderDetail(detail);
  } catch (error) {
    showToast(error.message || "无法加载订单详情");
  }
}

function showAdminOrderDetail(detail) {
  const modal = ensureAdminDetailModal();
  const order = detail.order || {};
  const ticket = detail.ticket;
  const payments = detail.payments || [];
  const refunds = detail.refunds || [];
  const ticketChanges = detail.ticketChanges || [];
  const notifications = detail.notifications || [];
  const risks = detail.risks || [];
  const outboxEvents = detail.outboxEvents || [];
  const logs = detail.operationLogs || [];
  modal.querySelector(".order-detail-body").innerHTML = `
    <div class="detail-hero">
      <div>
        <p class="eyebrow">Admin Order Detail</p>
        <h2>${escapeHtml(order.orderNo || "-")}</h2>
        <span>User ${order.userId || "-"} · ${escapeHtml(order.trainNo || "-")} · ${formatDate(order.travelDate)}</span>
      </div>
      <div class="detail-amount">
        <strong>¥${formatAmount(order.amount)}</strong>
        <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
      </div>
    </div>
    <section class="detail-section">
      <h3>电子票 / 行程单</h3>
      ${ticket ? renderTicketDetail(ticket) : `<div class="detail-empty">暂无电子票。</div>`}
    </section>
    <section class="detail-section">
      <h3>支付与退款</h3>
      <div class="detail-two-column">
        <div>${renderDetailRecords(payments, payment => `
          <div class="detail-record"><strong>${escapeHtml(payment.paymentNo)}</strong><span>${paymentStatusText(payment.status)} · ¥${formatAmount(payment.amount)}</span></div>
        `, "暂无支付记录")}</div>
        <div>${renderDetailRecords(refunds, refund => `
          <div class="detail-record"><strong>${escapeHtml(refund.refundNo)}</strong><span>${refundStatusText(refund.status)} · ¥${formatAmount(refund.amount)}</span></div>
        `, "暂无退款记录")}</div>
      </div>
    </section>
    <section class="detail-section">
      <h3>改签链路</h3>
      ${renderDetailRecords(ticketChanges, change => `
        <div class="detail-record ticket-change-detail-record">
          <strong>${escapeHtml(change.changeNo)}</strong>
          <span>${escapeHtml(change.originalTrainNo || "-")} → ${escapeHtml(change.newTrainNo || "-")} · ${changeStatusText(change.status)} · ${formatSignedAmount(change.priceDifference)}</span>
        </div>
      `, "暂无改签记录")}
    </section>
    <section class="detail-section">
      <h3>风险与事件链路</h3>
      <div class="detail-two-column">
        <div>${renderDetailRecords(risks, risk => `
          <div class="detail-record"><strong>${riskTypeText(risk.riskType)} / ${riskStatusText(risk.status)}</strong><span>${escapeHtml(risk.reason || "-")}</span></div>
        `, "暂无风险事件")}</div>
        <div>${renderDetailRecords(outboxEvents, event => `
          <div class="detail-record"><strong>${escapeHtml(event.eventType)}</strong><span>${outboxStatusText(event.status)} · ${formatDateTime(event.createdAt) || "-"}</span></div>
        `, "暂无 Outbox 事件")}</div>
      </div>
    </section>
    <section class="detail-section">
      <h3>操作日志</h3>
      ${renderDetailRecords(logs, log => `
        <div class="detail-record"><strong>${escapeHtml(log.action)}</strong><span>${escapeHtml(log.operator)} · ${formatDateTime(log.createdAt) || "-"} · ${escapeHtml(log.detail || "")}</span></div>
      `, "暂无订单操作日志")}
    </section>
  `;
  modal.querySelector(".order-detail-body").insertAdjacentHTML("afterbegin", `
    <section class="detail-section transaction-chain-section">
      <h3>交易链路排查</h3>
      ${renderAdminTransactionChain(order, ticket, payments, refunds, ticketChanges, notifications, risks, outboxEvents, logs)}
    </section>
  `);
  openDetailModal(modal);
}

function renderAdminTransactionChain(order, ticket, payments, refunds, ticketChanges, notifications, risks, outboxEvents, logs) {
  const nodes = [{
    type: "ORDER",
    title: "订单创建",
    status: statusText(order.status),
    time: order.createdAt,
    detail: order.orderNo || "-",
  }];
  if (ticket) {
    nodes.push({
      type: "TICKET",
      title: "电子票",
      status: ticketStatusText(ticket.status),
      time: ticket.issuedAt || ticket.createdAt,
      detail: ticket.ticketNo || "-",
    });
  }
  payments.forEach(payment => nodes.push({
    type: "PAYMENT",
    title: "支付流水",
    status: paymentStatusText(payment.status),
    time: payment.paidAt || payment.createdAt,
    detail: `${payment.paymentNo || "-"} / ${formatAmount(payment.amount)}`,
  }));
  refunds.forEach(refund => nodes.push({
    type: "REFUND",
    title: "退款流水",
    status: refundStatusText(refund.status),
    time: refund.refundedAt || refund.createdAt,
    detail: `${refund.refundNo || "-"} / ${formatAmount(refund.amount)}`,
  }));
  ticketChanges.forEach(change => nodes.push({
    type: "CHANGE",
    title: "改签记录",
    status: changeStatusText(change.status),
    time: change.completedAt || change.updatedAt || change.createdAt,
    detail: `${change.changeNo || "-"} / ${change.originalTrainNo || "-"} -> ${change.newTrainNo || "-"}`,
  }));
  notifications.forEach(notification => nodes.push({
    type: "NOTICE",
    title: "通知",
    status: notificationStatusText(notification.status),
    time: notification.createdAt,
    detail: notification.title || notification.notificationNo || "-",
  }));
  risks.forEach(risk => nodes.push({
    type: "RISK",
    title: "风险事件",
    status: riskStatusText(risk.status),
    time: risk.createdAt,
    detail: `${riskTypeText(risk.riskType)} / ${risk.reason || "-"}`,
  }));
  outboxEvents.slice(0, 8).forEach(event => nodes.push({
    type: "OUTBOX",
    title: "Outbox",
    status: outboxStatusText(event.status),
    time: event.processedAt || event.createdAt,
    detail: event.eventType || "-",
  }));
  logs.slice(0, 8).forEach(log => nodes.push({
    type: "LOG",
    title: "审计日志",
    status: log.operator || "-",
    time: log.createdAt,
    detail: log.action || "-",
  }));
  nodes.sort((left, right) => {
    const leftTime = left.time ? new Date(left.time).getTime() : Number.MAX_SAFE_INTEGER;
    const rightTime = right.time ? new Date(right.time).getTime() : Number.MAX_SAFE_INTEGER;
    return leftTime - rightTime;
  });
  return `
    <div class="admin-chain-board">
      ${nodes.map(node => `
        <article class="admin-chain-node ${escapeHtml(node.type.toLowerCase())}">
          <span>${escapeHtml(node.type)}</span>
          <strong>${escapeHtml(node.title)}</strong>
          <small>${escapeHtml(node.status || "-")} / ${escapeHtml(node.detail || "-")}</small>
          <time>${formatDateTime(node.time) || "-"}</time>
        </article>
      `).join("")}
    </div>
  `;
}

function renderTicketDetail(ticket) {
  return `
    <div class="ticket-itinerary">
      <div><span>Ticket No</span><strong>${escapeHtml(ticket.ticketNo)}</strong></div>
      <div><span>Route</span><strong>${escapeHtml(ticket.departureStation)} → ${escapeHtml(ticket.arrivalStation)}</strong></div>
      <div><span>Time</span><strong>${formatTime(ticket.departureTime)} - ${formatTime(ticket.arrivalTime)}</strong></div>
      <div><span>Passenger</span><strong>${escapeHtml(ticket.passengerName)} / ${idTypeText(ticket.passengerIdType)} / ${escapeHtml(ticket.passengerIdCardMasked)}</strong></div>
      <div><span>Phone</span><strong>${escapeHtml(ticket.passengerPhoneMasked || "-")}</strong></div>
      <div><span>Status</span><strong>${escapeHtml(ticket.status)}</strong></div>
      <div><span>Issued At</span><strong>${formatDateTime(ticket.issuedAt) || "-"}</strong></div>
    </div>
  `;
}

function renderDetailRecords(records, mapper, emptyText) {
  if (!records.length) {
    return `<div class="detail-empty">${escapeHtml(emptyText)}</div>`;
  }
  return records.map(mapper).join("");
}

function ensureAdminDetailModal() {
  let modal = document.querySelector("#admin-order-detail-modal");
  if (modal) {
    return modal;
  }
  modal = document.createElement("div");
  modal.id = "admin-order-detail-modal";
  modal.className = "modal-backdrop order-detail-modal";
  modal.setAttribute("aria-hidden", "true");
  modal.innerHTML = `
    <div class="purchase-modal order-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="admin-order-detail-title">
      <button class="modal-close" type="button" aria-label="关闭订单详情"></button>
      <div class="modal-head">
        <p class="eyebrow">Operations Detail</p>
        <h2 id="admin-order-detail-title">订单完整链路</h2>
        <p>汇总订单、电子票、支付、退款、风险、Outbox 和日志。</p>
      </div>
      <div class="order-detail-body"></div>
    </div>
  `;
  document.body.appendChild(modal);
  modal.addEventListener("click", event => {
    if (event.target === modal) {
      closeDetailModal(modal);
    }
  });
  modal.querySelector(".modal-close").addEventListener("click", () => closeDetailModal(modal));
  return modal;
}

function openDetailModal(modal) {
  modal.classList.add("show");
  modal.setAttribute("aria-hidden", "false");
  document.body.classList.add("modal-open");
}

function closeDetailModal(modal) {
  modal.classList.remove("show");
  modal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("modal-open");
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
        <td><strong>${payment.paymentNo}</strong></td>
        <td>${payment.orderNo}<br><span class="muted-text">#${payment.orderId}</span></td>
        <td>${payment.userId}</td>
        <td><span class="money">¥${payment.amount}</span></td>
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
        <td><strong>${refund.refundNo}</strong></td>
        <td>${refund.orderNo}<br><span class="muted-text">#${refund.orderId}</span></td>
        <td>${refund.paymentNo || "-"}</td>
        <td>${refund.userId}</td>
        <td><span class="money">¥${refund.amount}</span></td>
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

async function loadTicketChanges() {
  state.ticketChangePage.page = 0;
  await loadTicketChangesPage();
}

async function loadTicketChangesPage() {
  try {
    const page = await request(buildTicketChangeQueryPath());
    state.ticketChangePage = {
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
      first: page.first,
      last: page.last,
    };
    renderTicketChanges(page.content || []);
    renderTicketChangePagination();
  } catch (error) {
    elements.ticketChangeResults.innerHTML = tableEmpty(9, error.message || "无法获取改签记录");
    renderTicketChangePagination();
  }
}

function buildTicketChangeQueryPath() {
  const params = new URLSearchParams();
  appendParam(params, "status", elements.ticketChangeStatus.value);
  appendParam(params, "changeNo", elements.ticketChangeNo.value);
  appendParam(params, "userId", elements.ticketChangeUserId.value);
  params.set("page", String(state.ticketChangePage.page));
  params.set("size", String(state.ticketChangePage.size));
  return `/ticket-changes?${params.toString()}`;
}

function renderTicketChanges(changes) {
  if (changes.length === 0) {
    elements.ticketChangeResults.innerHTML = tableEmpty(9, "暂无改签记录");
    return;
  }
  elements.ticketChangeResults.innerHTML = changes
    .map(change => `
      <tr>
        <td><strong>${escapeHtml(change.changeNo)}</strong></td>
        <td>${change.userId || "-"}</td>
        <td>${escapeHtml(change.originalOrderNo || "-")}<br><span class="muted-text">${escapeHtml(change.originalTrainNo || "-")} / ${escapeHtml(change.originalTicketNo || "-")}</span></td>
        <td>${escapeHtml(change.newOrderNo || "-")}<br><span class="muted-text">${escapeHtml(change.newTrainNo || "-")} / ${escapeHtml(change.newTicketNo || "-")}</span></td>
        <td><span class="money">${formatSignedAmount(change.priceDifference)}</span><br><span class="muted-text">新票 ¥${formatAmount(change.newAmount)}</span></td>
        <td><span class="status ${changeStatusClass(change.status)}">${changeStatusText(change.status)}</span></td>
        <td>${formatDateTime(change.createdAt) || "-"}</td>
        <td>${formatDateTime(change.completedAt) || "-"}</td>
        <td>${escapeHtml(change.failureReason || change.reason || "-")}</td>
      </tr>
    `)
    .join("");
}

function renderTicketChangePagination() {
  const totalPages = Math.max(1, state.ticketChangePage.totalPages || 0);
  const currentPage = Math.min((state.ticketChangePage.page || 0) + 1, totalPages);
  elements.ticketChangePageInfo.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${state.ticketChangePage.totalElements || 0} 条`;
  elements.prevTicketChanges.disabled = Boolean(state.ticketChangePage.first);
  elements.nextTicketChanges.disabled = Boolean(state.ticketChangePage.last);
}

async function changeTicketChangePage(offset) {
  const nextPage = Math.max(0, state.ticketChangePage.page + offset);
  if (nextPage === state.ticketChangePage.page) {
    return;
  }
  state.ticketChangePage.page = nextPage;
  await loadTicketChangesPage();
}

async function resetTicketChangeFilters() {
  elements.ticketChangeStatus.value = "";
  elements.ticketChangeNo.value = "";
  elements.ticketChangeUserId.value = "";
  state.ticketChangePage.page = 0;
  await loadTicketChangesPage();
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

async function runGlobalSearch() {
  const keyword = (elements.globalSearchKeyword.value || "").trim();
  if (keyword.length < 2) {
    elements.globalSearchInfo.textContent = "请输入至少 2 个字符进行查询";
    elements.globalSearchResults.innerHTML = emptyItem("关键词太短，请输入订单号、票号、流水号或乘车人等信息");
    return;
  }
  state.globalSearch.loading = true;
  state.globalSearch.lastKeyword = keyword;
  elements.globalSearchInfo.textContent = "综合查询中...";
  elements.globalSearchResults.innerHTML = `<div class="search-loading">正在检索订单、票据、流水、通知和事件链路...</div>`;
  try {
    const result = await request(buildGlobalSearchPath(keyword));
    renderGlobalSearch(result);
  } catch (error) {
    elements.globalSearchInfo.textContent = "综合查询失败";
    elements.globalSearchResults.innerHTML = emptyItem(error.message || "综合查询失败，请稍后重试");
  } finally {
    state.globalSearch.loading = false;
  }
}

function buildGlobalSearchPath(keyword) {
  const params = new URLSearchParams();
  params.set("keyword", keyword);
  const selectedTypes = Array.from(elements.globalSearchTypes.selectedOptions || [])
    .map(option => option.value)
    .filter(Boolean);
  if (selectedTypes.length > 0) {
    params.set("types", selectedTypes.join(","));
  }
  appendParam(params, "limitPerType", elements.globalSearchLimit.value);
  if (elements.globalSearchTrace.checked) {
    params.set("includeTrace", "true");
  }
  return `/search?${params.toString()}`;
}

function renderGlobalSearch(result) {
  const groups = result.groups || [];
  const totalCount = result.totalCount || 0;
  elements.globalSearchInfo.textContent = `关键词“${result.keyword || state.globalSearch.lastKeyword}”共命中 ${totalCount} 条记录`;
  if (totalCount === 0) {
    elements.globalSearchResults.innerHTML = emptyItem("未找到相关记录，请更换订单号、票号或流水号再试");
    return;
  }
  elements.globalSearchResults.innerHTML = groups
    .filter(group => (group.items || []).length > 0)
    .map(group => `
      <article class="search-result-group">
        <div class="search-group-head">
          <div>
            <p class="eyebrow">${escapeHtml(group.type || "")}</p>
            <h3>${escapeHtml(group.typeName || group.type || "结果")}</h3>
          </div>
          <span>${group.count || 0} 条</span>
        </div>
        <div class="search-result-list">
          ${(group.items || []).map(renderGlobalSearchItem).join("")}
        </div>
      </article>
    `)
    .join("");
  elements.globalSearchResults.querySelectorAll("[data-search-order-detail]").forEach(button => {
    button.addEventListener("click", () => openAdminOrderDetail(button.dataset.searchOrderDetail));
  });
}

function renderGlobalSearchItem(item) {
  const fields = (item.matchedFields || []).map(field => `<span>${escapeHtml(field)}</span>`).join("");
  const trace = (item.trace || []).length > 0
    ? `<div class="search-trace">${item.trace.map(text => `<span>${escapeHtml(text)}</span>`).join("")}</div>`
    : "";
  const action = item.orderId
    ? `<button class="secondary-button compact-button" type="button" data-search-order-detail="${escapeHtml(String(item.orderId))}">打开订单详情</button>`
    : `<span class="muted-text">暂无可打开链路</span>`;
  return `
    <div class="search-result-card">
      <div class="search-result-main">
        <div>
          <strong>${escapeHtml(item.title || "-")}</strong>
          <p>${escapeHtml(item.subtitle || "-")}</p>
        </div>
        <span class="status ${searchStatusClass(item.status)}">${escapeHtml(item.status || item.businessType || "-")}</span>
      </div>
      <div class="search-result-meta">
        <span>${escapeHtml(item.orderNo || item.ticketNo || item.paymentNo || item.refundNo || item.changeNo || item.notificationNo || item.businessId || "-")}</span>
        <span>${formatDateTime(item.createdAt) || "-"}</span>
      </div>
      <div class="search-matched-fields">${fields || "<span>related</span>"}</div>
      ${trace}
      <div class="search-result-actions">${action}</div>
    </div>
  `;
}

function clearGlobalSearch() {
  elements.globalSearchKeyword.value = "";
  Array.from(elements.globalSearchTypes.options || []).forEach(option => {
    option.selected = false;
  });
  elements.globalSearchLimit.value = "5";
  elements.globalSearchTrace.checked = false;
  elements.globalSearchInfo.textContent = "请输入至少 2 个字符进行查询";
  elements.globalSearchResults.innerHTML = "";
}

function searchStatusClass(value) {
  const status = String(value || "").toUpperCase();
  if (["SUCCESS", "DONE", "PAID", "ISSUED", "READ", "CLOSED"].includes(status)) {
    return "success";
  }
  if (["FAILED", "REFUND_FAILED", "CANCELLED"].includes(status)) {
    return "danger";
  }
  if (["PENDING", "PENDING_PAYMENT", "UNREAD", "PROCESSING"].includes(status)) {
    return "pending";
  }
  return "neutral";
}

async function loadNotifications() {
  state.notificationPage.page = 0;
  await Promise.all([loadNotificationSummary(), loadNotificationsPage()]);
}

async function loadNotificationSummary() {
  try {
    const summary = await request("/notifications/summary");
    renderNotificationSummary(summary);
  } catch (error) {
    renderNotificationSummary(null);
  }
}

async function loadNotificationsPage() {
  try {
    const page = await request(buildNotificationQueryPath());
    state.notificationPage = {
      page: page.page,
      size: page.size,
      totalPages: page.totalPages,
      totalElements: page.totalElements,
      first: page.first,
      last: page.last,
    };
    renderNotifications(page.content || []);
    renderNotificationPagination();
  } catch (error) {
    elements.notificationResults.innerHTML = tableEmpty(9, error.message || "无法获取通知数据");
    renderNotificationPagination();
  }
}

function buildNotificationQueryPath() {
  const params = new URLSearchParams();
  appendParam(params, "status", elements.notificationStatus.value);
  appendParam(params, "type", elements.notificationType.value);
  appendParam(params, "orderNo", elements.notificationOrderNo.value);
  appendParam(params, "userId", elements.notificationUserId.value);
  params.set("page", String(state.notificationPage.page));
  params.set("size", String(state.notificationPage.size));
  return `/notifications?${params.toString()}`;
}

function renderNotifications(notifications) {
  if (notifications.length === 0) {
    elements.notificationResults.innerHTML = tableEmpty(9, "暂无站内通知");
    return;
  }
  elements.notificationResults.innerHTML = notifications
    .map(notification => `
      <tr>
        <td><span class="muted-text">${escapeHtml(notification.notificationNo)}</span></td>
        <td>${notification.userId}</td>
        <td>${notificationTypeText(notification.type)}</td>
        <td><strong>${escapeHtml(notification.title)}</strong><br><span class="muted-text">${escapeHtml(notification.content)}</span></td>
        <td><span class="status ${notificationStatusClass(notification.status)}">${notificationStatusText(notification.status)}</span></td>
        <td>${escapeHtml(notification.businessType || "-")}<br><span class="muted-text">${escapeHtml(notification.businessId || "-")}</span></td>
        <td>${escapeHtml(notification.orderNo || "-")}</td>
        <td>${formatDateTime(notification.createdAt) || "-"}</td>
        <td>
          ${notification.orderId
            ? `<button class="secondary-button compact-button" type="button" data-notification-order-detail="${notification.orderId}">打开链路</button>`
            : `<span class="muted-text">${escapeHtml(notification.actionTarget || "-")}</span>`}
        </td>
      </tr>
    `)
    .join("");
  elements.notificationResults.querySelectorAll("[data-notification-order-detail]").forEach(button => {
    button.addEventListener("click", () => openAdminOrderDetail(button.dataset.notificationOrderDetail));
  });
}

function renderNotificationSummary(summary) {
  const data = summary || {
    totalCount: 0,
    unreadCount: 0,
    readCount: 0,
    countByType: {},
    countByStatus: {},
    latestCreatedAt: null,
  };
  elements.notificationSummaryTotal.textContent = data.totalCount || 0;
  elements.notificationSummaryUnread.textContent = data.unreadCount || 0;
  elements.notificationSummaryRead.textContent = data.readCount || 0;
  elements.notificationSummaryLatest.textContent = formatDateTime(data.latestCreatedAt) || "-";
  elements.notificationTypeSummary.innerHTML = renderSummaryMap(data.countByType || {}, notificationTypeText);
  elements.notificationStatusSummary.innerHTML = renderSummaryMap(data.countByStatus || {}, notificationStatusText);
}

function renderNotificationPagination() {
  const totalPages = Math.max(1, state.notificationPage.totalPages || 0);
  const currentPage = Math.min((state.notificationPage.page || 0) + 1, totalPages);
  elements.notificationPageInfo.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${state.notificationPage.totalElements || 0} 条`;
  elements.prevNotifications.disabled = Boolean(state.notificationPage.first);
  elements.nextNotifications.disabled = Boolean(state.notificationPage.last);
}

async function changeNotificationPage(offset) {
  const nextPage = Math.max(0, state.notificationPage.page + offset);
  if (nextPage === state.notificationPage.page) {
    return;
  }
  state.notificationPage.page = nextPage;
  await loadNotificationsPage();
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
    state.openRiskHistoryId = null;
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
      button.addEventListener("click", () => toggleRiskHistory(button.dataset.riskHistory));
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

async function toggleRiskHistory(riskId) {
  if (state.openRiskHistoryId === riskId) {
    closeRiskHistory(riskId);
    return;
  }
  if (state.openRiskHistoryId) {
    closeRiskHistory(state.openRiskHistoryId);
  }
  await loadRiskHistory(riskId);
}

function closeRiskHistory(riskId) {
  const container = document.querySelector(`#risk-history-${riskId}`);
  const button = document.querySelector(`[data-risk-history="${riskId}"]`);
  if (container) {
    container.innerHTML = "";
    container.classList.remove("open");
  }
  if (button) {
    button.textContent = "处置历史";
  }
  if (state.openRiskHistoryId === riskId) {
    state.openRiskHistoryId = null;
  }
}

async function loadRiskHistory(riskId) {
  const container = document.querySelector(`#risk-history-${riskId}`);
  const button = document.querySelector(`[data-risk-history="${riskId}"]`);
  if (!container) {
    return;
  }
  container.classList.add("open");
  container.innerHTML = `<span class="muted-text">历史加载中...</span>`;
  if (button) {
    button.textContent = "收起历史";
  }
  state.openRiskHistoryId = riskId;
  try {
    const records = await request(`/risks/${riskId}/handle-records`);
    if (records.length === 0) {
      container.innerHTML = `
        <button class="history-close" type="button" data-risk-history-close="${riskId}" aria-label="关闭处置历史"></button>
        <span class="muted-text">暂无处置历史</span>
      `;
      bindRiskHistoryClose(riskId);
      return;
    }
    container.innerHTML = `
      <button class="history-close" type="button" data-risk-history-close="${riskId}" aria-label="关闭处置历史"></button>
      ${records
        .map(record => `
          <div class="history-row">
            <strong>${riskStatusText(record.fromStatus)} → ${riskStatusText(record.toStatus)}</strong>
            <span>${record.operatorName || "-"} / ${formatDateTime(record.operatedAt) || "-"}</span>
            <span>${record.remark ? escapeHtml(record.remark) : "无备注"}</span>
          </div>
        `)
        .join("")}
    `;
    bindRiskHistoryClose(riskId);
  } catch (error) {
    container.innerHTML = `
      <button class="history-close" type="button" data-risk-history-close="${riskId}" aria-label="关闭处置历史"></button>
      <span class="muted-text">无法获取处置历史</span>
    `;
    bindRiskHistoryClose(riskId);
  }
}

function bindRiskHistoryClose(riskId) {
  const closeButton = document.querySelector(`[data-risk-history-close="${riskId}"]`);
  if (closeButton) {
    closeButton.addEventListener("click", () => closeRiskHistory(riskId));
  }
}

async function loadLogs() {
  try {
    state.logs = await request("/logs");
    state.visibleLogCount = 10;
    renderLogs();
  } catch (error) {
    elements.logList.innerHTML = emptyItem(error.message || "无法获取审计日志");
    elements.logDisplayInfo.textContent = "日志加载失败";
    elements.showMoreLogs.disabled = true;
    elements.collapseLogs.disabled = true;
  }
}

function renderLogs() {
  const logs = state.logs || [];
  if (logs.length === 0) {
    elements.logList.innerHTML = emptyItem("暂无审计日志");
    elements.logDisplayInfo.textContent = "暂无审计日志";
    elements.showMoreLogs.disabled = true;
    elements.collapseLogs.disabled = true;
    return;
  }
  const visibleLogs = logs.slice(0, state.visibleLogCount);
  elements.logList.innerHTML = visibleLogs
    .map(log => `
      <div class="event-item">
        <strong>${escapeHtml(log.action)} / ${escapeHtml(log.targetType)}</strong>
        <span>${escapeHtml(log.operator)} · ${formatDateTime(log.createdAt) || "-"}</span>
        <span>${escapeHtml(log.detail)}</span>
      </div>
    `)
    .join("");
  const visibleCount = Math.min(state.visibleLogCount, logs.length);
  elements.logDisplayInfo.textContent = `已显示 ${visibleCount} / ${logs.length} 条`;
  elements.showMoreLogs.disabled = visibleCount >= logs.length;
  elements.showMoreLogs.textContent = visibleCount >= logs.length ? "已全部显示" : "展开更多";
  elements.collapseLogs.disabled = visibleCount <= 10;
}

function showMoreLogs() {
  state.visibleLogCount = Math.min((state.visibleLogCount || 10) + 20, state.logs.length);
  renderLogs();
}

function collapseLogs() {
  state.visibleLogCount = 10;
  renderLogs();
  document.querySelector("#logs").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function request(path, options = {}, withAuth = true) {
  const requestOptions = { ...options };
  requestOptions.headers = { ...(options.headers || {}) };
  const hasAuthHeader = Boolean(withAuth && state.auth && state.auth.token);
  if (withAuth && state.auth && state.auth.token) {
    requestOptions.headers.Authorization = `Bearer ${state.auth.token}`;
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, requestOptions);
  } catch (error) {
    const networkError = new Error("无法连接后端服务，请确认 Spring Boot 后端已启动");
    networkError.status = 0;
    networkError.code = "NETWORK_ERROR";
    throw networkError;
  }
  const data = await readResponseBody(response);
  if (!response.ok) {
    if (response.status === 401) {
      if (hasAuthHeader) {
        handleAuthExpired();
        const authError = new Error("登录已过期，请重新登录");
        authError.status = 401;
        throw authError;
      }
      const unauthorizedError = new Error(data.message || "请先登录后再访问该功能");
      unauthorizedError.status = 401;
      throw unauthorizedError;
    }
    if (response.status === 403) {
      const forbiddenError = new Error("当前账号权限不足");
      forbiddenError.status = 403;
      throw forbiddenError;
    }
    if (response.status === 429) {
      const rateLimitError = new Error("请求过于频繁，请稍后再试");
      rateLimitError.status = 429;
      throw rateLimitError;
    }
    const requestError = new Error(data.message || `请求失败（${response.status}）`);
    requestError.status = response.status;
    throw requestError;
  }
  return data;
}

async function readResponseBody(response) {
  const text = await response.text();
  if (!text) {
    return {};
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    return { message: text };
  }
}

function loadStoredAuth() {
  try {
    const stored = localStorage.getItem("railway-auth") || sessionStorage.getItem("railway-auth");
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    return null;
  }
}

function clearStoredAuth() {
  localStorage.removeItem("railway-auth");
  localStorage.removeItem("railway-token");
  localStorage.removeItem("token");
  localStorage.removeItem("auth");
  sessionStorage.removeItem("railway-auth");
  sessionStorage.removeItem("railway-token");
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("auth");
}

function handleAuthExpired() {
  clearStoredAuth();
  state.auth = null;
  renderAuthState();
  if (!state.authExpiredNotified) {
    state.authExpiredNotified = true;
    showToast("登录已过期，请重新登录");
  }
  const loginScreen = document.querySelector(".login-screen");
  if (loginScreen) {
    loginScreen.scrollIntoView({ behavior: "smooth", block: "start" });
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

function formatDate(value) {
  return value ? String(value).slice(0, 10) : "-";
}

function formatDateTime(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "";
}

function formatAmount(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number.toFixed(2) : String(value || "-");
}

function formatSignedAmount(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number) || number === 0) {
    return "¥0.00";
  }
  return `${number > 0 ? "+" : "-"}¥${formatAmount(Math.abs(number))}`;
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

function idTypeText(value) {
  const map = {
    ID_CARD: "居民身份证",
    PASSPORT: "护照",
    OTHER: "其他证件",
  };
  return map[value] || value || "-";
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

function changeStatusText(value) {
  const map = {
    PENDING_PAYMENT: "待补差支付",
    SUCCESS: "改签成功",
    FAILED: "改签失败",
    CANCELLED: "已取消",
  };
  return map[value] || value || "-";
}

function changeStatusClass(value) {
  const map = {
    PENDING_PAYMENT: "pending",
    SUCCESS: "",
    FAILED: "closed",
    CANCELLED: "closed",
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

function notificationTypeText(value) {
  const map = {
    ORDER_CREATED: "下单提醒",
    PAYMENT_SUCCEEDED: "支付提醒",
    TICKET_ISSUED: "出票提醒",
    ORDER_CLOSED: "关闭提醒",
    ORDER_REFUNDED: "退票提醒",
    REFUND_SUCCEEDED: "退款成功",
    REFUND_FAILED: "退款失败",
    TICKET_CHANGE_CREATED: "改签提醒",
    TICKET_CHANGE_PENDING_PAYMENT: "改签待支付",
    TICKET_CHANGE_SUCCEEDED: "改签成功",
    TICKET_CHANGE_FAILED: "改签失败",
    RISK_ALERT: "风险提醒",
  };
  return map[value] || value || "-";
}

function notificationStatusText(value) {
  const map = {
    UNREAD: "未读",
    READ: "已读",
  };
  return map[value] || value || "-";
}

function notificationStatusClass(value) {
  const map = {
    UNREAD: "pending",
    READ: "",
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

function setupDashboardDrilldowns() {
  bindMetricCard(elements.totalOrders, () => openOrdersByStatus("", "全部订单"));
  bindMetricCard(elements.pendingOrders, () => openOrdersByStatus("PENDING_PAYMENT", "待支付订单"));
  bindMetricCard(elements.paidOrders, () => openOrdersByStatus("PAID", "已支付订单"));
  bindMetricCard(elements.closedOrders, () => openOrdersByStatus("CLOSED", "已关闭订单"));
  bindMetricCard(elements.refundedOrders, () => openOrdersByStatus("REFUNDED", "退票订单"));
  bindMetricCard(elements.refundRate, () => openRefunds("", "退款流水"));
  bindMetricCard(elements.riskRate, () => openRisksByStatus("", "风险运营报表"));
  bindMetricCard(elements.openRisks, () => openRisksByStatus("PENDING", "未处理风险"));
  bindMetricCard(elements.outboxSummaryPending, () => openOutboxByStatus("PENDING", "待处理事件"));
  bindMetricCard(elements.outboxSummaryProcessing, () => openOutboxByStatus("PROCESSING", "处理中事件"));
  bindMetricCard(elements.outboxSummaryFailed, () => openOutboxByStatus("FAILED", "失败事件"));
  bindMetricCard(elements.outboxSummaryBacklog, () => openOutboxByStatus("", "Outbox 积压概览"));
}

function bindMetricCard(valueElement, handler) {
  if (!valueElement) {
    return;
  }
  const card = valueElement.closest(".metric");
  if (!card) {
    return;
  }
  card.classList.add("clickable-card");
  card.setAttribute("role", "button");
  card.setAttribute("tabindex", "0");
  card.addEventListener("click", handler);
  card.addEventListener("keydown", event => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handler();
    }
  });
}

async function openOrdersByStatus(status, label) {
  elements.orderUserId.value = "";
  elements.orderStatus.value = status;
  elements.orderNo.value = "";
  elements.orderFromDate.value = "";
  elements.orderToDate.value = "";
  state.orderPage.page = 0;
  navigateToSection("orders");
  await loadOrdersPage();
  showToast(`已切换到${label}`);
}

async function openRisksByStatus(status, label) {
  elements.riskStatus.value = status;
  elements.riskScene.value = "";
  elements.riskUserId.value = "";
  elements.riskOrderNo.value = "";
  elements.riskFromDate.value = "";
  elements.riskToDate.value = "";
  state.riskPage.page = 0;
  navigateToSection("risks");
  await Promise.all([loadRisksPage(), loadRiskSummary()]);
  showToast(`已切换到${label}`);
}

async function openRefunds(status, label) {
  elements.refundStatus.value = status;
  elements.refundNoFilter.value = "";
  elements.refundOrderId.value = "";
  state.refundPage.page = 0;
  navigateToSection("refunds");
  await loadRefundsPage();
  showToast(`已切换到${label}`);
}

async function openOutboxByStatus(status, label) {
  elements.outboxStatus.value = status;
  elements.outboxEventType.value = "";
  state.outboxPage.page = 0;
  navigateToSection("outbox");
  await Promise.all([loadOutboxSummary(), loadOutboxEventsPage()]);
  showToast(`已切换到${label}`);
}

function navigateToSection(sectionId) {
  const section = document.querySelector(`#${sectionId}`);
  if (!section) {
    return;
  }
  if (window.location.hash !== `#${sectionId}`) {
    window.location.hash = sectionId;
  }
  section.scrollIntoView({ behavior: "smooth", block: "start" });
  section.classList.remove("section-highlight");
  window.setTimeout(() => section.classList.add("section-highlight"), 20);
  window.setTimeout(() => section.classList.remove("section-highlight"), 1400);
  setActiveNav(sectionId);
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

function setupNavigation() {
  document.querySelectorAll(".nav a").forEach(link => {
    link.addEventListener("click", () => {
      const sectionId = link.getAttribute("href").replace("#", "");
      setActiveNav(sectionId);
    });
  });
  updateActiveNav();
}

function setupScrollSpy() {
  const sections = Array.from(document.querySelectorAll(".main > .section"));
  if (!("IntersectionObserver" in window)) {
    window.addEventListener("scroll", () => {
      const current = sections
        .map(section => ({ id: section.id, top: Math.abs(section.getBoundingClientRect().top) }))
        .sort((left, right) => left.top - right.top)[0];
      if (current) {
        setActiveNav(current.id);
      }
    }, { passive: true });
    return;
  }
  if (state.navObserver) {
    state.navObserver.disconnect();
  }
  state.navObserver = new IntersectionObserver(entries => {
    const visible = entries
      .filter(entry => entry.isIntersecting)
      .sort((left, right) => left.boundingClientRect.top - right.boundingClientRect.top);
    if (visible.length > 0) {
      setActiveNav(visible[0].target.id);
    }
  }, {
    root: null,
    rootMargin: "-18% 0px -58% 0px",
    threshold: [0.08, 0.2, 0.45],
  });
  sections.forEach(section => state.navObserver.observe(section));
}

function updateActiveNav() {
  const sectionId = (window.location.hash || "#dashboard").replace("#", "");
  setActiveNav(sectionId);
}

function setActiveNav(sectionId) {
  if (!sectionId || state.activeSectionId === sectionId) {
    return;
  }
  state.activeSectionId = sectionId;
  document.querySelectorAll(".nav a").forEach(link => {
    link.classList.toggle("active", link.getAttribute("href") === `#${sectionId}`);
  });
}
