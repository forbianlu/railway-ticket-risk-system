const API_BASE = "http://localhost:8080/api";
const PASSENGER_AUTH_KEY = "railway-passenger-auth";

const passengerState = {
  auth: loadPassengerAuth(),
  stations: [],
  trainByInventory: {},
  travelers: [],
  travelerById: {},
  selectedTrain: null,
  authExpiredNotified: false,
  navObserver: null,
  scrollTicking: false,
  lastScrollY: 0,
  trains: { items: [], page: 0, size: 5 },
  orders: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
  payments: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
  refunds: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
};

const PASSENGER_HOT_ROUTES = [
  { from: "BJP", to: "SHH", label: "北京南 → 上海虹桥" },
  { from: "SHH", to: "BJP", label: "上海虹桥 → 北京南" },
  { from: "GZQ", to: "WHN", label: "广州南 → 武汉" },
  { from: "WHN", to: "CDD", label: "武汉 → 成都东" },
  { from: "HFG", to: "SHH", label: "合肥南 → 上海虹桥" },
];

const elements = {
  loginForm: document.querySelector("#passenger-login-form"),
  loginUsername: document.querySelector("#passenger-login-username"),
  loginPassword: document.querySelector("#passenger-login-password"),
  apiStatus: document.querySelector("#passenger-api-status"),
  apiStatusText: document.querySelector("#passenger-api-status-text"),
  authRole: document.querySelector("#passenger-auth-role"),
  authUser: document.querySelector("#passenger-auth-user"),
  logoutButton: document.querySelector("#passenger-logout-button"),
  refreshPassenger: document.querySelector("#refresh-passenger"),
  metricTotal: document.querySelector("#passenger-metric-total"),
  metricPending: document.querySelector("#passenger-metric-pending"),
  metricPaid: document.querySelector("#passenger-metric-paid"),
  metricClosed: document.querySelector("#passenger-metric-closed"),
  metricRefunded: document.querySelector("#passenger-metric-refunded"),
  metricPayments: document.querySelector("#passenger-metric-payments"),
  metricRefunds: document.querySelector("#passenger-metric-refunds"),
  latestOrders: document.querySelector("#passenger-latest-orders"),
  upcomingTrips: document.querySelector("#passenger-upcoming-trips"),
  searchForm: document.querySelector("#passenger-search-form"),
  fromStation: document.querySelector("#passenger-from-station"),
  toStation: document.querySelector("#passenger-to-station"),
  travelDate: document.querySelector("#passenger-travel-date"),
  showAvailable: document.querySelector("#passenger-show-available"),
  showHotRoutes: document.querySelector("#passenger-show-hot-routes"),
  hotRoutes: document.querySelector("#passenger-hot-routes"),
  trainResults: document.querySelector("#passenger-train-results"),
  refreshTravelers: document.querySelector("#passenger-refresh-travelers"),
  travelerForm: document.querySelector("#passenger-traveler-form"),
  travelerId: document.querySelector("#passenger-traveler-id"),
  travelerName: document.querySelector("#passenger-traveler-name"),
  travelerIdType: document.querySelector("#passenger-traveler-id-type"),
  travelerIdNo: document.querySelector("#passenger-traveler-id-no"),
  travelerPhone: document.querySelector("#passenger-traveler-phone"),
  travelerDefault: document.querySelector("#passenger-traveler-default"),
  travelerError: document.querySelector("#passenger-traveler-error"),
  travelerSubmit: document.querySelector("#passenger-traveler-submit"),
  travelerCancel: document.querySelector("#passenger-traveler-cancel"),
  travelerList: document.querySelector("#passenger-traveler-list"),
  orderStatus: document.querySelector("#passenger-order-status"),
  loadOrders: document.querySelector("#passenger-load-orders"),
  resetOrders: document.querySelector("#passenger-reset-orders"),
  orderCards: document.querySelector("#passenger-order-cards"),
  orderPageInfo: document.querySelector("#passenger-order-page-info"),
  prevOrders: document.querySelector("#passenger-prev-orders"),
  nextOrders: document.querySelector("#passenger-next-orders"),
  paymentStatus: document.querySelector("#passenger-payment-status"),
  loadPayments: document.querySelector("#passenger-load-payments"),
  paymentResults: document.querySelector("#passenger-payment-results"),
  paymentPageInfo: document.querySelector("#passenger-payment-page-info"),
  prevPayments: document.querySelector("#passenger-prev-payments"),
  nextPayments: document.querySelector("#passenger-next-payments"),
  refundStatus: document.querySelector("#passenger-refund-status"),
  loadRefunds: document.querySelector("#passenger-load-refunds"),
  refundResults: document.querySelector("#passenger-refund-results"),
  refundPageInfo: document.querySelector("#passenger-refund-page-info"),
  prevRefunds: document.querySelector("#passenger-prev-refunds"),
  nextRefunds: document.querySelector("#passenger-next-refunds"),
  buyModal: document.querySelector("#passenger-buy-modal"),
  buyModalClose: document.querySelector("#passenger-buy-modal-close"),
  buyCancel: document.querySelector("#passenger-buy-cancel"),
  buyForm: document.querySelector("#passenger-buy-form"),
  buySummary: document.querySelector("#passenger-buy-summary"),
  buyTraveler: document.querySelector("#passenger-buy-traveler"),
  buyName: document.querySelector("#passenger-buy-name"),
  buyIdType: document.querySelector("#passenger-buy-id-type"),
  buyIdCard: document.querySelector("#passenger-buy-id-card"),
  buyPhone: document.querySelector("#passenger-buy-phone"),
  buyError: document.querySelector("#passenger-buy-error"),
  buyConfirm: document.querySelector("#passenger-buy-confirm"),
  toast: document.querySelector("#passenger-toast"),
};

elements.loginForm.addEventListener("submit", event => {
  event.preventDefault();
  loginPassenger();
});
elements.logoutButton.addEventListener("click", logoutPassenger);
elements.refreshPassenger.addEventListener("click", refreshPassengerData);
elements.searchForm.addEventListener("submit", event => {
  event.preventDefault();
  searchPassengerTrains();
});
elements.showAvailable.addEventListener("click", loadAvailablePassengerTrains);
elements.showHotRoutes.addEventListener("click", () => {
  renderHotRoutes(true);
  showToast("已展开热门线路");
});
elements.refreshTravelers.addEventListener("click", loadPassengerTravelers);
elements.travelerForm.addEventListener("submit", event => {
  event.preventDefault();
  savePassengerTraveler();
});
elements.travelerCancel.addEventListener("click", () => {
  resetTravelerForm();
  showToast("已取消编辑，表单已恢复为新增模式");
});
elements.buyTraveler.addEventListener("change", applySelectedTravelerToBuyForm);
elements.loadOrders.addEventListener("click", () => {
  passengerState.orders.page = 0;
  loadPassengerOrders();
});
elements.resetOrders.addEventListener("click", () => {
  elements.orderStatus.value = "";
  passengerState.orders.page = 0;
  loadPassengerOrders();
});
elements.prevOrders.addEventListener("click", () => changePassengerPage("orders", -1));
elements.nextOrders.addEventListener("click", () => changePassengerPage("orders", 1));
elements.loadPayments.addEventListener("click", () => {
  passengerState.payments.page = 0;
  loadPassengerPayments();
});
elements.prevPayments.addEventListener("click", () => changePassengerPage("payments", -1));
elements.nextPayments.addEventListener("click", () => changePassengerPage("payments", 1));
elements.loadRefunds.addEventListener("click", () => {
  passengerState.refunds.page = 0;
  loadPassengerRefunds();
});
elements.prevRefunds.addEventListener("click", () => changePassengerPage("refunds", -1));
elements.nextRefunds.addEventListener("click", () => changePassengerPage("refunds", 1));
elements.buyForm.addEventListener("submit", event => {
  event.preventDefault();
  submitPassengerOrder();
});
elements.buyCancel.addEventListener("click", closeBuyModal);
elements.buyModalClose.addEventListener("click", closeBuyModal);
elements.buyModal.addEventListener("click", event => {
  if (event.target === elements.buyModal) {
    closeBuyModal();
  }
});
window.addEventListener("keydown", event => {
  if (event.key === "Escape" && elements.buyModal.classList.contains("show")) {
    closeBuyModal();
  }
});

document.querySelectorAll("[data-jump-orders]").forEach(card => {
  card.addEventListener("click", () => {
    elements.orderStatus.value = card.dataset.jumpOrders || "";
    passengerState.orders.page = 0;
    activateSection("passenger-orders");
    loadPassengerOrders();
  });
});
document.querySelector("[data-jump-payments]").addEventListener("click", () => {
  activateSection("passenger-payments");
  loadPassengerPayments();
});
document.querySelector("[data-jump-refunds]").addEventListener("click", () => {
  activateSection("passenger-refunds");
  loadPassengerRefunds();
});

initPassenger();

async function initPassenger() {
  setupPassengerNavigation();
  setupPassengerScrollSpy();
  renderAuthState();
  elements.travelDate.value = new Date().toISOString().slice(0, 10);
  await checkHealth();
  await loadStations();
  renderHotRoutes(true);
  if (passengerState.auth && passengerState.auth.token) {
    await refreshPassengerData();
  } else {
    renderLoggedOutPlaceholders();
  }
}

async function loginPassenger() {
  try {
    const auth = await passengerRequest("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: elements.loginUsername.value.trim(),
        password: elements.loginPassword.value,
      }),
    }, false);
    if (auth.role !== "USER") {
      throw new Error("请使用乘客账号登录乘客端");
    }
    passengerState.auth = auth;
    passengerState.authExpiredNotified = false;
    localStorage.setItem(PASSENGER_AUTH_KEY, JSON.stringify(auth));
    renderAuthState();
    showToast("登录成功，欢迎使用乘客购票服务");
    await refreshPassengerData();
    await loadAvailablePassengerTrains();
  } catch (error) {
    showToast(error.message || "登录失败");
  }
}

function logoutPassenger() {
  passengerState.auth = null;
  localStorage.removeItem(PASSENGER_AUTH_KEY);
  sessionStorage.removeItem(PASSENGER_AUTH_KEY);
  renderAuthState();
  renderLoggedOutPlaceholders();
  showToast("已退出乘客端");
}

function renderAuthState() {
  const signedIn = Boolean(passengerState.auth && passengerState.auth.token);
  document.body.dataset.passengerAuthenticated = signedIn ? "true" : "false";
  elements.authRole.textContent = signedIn ? roleText(passengerState.auth.role) : "未登录";
  elements.authUser.textContent = signedIn ? (passengerState.auth.displayName || passengerState.auth.username) : "访客模式";
}

async function checkHealth() {
  try {
    const health = await passengerRequest("/health", {}, false);
    elements.apiStatus.className = "status-dot ok";
    elements.apiStatusText.textContent = `${health.service || "后端服务"} 已连接`;
  } catch (error) {
    elements.apiStatus.className = "status-dot fail";
    elements.apiStatusText.textContent = "后端未连接";
    showToast(error.message || "无法连接后端服务，请确认 Spring Boot 后端已启动");
  }
}

async function loadStations() {
  try {
    passengerState.stations = await passengerRequest("/stations", {}, false);
  } catch (error) {
    passengerState.stations = [
      { code: "BJP", name: "北京南" },
      { code: "SHH", name: "上海虹桥" },
      { code: "GZQ", name: "广州南" },
      { code: "WHN", name: "武汉" },
      { code: "CDD", name: "成都东" },
    ];
  }
  renderStationOptions();
}

function renderStationOptions() {
  const options = passengerState.stations
    .map(station => `<option value="${escapeHtml(station.code)}">${escapeHtml(station.name)}</option>`)
    .join("");
  elements.fromStation.innerHTML = options;
  elements.toStation.innerHTML = options;
  elements.fromStation.value = "BJP";
  elements.toStation.value = "SHH";
}

async function refreshPassengerData() {
  if (!ensureSignedIn()) {
    return;
  }
  await Promise.all([
    loadPassengerSummary(),
    loadPassengerTravelers(),
    loadPassengerOrders(),
    loadPassengerPayments(),
    loadPassengerRefunds(),
  ]);
}

async function loadPassengerSummary() {
  try {
    const summary = await passengerRequest("/passenger/summary");
    const totalOrders = Number(summary.pendingPaymentOrderCount || 0)
      + Number(summary.paidOrderCount || 0)
      + Number(summary.closedOrderCount || 0)
      + Number(summary.refundedOrderCount || 0);
    elements.metricTotal.textContent = totalOrders;
    elements.metricPending.textContent = summary.pendingPaymentOrderCount || 0;
    elements.metricPaid.textContent = summary.paidOrderCount || 0;
    elements.metricClosed.textContent = summary.closedOrderCount || 0;
    elements.metricRefunded.textContent = summary.refundedOrderCount || 0;
    elements.metricPayments.textContent = summary.paymentCount || 0;
    elements.metricRefunds.textContent = summary.refundCount || 0;
    renderMiniOrders(elements.latestOrders, summary.latestOrders || [], "暂无最近订单");
    renderMiniOrders(elements.upcomingTrips, summary.upcomingTrips || [], "暂无即将出行");
  } catch (error) {
    showToast(error.message || "无法加载乘客概览");
  }
}

function renderMiniOrders(container, orders, emptyText) {
  if (!orders.length) {
    container.innerHTML = emptyItem(emptyText);
    return;
  }
  container.innerHTML = orders.map(order => `
    <button class="event-item passenger-mini-order" type="button" data-order-status-jump="${escapeHtml(order.status)}">
      <div class="event-header">
        <strong>${escapeHtml(order.trainNo)} · ${formatDate(order.travelDate)}</strong>
        <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
      </div>
      <span>${escapeHtml(order.orderNo)} / ${seatTypeText(order.seatType)} / ${escapeHtml(order.passengerName)}</span>
      <span class="money">¥${formatAmount(order.amount)}</span>
    </button>
  `).join("");
  container.querySelectorAll("[data-order-status-jump]").forEach(item => {
    item.addEventListener("click", () => {
      elements.orderStatus.value = item.dataset.orderStatusJump || "";
      activateSection("passenger-orders");
      loadPassengerOrders();
    });
  });
}

async function loadPassengerTravelers(options = {}) {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const travelers = await passengerRequest("/passenger/travelers");
    passengerState.travelers = travelers || [];
    passengerState.travelerById = {};
    passengerState.travelers.forEach(traveler => {
      passengerState.travelerById[String(traveler.id)] = traveler;
    });
    renderPassengerTravelers();
    renderBuyTravelerOptions();
  } catch (error) {
    if (!options.silent) {
      elements.travelerList.innerHTML = recordEmpty(error.message || "无法加载常用乘车人");
      return;
    }
    throw error;
  }
}

function renderPassengerTravelers() {
  if (!passengerState.travelers.length) {
    elements.travelerList.innerHTML = recordEmpty("暂无常用乘车人，可先新增一位乘车人用于购票");
    return;
  }
  elements.travelerList.innerHTML = passengerState.travelers.map(traveler => `
    <article class="traveler-card ${traveler.defaultTraveler ? "default-traveler" : ""}">
      <div>
        <div class="event-header">
          <strong>${escapeHtml(traveler.passengerName)}</strong>
          ${traveler.defaultTraveler ? `<span class="status success">默认</span>` : ""}
        </div>
        <span>${idTypeText(traveler.idType)} / ${escapeHtml(traveler.idNoMasked || "-")}</span>
        <span>手机号 ${escapeHtml(traveler.phoneMasked || "-")}</span>
      </div>
      <div class="inline-actions">
        <button class="secondary-button compact-button" type="button" data-edit-traveler="${traveler.id}">编辑</button>
        <button class="ghost-button compact-button" type="button" data-default-traveler="${traveler.id}" ${traveler.defaultTraveler ? "disabled" : ""}>设默认</button>
        <button class="danger-button compact-button" type="button" data-delete-traveler="${traveler.id}">删除</button>
      </div>
    </article>
  `).join("");
  elements.travelerList.querySelectorAll("[data-edit-traveler]").forEach(button => {
    button.addEventListener("click", () => editTraveler(button.dataset.editTraveler));
  });
  elements.travelerList.querySelectorAll("[data-default-traveler]").forEach(button => {
    button.addEventListener("click", () => setDefaultTraveler(button.dataset.defaultTraveler));
  });
  elements.travelerList.querySelectorAll("[data-delete-traveler]").forEach(button => {
    button.addEventListener("click", () => deleteTraveler(button.dataset.deleteTraveler));
  });
}

function renderBuyTravelerOptions() {
  const options = passengerState.travelers.map(traveler => `
    <option value="${traveler.id}" ${traveler.defaultTraveler ? "selected" : ""}>
      ${escapeHtml(traveler.passengerName)} / ${idTypeText(traveler.idType)} / ${escapeHtml(traveler.idNoMasked || "-")}
    </option>
  `).join("");
  elements.buyTraveler.innerHTML = `<option value="">手动填写乘车人</option>${options}`;
  const defaultTraveler = passengerState.travelers.find(traveler => traveler.defaultTraveler);
  if (defaultTraveler) {
    elements.buyTraveler.value = String(defaultTraveler.id);
  }
}

async function savePassengerTraveler() {
  if (!ensureSignedIn()) {
    return;
  }
  const travelerId = elements.travelerId.value;
  const body = {
    passengerName: elements.travelerName.value.trim(),
    idType: elements.travelerIdType.value,
    idNo: elements.travelerIdNo.value.trim(),
    phone: elements.travelerPhone.value.trim(),
    defaultTraveler: elements.travelerDefault.checked,
  };
  if (!body.passengerName || (!travelerId && !body.idNo)) {
    elements.travelerError.textContent = "请填写乘车人姓名和证件号";
    return;
  }
  elements.travelerSubmit.disabled = true;
  elements.travelerError.textContent = "";
  try {
    await passengerRequest(travelerId ? `/passenger/travelers/${travelerId}` : "/passenger/travelers", {
      method: travelerId ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    resetTravelerForm();
    await loadPassengerTravelers();
    showToast(travelerId ? "常用乘车人已更新" : "常用乘车人已新增");
    document.querySelector("#passenger-travelers").scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    elements.travelerError.textContent = error.message || "保存乘车人失败";
  } finally {
    elements.travelerSubmit.disabled = false;
  }
}

function editTraveler(travelerId) {
  const traveler = passengerState.travelerById[String(travelerId)];
  if (!traveler) {
    return;
  }
  elements.travelerId.value = traveler.id;
  elements.travelerName.value = traveler.passengerName || "";
  elements.travelerIdType.value = traveler.idType || "ID_CARD";
  elements.travelerIdNo.value = "";
  elements.travelerIdNo.placeholder = "请重新输入证件号";
  elements.travelerPhone.value = "";
  elements.travelerPhone.placeholder = traveler.phoneMasked ? `当前 ${traveler.phoneMasked}，可重新输入` : "请输入手机号";
  elements.travelerDefault.checked = Boolean(traveler.defaultTraveler);
  elements.travelerSubmit.textContent = "保存修改";
  elements.travelerCancel.hidden = false;
  elements.travelerError.textContent = "如需修改证件号或手机号请重新输入；留空将保留当前脱敏信息。";
}

function resetTravelerForm() {
  elements.travelerId.value = "";
  elements.travelerName.value = "";
  elements.travelerIdType.value = "ID_CARD";
  elements.travelerIdNo.value = "";
  elements.travelerIdNo.placeholder = "请输入证件号";
  elements.travelerPhone.value = "";
  elements.travelerPhone.placeholder = "请输入手机号";
  elements.travelerDefault.checked = false;
  elements.travelerSubmit.textContent = "保存乘车人";
  elements.travelerCancel.hidden = true;
  elements.travelerError.textContent = "";
}

async function setDefaultTraveler(travelerId) {
  try {
    await passengerRequest(`/passenger/travelers/${travelerId}/default`, { method: "POST" });
    await loadPassengerTravelers();
    showToast("默认乘车人已更新");
  } catch (error) {
    showToast(error.message || "设置默认乘车人失败");
  }
}

async function deleteTraveler(travelerId) {
  try {
    await passengerRequest(`/passenger/travelers/${travelerId}`, { method: "DELETE" });
    await loadPassengerTravelers();
    showToast("常用乘车人已删除");
  } catch (error) {
    showToast(error.message || "删除乘车人失败");
  }
}

async function searchPassengerTrains() {
  const from = elements.fromStation.value;
  const to = elements.toStation.value;
  const date = elements.travelDate.value;
  if (!from || !to || !date) {
    renderTrainEmpty("请选择出发站、到达站和乘车日期，或直接查看全部可购车次。");
    return;
  }
  try {
    const trains = await passengerRequest(`/trains/search?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&date=${encodeURIComponent(date)}`);
    renderPassengerTrains(trains);
  } catch (error) {
    renderTrainEmpty(error.message || "无法获取车次数据");
  }
}

async function loadAvailablePassengerTrains() {
  try {
    const params = new URLSearchParams();
    if (elements.travelDate.value) {
      params.set("travelDate", elements.travelDate.value);
    }
    params.set("page", "0");
    params.set("size", "80");
    const trains = await passengerRequest(`/trains/available?${params.toString()}`);
    renderPassengerTrains(trains);
    showToast("已加载全部可购车次");
  } catch (error) {
    renderTrainEmpty(error.message || "无法获取可购车次");
  }
}

function renderPassengerTrains(trains) {
  if (!trains.length) {
    passengerState.trains = { ...passengerState.trains, items: [], page: 0 };
    renderTrainEmpty("当前线路暂无可售车次，可查看全部可购车次或选择热门线路。");
    return;
  }
  passengerState.trainByInventory = {};
  trains.forEach(train => {
    passengerState.trainByInventory[String(train.inventoryId)] = train;
  });
  passengerState.trains = { ...passengerState.trains, items: trains, page: 0 };
  renderTrainPage();
}

function renderTrainPage() {
  const { items, page, size } = passengerState.trains;
  if (!items.length) {
    renderTrainEmpty("当前线路暂无可售车次，可查看全部可购车次或选择热门线路。");
    return;
  }
  const totalPages = Math.max(1, Math.ceil(items.length / size));
  const safePage = Math.min(Math.max(page, 0), totalPages - 1);
  passengerState.trains.page = safePage;
  const start = safePage * size;
  const visibleTrains = items.slice(start, start + size);
  const controls = `
    <div class="train-list-toolbar">
      <div>
        <strong>当前展示 ${visibleTrains.length} / ${items.length} 趟</strong>
        <span>第 ${safePage + 1} / ${totalPages} 页</span>
      </div>
      <div class="inline-actions">
        <button class="secondary-button compact-button" type="button" data-train-page="prev" ${safePage === 0 ? "disabled" : ""}>上一页</button>
        <button class="secondary-button compact-button" type="button" data-train-page="next" ${safePage >= totalPages - 1 ? "disabled" : ""}>下一页</button>
      </div>
    </div>
  `;
  elements.trainResults.innerHTML = visibleTrains.map(train => `
    <article class="train-ticket-card">
      <div class="ticket-card-route">
        <strong class="train-no">${escapeHtml(train.trainNo)}</strong>
        <span>${escapeHtml(train.departureStation)} → ${escapeHtml(train.arrivalStation)}</span>
      </div>
      <div class="ticket-card-time">
        <strong>${formatTime(train.departureTime)}</strong>
        <span>${formatDate(train.travelDate)}</span>
        <strong>${formatTime(train.arrivalTime)}</strong>
      </div>
      <div class="ticket-card-meta">
        <span>${seatTypeText(train.seatType)}</span>
        <span class="${Number(train.remainingSeats || 0) <= 5 ? "inventory-low" : "inventory-ok"}">余票 ${train.remainingSeats}</span>
      </div>
      <div class="ticket-card-action">
        <span class="ticket-price">¥${formatAmount(train.price)}</span>
        <button class="primary-button compact-button" type="button" data-passenger-buy="${train.inventoryId}">购票</button>
      </div>
    </article>
  `).join("") + controls;
  elements.trainResults.querySelectorAll("[data-train-page]").forEach(button => {
    button.addEventListener("click", () => {
      const direction = button.dataset.trainPage === "next" ? 1 : -1;
      passengerState.trains.page += direction;
      renderTrainPage();
      elements.trainResults.scrollIntoView({ behavior: "smooth", block: "nearest" });
    });
  });
  elements.trainResults.querySelectorAll("[data-passenger-buy]").forEach(button => {
    button.addEventListener("click", () => openBuyModal(button.dataset.passengerBuy));
  });
}

function renderTrainEmpty(message) {
  elements.trainResults.innerHTML = `
    <div class="empty-action passenger-empty-action">
      <strong>${escapeHtml(message)}</strong>
      <span>可以直接查看全部可购车次，或点击热门线路快速定位有库存的线路。</span>
      <div class="inline-actions">
        <button class="secondary-button compact-button" type="button" data-empty-action="available">查看全部可购车次</button>
        <button class="ghost-button compact-button" type="button" data-empty-action="hot-routes">查看热门线路</button>
      </div>
    </div>
  `;
  elements.trainResults.querySelector('[data-empty-action="available"]').addEventListener("click", loadAvailablePassengerTrains);
  elements.trainResults.querySelector('[data-empty-action="hot-routes"]').addEventListener("click", () => renderHotRoutes(true));
}

function renderHotRoutes(expanded) {
  elements.hotRoutes.classList.toggle("expanded", Boolean(expanded));
  elements.hotRoutes.innerHTML = PASSENGER_HOT_ROUTES.map(route => `
    <button class="route-chip" type="button" data-hot-from="${route.from}" data-hot-to="${route.to}">
      ${escapeHtml(route.label)}
    </button>
  `).join("");
  elements.hotRoutes.querySelectorAll("[data-hot-from]").forEach(button => {
    button.addEventListener("click", async () => {
      elements.fromStation.value = button.dataset.hotFrom;
      elements.toStation.value = button.dataset.hotTo;
      if (!elements.travelDate.value) {
        elements.travelDate.value = new Date().toISOString().slice(0, 10);
      }
      await searchPassengerTrains();
    });
  });
}

async function openBuyModal(inventoryId) {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    await loadPassengerTravelers({ silent: true });
  } catch (error) {
    showToast(error.message || "无法刷新常用乘车人，请稍后重试");
  }
  const train = passengerState.trainByInventory[String(inventoryId)];
  if (!train) {
    showToast("未找到当前车次库存，请重新查询");
    return;
  }
  passengerState.selectedTrain = train;
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
      <div><span>票价</span><strong class="money">¥${formatAmount(train.price)}</strong></div>
      <div><span>剩余票数</span><strong>${train.remainingSeats}</strong></div>
    </div>
  `;
  renderBuyTravelerOptions();
  applySelectedTravelerToBuyForm();
  if (!elements.buyTraveler.value) {
    elements.buyName.value = passengerState.auth.displayName || "";
    elements.buyIdType.value = "ID_CARD";
    elements.buyIdCard.value = "";
    elements.buyPhone.value = "";
  }
  elements.buyError.textContent = "";
  elements.buyConfirm.disabled = false;
  elements.buyConfirm.textContent = "确认下单";
  elements.buyModal.classList.add("show");
  elements.buyModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("modal-open");
  window.setTimeout(() => (elements.buyTraveler.value ? elements.buyConfirm.focus() : elements.buyName.focus()), 80);
}

function applySelectedTravelerToBuyForm() {
  const travelerId = elements.buyTraveler.value;
  const traveler = passengerState.travelerById[String(travelerId)];
  if (!traveler) {
    elements.buyName.disabled = false;
    elements.buyIdType.disabled = false;
    elements.buyIdCard.disabled = false;
    elements.buyPhone.disabled = false;
    if (!elements.buyName.value && passengerState.auth && passengerState.auth.displayName) {
      elements.buyName.value = passengerState.auth.displayName;
    }
    return;
  }
  elements.buyName.value = traveler.passengerName || "";
  elements.buyIdType.value = traveler.idType || "ID_CARD";
  elements.buyIdCard.value = traveler.idNoMasked || "";
  elements.buyPhone.value = traveler.phoneMasked || "";
  elements.buyName.disabled = true;
  elements.buyIdType.disabled = true;
  elements.buyIdCard.disabled = true;
  elements.buyPhone.disabled = true;
}

function closeBuyModal() {
  elements.buyModal.classList.remove("show");
  elements.buyModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("modal-open");
  passengerState.selectedTrain = null;
}

async function submitPassengerOrder() {
  const train = passengerState.selectedTrain;
  if (!train) {
    return;
  }
  const selectedTravelerId = elements.buyTraveler.value;
  const passengerName = elements.buyName.value.trim();
  const passengerIdCard = elements.buyIdCard.value.trim();
  if (!selectedTravelerId && (!passengerName || !passengerIdCard)) {
    elements.buyError.textContent = "请完整填写乘客姓名和证件号";
    return;
  }
  elements.buyConfirm.disabled = true;
  elements.buyConfirm.textContent = "下单中...";
  elements.buyError.textContent = "";
  try {
    const body = {
      requestId: generateRequestId("PAX-ORDER"),
      trainId: Number(train.trainId),
      inventoryId: Number(train.inventoryId),
    };
    if (selectedTravelerId) {
      body.travelerId = Number(selectedTravelerId);
    } else {
      body.passengerName = passengerName;
      body.passengerIdCard = passengerIdCard;
      body.passengerIdType = elements.buyIdType.value;
      body.passengerPhone = elements.buyPhone.value.trim();
    }
    await passengerRequest("/passenger/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    closeBuyModal();
    showToast("订单已创建，库存已锁定，请及时支付");
    await Promise.all([loadPassengerSummary(), loadPassengerTravelers(), loadPassengerOrders(), loadAvailablePassengerTrains()]);
    activateSection("passenger-orders");
  } catch (error) {
    elements.buyError.textContent = error.message || "购票失败，请稍后重试";
    elements.buyConfirm.disabled = false;
    elements.buyConfirm.textContent = "确认下单";
  }
}

async function loadPassengerOrders() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const params = new URLSearchParams();
    if (elements.orderStatus.value) {
      params.set("status", elements.orderStatus.value);
    }
    params.set("page", String(passengerState.orders.page));
    params.set("size", String(passengerState.orders.size));
    const page = await passengerRequest(`/passenger/orders?${params.toString()}`);
    passengerState.orders = { ...passengerState.orders, ...page };
    renderPassengerOrders(page.content || []);
    renderPagination("orders");
  } catch (error) {
    elements.orderCards.innerHTML = emptyItem(error.message || "无法加载我的订单");
    renderPagination("orders");
  }
}

function renderPassengerOrders(orders) {
  if (!orders.length) {
    elements.orderCards.innerHTML = emptyItem("暂无订单，可先查询车次并下单。");
    return;
  }
  elements.orderCards.innerHTML = orders.map(order => `
    <article class="passenger-order-card">
      <div class="passenger-order-main">
        <div>
          <p class="eyebrow">${escapeHtml(order.orderNo)}</p>
          <h3>${escapeHtml(order.trainNo)} · ${formatDate(order.travelDate)}</h3>
          <span>${seatTypeText(order.seatType)} / ${escapeHtml(order.passengerName)} / ${escapeHtml(order.passengerIdNoMasked || "-")}</span>
        </div>
        <div class="passenger-order-price">
          <strong>¥${formatAmount(order.amount)}</strong>
          <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
        </div>
      </div>
      <div class="passenger-order-meta">
        <span>创建：${formatDateTime(order.createdAt) || "-"}</span>
        <span>支付截止：${formatDateTime(order.paymentDeadlineAt) || "-"}</span>
      </div>
      <div class="inline-actions passenger-order-actions">${renderOrderActions(order)}</div>
    </article>
  `).join("");
  decoratePassengerOrderDetailButtons(orders);
  elements.orderCards.querySelectorAll("[data-pay-order]").forEach(button => {
    button.addEventListener("click", () => payPassengerOrder(button.dataset.payOrder));
  });
  elements.orderCards.querySelectorAll("[data-close-order]").forEach(button => {
    button.addEventListener("click", () => closePassengerOrder(button.dataset.closeOrder));
  });
  elements.orderCards.querySelectorAll("[data-refund-order]").forEach(button => {
    button.addEventListener("click", () => refundPassengerOrder(button.dataset.refundOrder));
  });
  elements.orderCards.querySelectorAll("[data-jump-refunds]").forEach(button => {
    button.addEventListener("click", () => {
      activateSection("passenger-refunds");
      loadPassengerRefunds();
    });
  });
}

function decoratePassengerOrderDetailButtons(orders) {
  const cards = elements.orderCards.querySelectorAll(".passenger-order-card");
  cards.forEach((card, index) => {
    const order = orders[index];
    const actions = card.querySelector(".passenger-order-actions");
    if (!order || !actions || actions.querySelector("[data-passenger-order-detail]")) {
      return;
    }
    const button = document.createElement("button");
    button.className = "secondary-button compact-button";
    button.type = "button";
    button.textContent = "详情";
    button.dataset.passengerOrderDetail = String(order.id);
    button.addEventListener("click", () => openPassengerOrderDetail(order.id));
    actions.insertBefore(button, actions.firstChild);
  });
}

async function openPassengerOrderDetail(orderId) {
  try {
    const detail = await passengerRequest(`/passenger/orders/${orderId}/detail`);
    showPassengerOrderDetail(detail);
  } catch (error) {
    showToast(error.message || "无法加载订单详情");
  }
}

function showPassengerOrderDetail(detail) {
  const modal = ensurePassengerDetailModal();
  const order = detail.order || {};
  const ticket = detail.ticket;
  const payments = detail.payments || [];
  const refunds = detail.refunds || [];
  modal.querySelector(".order-detail-body").innerHTML = `
    <div class="detail-hero">
      <div>
        <p class="eyebrow">Order Detail</p>
        <h2>${escapeHtml(order.trainNo || "-")} / ${escapeHtml(order.orderNo || "-")}</h2>
        <span>${formatDate(order.travelDate)} · ${seatTypeText(order.seatType)} · ${escapeHtml(order.passengerName || "-")}</span>
      </div>
      <div class="detail-amount">
        <strong>¥${formatAmount(order.amount)}</strong>
        <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
      </div>
    </div>
    <section class="detail-section">
      <h3>电子票 / 行程单</h3>
      ${ticket ? renderTicketDetail(ticket) : `<div class="detail-empty">订单支付成功后自动生成电子票。</div>`}
    </section>
    <section class="detail-section">
      <h3>支付记录</h3>
      ${renderDetailRecords(payments, payment => `
        <div class="detail-record">
          <strong>${escapeHtml(payment.paymentNo)}</strong>
          <span>${paymentStatusText(payment.status)} · ¥${formatAmount(payment.amount)} · ${formatDateTime(payment.paidAt) || "-"}</span>
        </div>
      `, "暂无支付记录")}
    </section>
    <section class="detail-section">
      <h3>退款记录</h3>
      ${renderDetailRecords(refunds, refund => `
        <div class="detail-record">
          <strong>${escapeHtml(refund.refundNo)}</strong>
          <span>${refundStatusText(refund.status)} · ¥${formatAmount(refund.amount)} · ${formatDateTime(refund.refundedAt) || "-"}</span>
        </div>
      `, "暂无退款记录")}
    </section>
  `;
  openDetailModal(modal);
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

function ensurePassengerDetailModal() {
  let modal = document.querySelector("#passenger-order-detail-modal");
  if (modal) {
    return modal;
  }
  modal = document.createElement("div");
  modal.id = "passenger-order-detail-modal";
  modal.className = "modal-backdrop order-detail-modal";
  modal.setAttribute("aria-hidden", "true");
  modal.innerHTML = `
    <div class="purchase-modal order-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="passenger-order-detail-title">
      <button class="modal-close" type="button" aria-label="关闭订单详情"></button>
      <div class="modal-head">
        <p class="eyebrow">Passenger Order</p>
        <h2 id="passenger-order-detail-title">订单详情</h2>
        <p>展示当前订单的电子票、支付和退款记录。</p>
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

function renderOrderActions(order) {
  if (order.status === "PENDING_PAYMENT") {
    return `
      <button class="primary-button compact-button" type="button" data-pay-order="${order.id}">去支付</button>
      <button class="danger-button compact-button" type="button" data-close-order="${order.id}">取消订单</button>
    `;
  }
  if (order.status === "PAID") {
    return `<button class="danger-button compact-button" type="button" data-refund-order="${order.id}">申请退票</button>`;
  }
  if (order.status === "REFUNDED") {
    return `<button class="secondary-button compact-button" type="button" data-jump-refunds>查看退款</button>`;
  }
  return `<span class="muted-text">无需操作</span>`;
}

async function payPassengerOrder(orderId) {
  try {
    await passengerRequest(`/passenger/orders/${orderId}/pay`, { method: "POST" });
    showToast("支付成功，订单状态已更新");
    await refreshPassengerData();
    activateSection("passenger-orders");
  } catch (error) {
    showToast(error.message || "支付失败");
  }
}

async function closePassengerOrder(orderId) {
  try {
    await passengerRequest(`/passenger/orders/${orderId}/close`, { method: "POST" });
    showToast("订单已取消，库存已释放");
    await refreshPassengerData();
  } catch (error) {
    showToast(error.message || "取消订单失败");
  }
}

async function refundPassengerOrder(orderId) {
  try {
    await passengerRequest(`/passenger/orders/${orderId}/refund`, { method: "POST" });
    showToast("退票成功，已创建退款流水");
    await refreshPassengerData();
    await loadPassengerRefunds();
  } catch (error) {
    showToast(error.message || "退票失败");
  }
}

async function loadPassengerPayments() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const params = new URLSearchParams();
    if (elements.paymentStatus.value) {
      params.set("status", elements.paymentStatus.value);
    }
    params.set("page", String(passengerState.payments.page));
    params.set("size", String(passengerState.payments.size));
    const page = await passengerRequest(`/passenger/payments?${params.toString()}`);
    passengerState.payments = { ...passengerState.payments, ...page };
    renderPassengerPayments(page.content || []);
    renderPagination("payments");
  } catch (error) {
    elements.paymentResults.innerHTML = recordEmpty(error.message || "无法加载支付流水");
    renderPagination("payments");
  }
}

function renderPassengerPayments(payments) {
  if (!payments.length) {
    elements.paymentResults.innerHTML = recordEmpty("暂无支付流水");
    return;
  }
  elements.paymentResults.innerHTML = payments.map(payment => `
    <article class="money-record-card">
      <div class="record-title-row">
        <div>
          <span>支付流水</span>
          <strong>${escapeHtml(payment.paymentNo)}</strong>
        </div>
        <span class="status ${paymentStatusClass(payment.status)}">${paymentStatusText(payment.status)}</span>
      </div>
      <div class="record-detail-grid">
        <div><span>订单号</span><strong>${escapeHtml(payment.orderNo)}</strong></div>
        <div><span>金额</span><strong class="money">¥${formatAmount(payment.amount)}</strong></div>
        <div><span>渠道流水</span><strong>${escapeHtml(payment.channelPaymentNo || "-")}</strong></div>
        <div><span>支付时间</span><strong>${formatDateTime(payment.paidAt) || "-"}</strong></div>
      </div>
    </article>
  `).join("");
}

async function loadPassengerRefunds() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const params = new URLSearchParams();
    if (elements.refundStatus.value) {
      params.set("status", elements.refundStatus.value);
    }
    params.set("page", String(passengerState.refunds.page));
    params.set("size", String(passengerState.refunds.size));
    const page = await passengerRequest(`/passenger/refunds?${params.toString()}`);
    passengerState.refunds = { ...passengerState.refunds, ...page };
    renderPassengerRefunds(page.content || []);
    renderPagination("refunds");
  } catch (error) {
    elements.refundResults.innerHTML = recordEmpty(error.message || "无法加载退款流水");
    renderPagination("refunds");
  }
}

function renderPassengerRefunds(refunds) {
  if (!refunds.length) {
    elements.refundResults.innerHTML = recordEmpty("暂无退款流水");
    return;
  }
  elements.refundResults.innerHTML = refunds.map(refund => `
    <article class="money-record-card refund-record-card">
      <div class="record-title-row">
        <div>
          <span>退款流水</span>
          <strong>${escapeHtml(refund.refundNo)}</strong>
        </div>
        <span class="status ${refundStatusClass(refund.status)}">${refundStatusText(refund.status)}</span>
      </div>
      <div class="record-detail-grid">
        <div><span>订单号</span><strong>${escapeHtml(refund.orderNo)}</strong></div>
        <div><span>支付流水</span><strong>${escapeHtml(refund.paymentNo || "-")}</strong></div>
        <div><span>金额</span><strong class="money">¥${formatAmount(refund.amount)}</strong></div>
        <div><span>渠道退款号</span><strong>${escapeHtml(refund.channelRefundNo || "-")}</strong></div>
        <div><span>退款时间</span><strong>${formatDateTime(refund.refundedAt) || "-"}</strong></div>
      </div>
    </article>
  `).join("");
}

async function changePassengerPage(type, offset) {
  const pageState = passengerState[type];
  const nextPage = Math.max(0, pageState.page + offset);
  if (nextPage === pageState.page) {
    return;
  }
  pageState.page = nextPage;
  if (type === "orders") {
    await loadPassengerOrders();
  } else if (type === "payments") {
    await loadPassengerPayments();
  } else {
    await loadPassengerRefunds();
  }
}

function renderPagination(type) {
  const map = {
    orders: { state: passengerState.orders, info: elements.orderPageInfo, prev: elements.prevOrders, next: elements.nextOrders },
    payments: { state: passengerState.payments, info: elements.paymentPageInfo, prev: elements.prevPayments, next: elements.nextPayments },
    refunds: { state: passengerState.refunds, info: elements.refundPageInfo, prev: elements.prevRefunds, next: elements.nextRefunds },
  };
  const target = map[type];
  const totalPages = Math.max(1, target.state.totalPages || 0);
  const currentPage = Math.min((target.state.page || 0) + 1, totalPages);
  target.info.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${target.state.totalElements || 0} 条`;
  target.prev.disabled = Boolean(target.state.first);
  target.next.disabled = Boolean(target.state.last);
}

function renderLoggedOutPlaceholders() {
  elements.metricTotal.textContent = "0";
  elements.metricPending.textContent = "0";
  elements.metricPaid.textContent = "0";
  elements.metricClosed.textContent = "0";
  elements.metricRefunded.textContent = "0";
  elements.metricPayments.textContent = "0";
  elements.metricRefunds.textContent = "0";
  elements.latestOrders.innerHTML = emptyItem("登录后查看最近订单");
  elements.upcomingTrips.innerHTML = emptyItem("登录后查看即将出行");
  passengerState.travelers = [];
  passengerState.travelerById = {};
  elements.travelerList.innerHTML = recordEmpty("登录后维护常用乘车人");
  renderBuyTravelerOptions();
  resetTravelerForm();
  elements.orderCards.innerHTML = emptyItem("登录后查看我的订单");
  elements.paymentResults.innerHTML = recordEmpty("登录后查看支付流水");
  elements.refundResults.innerHTML = recordEmpty("登录后查看退款流水");
}

async function passengerRequest(path, options = {}, withAuth = true) {
  const requestOptions = { ...options };
  requestOptions.headers = { ...(options.headers || {}) };
  const hasAuthHeader = Boolean(withAuth && passengerState.auth && passengerState.auth.token);
  if (hasAuthHeader) {
    requestOptions.headers.Authorization = `Bearer ${passengerState.auth.token}`;
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, requestOptions);
  } catch (error) {
    const networkError = new Error("无法连接后端服务，请确认 Spring Boot 后端已启动");
    networkError.status = 0;
    throw networkError;
  }
  const data = await readBody(response);
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
      const forbiddenError = new Error("当前账号无权访问该功能");
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

async function readBody(response) {
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

function ensureSignedIn() {
  if (passengerState.auth && passengerState.auth.token) {
    return true;
  }
  showToast("请先登录乘客端");
  document.querySelector(".passenger-login").scrollIntoView({ behavior: "smooth", block: "start" });
  return false;
}

function handleAuthExpired() {
  passengerState.auth = null;
  passengerState.authExpiredNotified = true;
  localStorage.removeItem(PASSENGER_AUTH_KEY);
  sessionStorage.removeItem(PASSENGER_AUTH_KEY);
  renderAuthState();
  renderLoggedOutPlaceholders();
  showToast("登录已过期，请重新登录");
}

function loadPassengerAuth() {
  try {
    const stored = localStorage.getItem(PASSENGER_AUTH_KEY) || sessionStorage.getItem(PASSENGER_AUTH_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    return null;
  }
}

function setupPassengerNavigation() {
  document.querySelectorAll(".passenger-nav a").forEach(link => {
    link.addEventListener("click", event => {
      event.preventDefault();
      activateSection(link.getAttribute("href").slice(1));
    });
  });
}

function setupPassengerScrollSpy() {
  const sections = Array.from(document.querySelectorAll(".passenger-section"));
  if (!sections.length) {
    updateActiveNav("passenger-search");
    return;
  }
  const onScroll = () => {
    if (passengerState.scrollTicking) {
      return;
    }
    passengerState.scrollTicking = true;
    window.requestAnimationFrame(() => {
      updateHeaderDensity();
      const headerOffset = getPassengerHeaderOffset();
      const anchorY = window.scrollY + headerOffset + 28;
      let current = sections[0].id;
      sections.forEach(section => {
        if (section.offsetTop <= anchorY) {
          current = section.id;
        }
      });
      updateActiveNav(current);
      passengerState.scrollTicking = false;
    });
  };
  window.addEventListener("scroll", onScroll, { passive: true });
  window.addEventListener("resize", onScroll);
  onScroll();
}

function updateActiveNav(sectionId) {
  document.querySelectorAll(".passenger-nav a").forEach(link => {
    link.classList.toggle("active", link.getAttribute("href") === `#${sectionId}`);
  });
}

function activateSection(sectionId) {
  const section = document.querySelector(`#${sectionId}`);
  if (!section) {
    return;
  }
  updateActiveNav(sectionId);
  const top = section.getBoundingClientRect().top + window.scrollY - getPassengerHeaderOffset() - 14;
  window.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
  section.classList.add("section-highlight");
  window.setTimeout(() => section.classList.remove("section-highlight"), 1200);
}

function getPassengerHeaderOffset() {
  const header = document.querySelector(".passenger-site-header");
  return header ? header.getBoundingClientRect().height : 0;
}

function updateHeaderDensity() {
  const currentY = window.scrollY;
  const goingDown = currentY > passengerState.lastScrollY + 8;
  const goingUp = currentY < passengerState.lastScrollY - 8;
  const atTop = currentY < 24;
  if (atTop || goingUp) {
    document.body.classList.remove("passenger-header-compact");
  } else if (goingDown && currentY > 120) {
    document.body.classList.add("passenger-header-compact");
  }
  passengerState.lastScrollY = currentY;
}

function tableEmpty(colspan, message) {
  return `<tr><td colspan="${colspan}"><div class="empty-action"><strong>${escapeHtml(message)}</strong></div></td></tr>`;
}

function emptyItem(message) {
  return `<div class="event-item empty-item"><span>${escapeHtml(message)}</span></div>`;
}

function recordEmpty(message) {
  return `<div class="record-empty"><strong>${escapeHtml(message)}</strong><span>完成相关交易后，这里会展示当前乘客账号的资金记录。</span></div>`;
}

function generateRequestId(prefix) {
  if (window.crypto && window.crypto.randomUUID) {
    return `${prefix}-${window.crypto.randomUUID()}`;
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : "-";
}

function formatDateTime(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "";
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : "-";
}

function formatAmount(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number.toFixed(2) : String(value || "-");
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

function roleText(value) {
  const map = {
    USER: "普通乘客",
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

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.add("show");
  window.setTimeout(() => {
    elements.toast.classList.remove("show");
  }, 2800);
}
