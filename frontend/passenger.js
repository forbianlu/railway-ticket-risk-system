const API_BASE =
  window.RAILWAY_API_BASE ||
  (["localhost", "127.0.0.1"].includes(window.location.hostname)
    ? "http://localhost:8080/api"
    : "/api");
const PASSENGER_AUTH_KEY = "railway-passenger-auth";
const PASSENGER_ONBOARDING_KEY_PREFIX = "railway-passenger-onboarding-dismissed:";

const passengerState = {
  auth: loadPassengerAuth(),
  stations: [],
  trainByInventory: {},
  trainGroupByKey: {},
  travelers: [],
  travelerById: {},
  selectedTrain: null,
  selectedInventory: null,
  selectedChangeOrder: null,
  selectedChangeTrain: null,
  changeCandidates: [],
  activeDetailOrderId: null,
  showOnboarding: false,
  refreshingFlow: false,
  authExpiredNotified: false,
  profile: null,
  avatarObjectUrl: null,
  avatarRequestId: 0,
  summary: null,
  navObserver: null,
  scrollTicking: false,
  lastScrollY: 0,
  selectedNotificationId: null,
  trains: { items: [], page: 0, size: 7 },
  orders: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
  tickets: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
  payments: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
  refunds: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
  changes: { page: 0, size: 6, totalPages: 0, totalElements: 0, first: true, last: true },
  notifications: { page: 0, size: 8, totalPages: 0, totalElements: 0, first: true, last: true },
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
  registerForm: document.querySelector("#passenger-register-form"),
  registerDisplayName: document.querySelector("#passenger-register-display-name"),
  registerUsername: document.querySelector("#passenger-register-username"),
  registerPassword: document.querySelector("#passenger-register-password"),
  registerConfirmPassword: document.querySelector("#passenger-register-confirm-password"),
  registerError: document.querySelector("#passenger-register-error"),
  showLogin: document.querySelector("#passenger-show-login"),
  showRegister: document.querySelector("#passenger-show-register"),
  apiStatus: document.querySelector("#passenger-api-status"),
  apiStatusText: document.querySelector("#passenger-api-status-text"),
  authRole: document.querySelector("#passenger-auth-role"),
  authUser: document.querySelector("#passenger-auth-user"),
  topAvatar: document.querySelector(".passenger-avatar"),
  logoutButton: document.querySelector("#passenger-logout-button"),
  refreshPassenger: document.querySelector("#refresh-passenger"),
  onboardingNav: document.querySelector('.passenger-nav a[href="#passenger-onboarding"]'),
  onboardingSection: document.querySelector("#passenger-onboarding"),
  onboardingSteps: document.querySelector("#passenger-onboarding-steps"),
  onboardingAction: document.querySelector("#passenger-onboarding-action"),
  profileAvatar: document.querySelector("#passenger-profile-avatar"),
  profileRole: document.querySelector("#passenger-profile-role"),
  profileDisplay: document.querySelector("#passenger-profile-display"),
  profileUsername: document.querySelector("#passenger-profile-username"),
  profileDefaultTraveler: document.querySelector("#passenger-profile-default-traveler"),
  profileOrderCount: document.querySelector("#passenger-profile-order-count"),
  profileTicketCount: document.querySelector("#passenger-profile-ticket-count"),
  profileDetailDisplay: document.querySelector("#passenger-profile-detail-display"),
  profileDetailUsername: document.querySelector("#passenger-profile-detail-username"),
  profileDetailRole: document.querySelector("#passenger-profile-detail-role"),
  profileDetailTraveler: document.querySelector("#passenger-profile-detail-traveler"),
  profileDetailOrders: document.querySelector("#passenger-profile-detail-orders"),
  profileDetailTickets: document.querySelector("#passenger-profile-detail-tickets"),
  profileForm: document.querySelector("#passenger-profile-form"),
  profileDisplayName: document.querySelector("#passenger-profile-display-name"),
  profileError: document.querySelector("#passenger-profile-error"),
  avatarFile: document.querySelector("#passenger-avatar-file"),
  avatarUploadButton: document.querySelector("#passenger-avatar-upload-button"),
  avatarRemoveButton: document.querySelector("#passenger-avatar-remove-button"),
  avatarStatus: document.querySelector("#passenger-avatar-status"),
  passwordForm: document.querySelector("#passenger-password-form"),
  oldPassword: document.querySelector("#passenger-old-password"),
  newPassword: document.querySelector("#passenger-new-password"),
  confirmPassword: document.querySelector("#passenger-confirm-password"),
  passwordError: document.querySelector("#passenger-password-error"),
  metricTotal: document.querySelector("#passenger-metric-total"),
  metricPending: document.querySelector("#passenger-metric-pending"),
  metricPaid: document.querySelector("#passenger-metric-paid"),
  metricClosed: document.querySelector("#passenger-metric-closed"),
  metricRefunded: document.querySelector("#passenger-metric-refunded"),
  metricPayments: document.querySelector("#passenger-metric-payments"),
  metricRefunds: document.querySelector("#passenger-metric-refunds"),
  latestOrders: document.querySelector("#passenger-latest-orders"),
  upcomingTrips: document.querySelector("#passenger-upcoming-trips"),
  refreshTransactions: document.querySelector("#passenger-refresh-transactions"),
  transactionStats: document.querySelector("#passenger-transaction-stats"),
  transactionTodos: document.querySelector("#passenger-transaction-todos"),
  transactionOrders: document.querySelector("#passenger-transaction-orders"),
  transactionChanges: document.querySelector("#passenger-transaction-changes"),
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
  ticketStatus: document.querySelector("#passenger-ticket-status"),
  loadTickets: document.querySelector("#passenger-load-tickets"),
  ticketResults: document.querySelector("#passenger-ticket-results"),
  ticketPageInfo: document.querySelector("#passenger-ticket-page-info"),
  prevTickets: document.querySelector("#passenger-prev-tickets"),
  nextTickets: document.querySelector("#passenger-next-tickets"),
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
  changeStatus: document.querySelector("#passenger-change-status"),
  loadChanges: document.querySelector("#passenger-load-changes"),
  changeResults: document.querySelector("#passenger-change-results"),
  changePageInfo: document.querySelector("#passenger-change-page-info"),
  prevChanges: document.querySelector("#passenger-prev-changes"),
  nextChanges: document.querySelector("#passenger-next-changes"),
  navUnread: document.querySelector("#passenger-nav-unread"),
  notificationStatus: document.querySelector("#passenger-notification-status"),
  notificationTotal: document.querySelector("#passenger-notification-total"),
  notificationUnread: document.querySelector("#passenger-notification-unread"),
  notificationGuide: document.querySelector("#passenger-notification-guide"),
  loadNotifications: document.querySelector("#passenger-load-notifications"),
  refreshNotifications: document.querySelector("#passenger-refresh-notifications"),
  readAllNotifications: document.querySelector("#passenger-read-all-notifications"),
  notificationResults: document.querySelector("#passenger-notification-results"),
  notificationPageInfo: document.querySelector("#passenger-notification-page-info"),
  prevNotifications: document.querySelector("#passenger-prev-notifications"),
  nextNotifications: document.querySelector("#passenger-next-notifications"),
  buyModal: document.querySelector("#passenger-buy-modal"),
  buyModalClose: document.querySelector("#passenger-buy-modal-close"),
  buyCancel: document.querySelector("#passenger-buy-cancel"),
  buyForm: document.querySelector("#passenger-buy-form"),
  buySummary: document.querySelector("#passenger-buy-summary"),
  buySeatType: document.querySelector("#passenger-buy-seat-type"),
  buyTraveler: document.querySelector("#passenger-buy-traveler"),
  buyName: document.querySelector("#passenger-buy-name"),
  buyIdType: document.querySelector("#passenger-buy-id-type"),
  buyIdCard: document.querySelector("#passenger-buy-id-card"),
  buyPhone: document.querySelector("#passenger-buy-phone"),
  buyError: document.querySelector("#passenger-buy-error"),
  buyConfirm: document.querySelector("#passenger-buy-confirm"),
  changeModal: document.querySelector("#passenger-change-modal"),
  changeModalClose: document.querySelector("#passenger-change-modal-close"),
  changeCancel: document.querySelector("#passenger-change-cancel"),
  changeForm: document.querySelector("#passenger-change-form"),
  changeSummary: document.querySelector("#passenger-change-summary"),
  changeDate: document.querySelector("#passenger-change-date"),
  changeReason: document.querySelector("#passenger-change-reason"),
  changeLoadTrains: document.querySelector("#passenger-change-load-trains"),
  changeCandidates: document.querySelector("#passenger-change-candidates"),
  changeError: document.querySelector("#passenger-change-error"),
  changeConfirm: document.querySelector("#passenger-change-confirm"),
  onboardingModal: document.querySelector("#passenger-onboarding-modal"),
  onboardingModalClose: document.querySelector("#passenger-onboarding-modal-close"),
  onboardingStart: document.querySelector("#passenger-onboarding-start"),
  onboardingSkip: document.querySelector("#passenger-onboarding-skip"),
  toast: document.querySelector("#passenger-toast"),
};

elements.loginForm.addEventListener("submit", event => {
  event.preventDefault();
  loginPassenger();
});
elements.registerForm.addEventListener("submit", event => {
  event.preventDefault();
  registerPassenger();
});
elements.showLogin.addEventListener("click", () => switchPassengerAuthMode("login"));
elements.showRegister.addEventListener("click", () => switchPassengerAuthMode("register"));
elements.logoutButton.addEventListener("click", logoutPassenger);
elements.refreshPassenger?.addEventListener("click", refreshPassengerData);
elements.onboardingAction.addEventListener("click", () => {
  const target = elements.onboardingAction.dataset.target || "passenger-search";
  if (target === "dismiss-onboarding") {
    dismissPassengerOnboarding();
    return;
  }
  activateSection(target);
});
elements.onboardingModalClose.addEventListener("click", skipPassengerOnboarding);
elements.onboardingSkip.addEventListener("click", skipPassengerOnboarding);
elements.onboardingStart.addEventListener("click", startPassengerOnboarding);
elements.onboardingModal.addEventListener("click", event => {
  if (event.target === elements.onboardingModal) {
    skipPassengerOnboarding();
  }
});
elements.profileForm.addEventListener("submit", event => {
  event.preventDefault();
  savePassengerProfile();
});
elements.avatarUploadButton?.addEventListener("click", () => {
  if (!ensureSignedIn()) {
    return;
  }
  elements.avatarFile?.click();
});
elements.avatarFile?.addEventListener("change", updatePassengerAvatar);
elements.avatarRemoveButton?.addEventListener("click", deletePassengerAvatar);
elements.passwordForm.addEventListener("submit", event => {
  event.preventDefault();
  changePassengerPassword();
});
elements.refreshTransactions?.addEventListener("click", loadPassengerTransactionSummary);
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
elements.buySeatType?.addEventListener("change", updateSelectedBuySeat);
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
elements.loadTickets.addEventListener("click", () => {
  passengerState.tickets.page = 0;
  loadPassengerTickets();
});
elements.ticketStatus.addEventListener("change", () => {
  passengerState.tickets.page = 0;
  loadPassengerTickets();
});
elements.prevTickets.addEventListener("click", () => changePassengerPage("tickets", -1));
elements.nextTickets.addEventListener("click", () => changePassengerPage("tickets", 1));
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
elements.loadChanges.addEventListener("click", () => {
  passengerState.changes.page = 0;
  loadPassengerChanges();
});
elements.changeStatus.addEventListener("change", () => {
  passengerState.changes.page = 0;
  loadPassengerChanges();
});
elements.prevChanges.addEventListener("click", () => changePassengerPage("changes", -1));
elements.nextChanges.addEventListener("click", () => changePassengerPage("changes", 1));
elements.loadNotifications.addEventListener("click", () => {
  passengerState.notifications.page = 0;
  loadPassengerNotifications();
});
elements.refreshNotifications?.addEventListener("click", () => loadPassengerNotifications());
elements.readAllNotifications?.addEventListener("click", markAllPassengerNotificationsRead);
elements.prevNotifications.addEventListener("click", () => changePassengerPage("notifications", -1));
elements.nextNotifications.addEventListener("click", () => changePassengerPage("notifications", 1));
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
elements.changeLoadTrains.addEventListener("click", loadChangeCandidates);
elements.changeForm.addEventListener("submit", event => {
  event.preventDefault();
  submitTicketChange();
});
elements.changeCancel.addEventListener("click", closeChangeModal);
elements.changeModalClose.addEventListener("click", closeChangeModal);
elements.changeModal.addEventListener("click", event => {
  if (event.target === elements.changeModal) {
    closeChangeModal();
  }
});
window.addEventListener("keydown", event => {
  if (event.key === "Escape" && elements.buyModal.classList.contains("show")) {
    closeBuyModal();
  }
  if (event.key === "Escape" && elements.changeModal.classList.contains("show")) {
    closeChangeModal();
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
  organizePassengerSections();
  setupPassengerFeatureTabs();
  setupPassengerNavigation();
  setupPassengerScrollSpy();
  syncPassengerOnboardingNav(true);
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
    passengerState.showOnboarding = false;
    localStorage.setItem(PASSENGER_AUTH_KEY, JSON.stringify(auth));
    let revealedTarget = false;
    let refreshPromise = Promise.resolve();
    const revealTarget = () => {
      if (revealedTarget) {
        return;
      }
      revealedTarget = true;
      renderAuthState();
      refreshPromise = refreshPassengerData().then(() => loadAvailablePassengerTrains());
    };
    const transition = typeof window.playRailwayLoginTransition === "function"
      ? window.playRailwayLoginTransition({
          variant: "passenger",
          label: "\u94c1\u8def\u5ba2\u8fd0\u7968\u52a1",
          destination: "\u4e58\u5ba2\u7aef",
          prepareReveal: revealTarget,
        })
      : Promise.resolve();
    await transition;
    revealTarget();
    await refreshPromise;
    showToast("登录成功，欢迎使用乘客购票服务");
  } catch (error) {
    showToast(error.message || "登录失败");
  }
}

function organizePassengerSections() {
  mergePassengerSection("passenger-travelers", "passenger-account", "merged-travelers");
  if (elements.loadPayments) {
    const heading = document.querySelector("#passenger-payments .passenger-section-head h2");
    const eyebrow = document.querySelector("#passenger-payments .passenger-section-head .eyebrow");
    if (heading) {
      heading.textContent = "支付与退款记录";
    }
    if (eyebrow) {
      eyebrow.textContent = "订单记录";
    }
  }
}

function mergePassengerSection(childId, parentId, className) {
  const child = document.getElementById(childId);
  const parent = document.getElementById(parentId);
  if (!child || !parent || child.parentElement === parent) {
    return;
  }
  child.classList.add("passenger-section-embedded", className);
  parent.appendChild(child);
}

function setupPassengerFeatureTabs() {
  const state = getPassengerFeatureTabState();
  state.tabById.clear();
  state.tabBySectionId.clear();
  state.defaultTabBySectionId.clear();
  document.body.dataset.passengerFeatureTabsReady = "true";

  setupPassengerTabGroup({
    sectionId: "passenger-summary",
    tabsLabel: "行程分页",
    defaultTabId: "journey-overview",
    tabs: [
      { id: "journey-overview", label: "我的行程", panelId: "journey-overview" },
      { id: "journey-reminders", label: "我的行程提醒", panelId: "journey-reminders", sourceId: "passenger-transactions" },
    ],
    panels: [
      { id: "journey-overview", sourceId: "passenger-summary" },
      { id: "journey-reminders", sourceId: "passenger-transactions" },
    ],
  });

  setupPassengerTabGroup({
    sectionId: "passenger-orders",
    tabsLabel: "订单分页",
    defaultTabId: "order-status",
    tabs: [
      { id: "order-status", label: "订单状态", panelId: "order-status" },
      { id: "order-changes", label: "改签记录", panelId: "order-changes", sourceId: "passenger-changes" },
      { id: "order-payments", label: "支付记录", panelId: "order-payments", sourceId: "passenger-payments" },
      { id: "order-refunds", label: "退款记录", panelId: "order-refunds", sourceId: "passenger-refunds" },
    ],
    panels: [
      { id: "order-status", sourceId: "passenger-orders" },
      { id: "order-changes", sourceId: "passenger-changes" },
      { id: "order-payments", sourceId: "passenger-payments" },
      { id: "order-refunds", sourceId: "passenger-refunds" },
    ],
  });

  setupPassengerTabGroup({
    sectionId: "passenger-tickets",
    tabsLabel: "车票分页",
    defaultTabId: "ticket-all",
    tabs: [
      { id: "ticket-all", label: "全部车票", panelId: "ticket-list", filterTarget: "ticketStatus", filterValue: "" },
      { id: "ticket-issued", label: "有效票", panelId: "ticket-list", filterTarget: "ticketStatus", filterValue: "ISSUED" },
      { id: "ticket-refunded", label: "已退票", panelId: "ticket-list", filterTarget: "ticketStatus", filterValue: "REFUNDED" },
      { id: "ticket-cancelled", label: "已取消", panelId: "ticket-list", filterTarget: "ticketStatus", filterValue: "CANCELLED" },
    ],
    panels: [
      { id: "ticket-list", sourceId: "passenger-tickets" },
    ],
  });

  setupPassengerTabGroup({
    sectionId: "passenger-notifications",
    tabsLabel: "消息分页",
    defaultTabId: "notification-all",
    tabs: [
      { id: "notification-all", label: "全部消息", panelId: "notification-list", filterTarget: "notificationStatus", filterValue: "" },
      { id: "notification-read", label: "已读消息", panelId: "notification-list", filterTarget: "notificationStatus", filterValue: "READ" },
      { id: "notification-unread", label: "未读消息", panelId: "notification-list", filterTarget: "notificationStatus", filterValue: "UNREAD" },
    ],
    panels: [
      { id: "notification-list", sourceId: "passenger-notifications" },
    ],
  });
}

function getPassengerFeatureTabState() {
  if (!window.passengerFeatureTabState) {
    window.passengerFeatureTabState = {
      tabById: new Map(),
      tabBySectionId: new Map(),
      defaultTabBySectionId: new Map(),
    };
  }
  return window.passengerFeatureTabState;
}

function setupPassengerTabGroup(config) {
  const section = document.getElementById(config.sectionId);
  if (!section || section.dataset.passengerTabGroupReady === "true") {
    return;
  }
  const state = getPassengerFeatureTabState();
  const header = section.querySelector(":scope > .passenger-section-head");
  const tabList = document.createElement("div");
  const panelWrap = document.createElement("div");
  const tabMap = new Map();
  const panelMap = new Map();

  section.dataset.passengerTabGroupReady = "true";
  section.dataset.passengerDefaultTab = config.defaultTabId;
  section.classList.add("passenger-feature-tabbed");
  state.defaultTabBySectionId.set(config.sectionId, config.defaultTabId);

  tabList.className = "passenger-inner-tabs";
  tabList.setAttribute("role", "tablist");
  tabList.setAttribute("aria-label", config.tabsLabel || "功能分页");
  panelWrap.className = "passenger-tab-panels";

  (config.panels || []).forEach(panelConfig => {
    const panel = document.createElement("div");
    panel.className = "passenger-tab-panel";
    panel.dataset.passengerTabPanel = panelConfig.id;
    panel.setAttribute("role", "tabpanel");
    panel.hidden = true;
    panelMap.set(panelConfig.id, panel);
    panelWrap.appendChild(panel);
  });

  config.tabs.forEach(tabConfig => {
    const button = document.createElement("button");
    const panelId = tabConfig.panelId || tabConfig.id;
    button.className = "passenger-inner-tab";
    button.type = "button";
    button.dataset.passengerTab = tabConfig.id;
    button.setAttribute("role", "tab");
    button.setAttribute("aria-selected", "false");
    button.textContent = tabConfig.label;
    button.addEventListener("click", () => {
      activatePassengerFeatureTab(tabConfig.id, { userInitiated: true });
    });
    tabList.appendChild(button);
    tabMap.set(tabConfig.id, button);
    state.tabById.set(tabConfig.id, { ...tabConfig, sectionId: config.sectionId, panelId });
    if (tabConfig.sourceId) {
      state.tabBySectionId.set(tabConfig.sourceId, tabConfig.id);
    }
  });

  state.tabBySectionId.set(config.sectionId, config.defaultTabId);
  if (header) {
    header.insertAdjacentElement("afterend", tabList);
    tabList.insertAdjacentElement("afterend", panelWrap);
  } else {
    section.prepend(panelWrap);
    section.prepend(tabList);
  }

  (config.panels || []).forEach(panelConfig => {
    const panel = panelMap.get(panelConfig.id);
    const source = document.getElementById(panelConfig.sourceId);
    if (!panel || !source) {
      return;
    }
    movePassengerSectionBody(source, panel);
    if (source !== section) {
      source.hidden = true;
      source.classList.add("passenger-tab-source");
    }
  });

  section._passengerTabMap = tabMap;
  section._passengerPanelMap = panelMap;
  activatePassengerFeatureTab(config.defaultTabId, { silent: true, skipLoad: true });
}

function movePassengerSectionBody(source, panel) {
  Array.from(source.children).forEach(child => {
    if (
      child.classList.contains("passenger-section-head")
      || child.classList.contains("passenger-inner-tabs")
      || child.classList.contains("passenger-tab-panels")
    ) {
      return;
    }
    panel.appendChild(child);
  });
}

function activatePassengerFeatureTab(tabId, options = {}) {
  const state = getPassengerFeatureTabState();
  const tabConfig = state.tabById.get(tabId);
  if (!tabConfig) {
    return;
  }
  const section = document.getElementById(tabConfig.sectionId);
  if (!section) {
    return;
  }
  const activePanelId = tabConfig.panelId || tabId;
  section.querySelectorAll(".passenger-inner-tab").forEach(tab => {
    const isActive = tab.dataset.passengerTab === tabId;
    tab.classList.toggle("active", isActive);
    tab.setAttribute("aria-selected", isActive ? "true" : "false");
  });
  section.querySelectorAll(".passenger-tab-panel").forEach(panel => {
    panel.hidden = panel.dataset.passengerTabPanel !== activePanelId;
    panel.classList.toggle("active", !panel.hidden);
  });
  section.dataset.passengerActiveTab = tabId;
  applyPassengerFeatureTabFilter(tabConfig, options);
}

function applyPassengerFeatureTabFilter(tabConfig, options = {}) {
  if (!tabConfig.filterTarget || !elements[tabConfig.filterTarget]) {
    return;
  }
  const select = elements[tabConfig.filterTarget];
  if (select.value !== tabConfig.filterValue) {
    select.value = tabConfig.filterValue;
  }
  if (options.skipLoad) {
    return;
  }
  if (tabConfig.filterTarget === "ticketStatus") {
    passengerState.tickets.page = 0;
    loadPassengerTickets();
  }
  if (tabConfig.filterTarget === "notificationStatus") {
    passengerState.notifications.page = 0;
    loadPassengerNotifications();
  }
}

function syncPassengerFeatureTabForSection(sectionId) {
  const state = getPassengerFeatureTabState();
  const rootSectionId = normalizePassengerSectionId(navSectionAlias(sectionId));
  if (rootSectionId === "passenger-tickets") {
    activatePassengerFeatureTab(ticketTabIdForStatus(elements.ticketStatus.value), { silent: true, skipLoad: true });
    return;
  }
  if (rootSectionId === "passenger-notifications") {
    activatePassengerFeatureTab(notificationTabIdForStatus(elements.notificationStatus.value), { silent: true, skipLoad: true });
    return;
  }
  const tabId = state.tabBySectionId.get(sectionId) || state.defaultTabBySectionId.get(rootSectionId);
  if (tabId) {
    activatePassengerFeatureTab(tabId, { silent: true, skipLoad: true });
  }
}

function ticketTabIdForStatus(status) {
  if (status === "ISSUED") {
    return "ticket-issued";
  }
  if (status === "REFUNDED") {
    return "ticket-refunded";
  }
  if (status === "CANCELLED") {
    return "ticket-cancelled";
  }
  return "ticket-all";
}

function notificationTabIdForStatus(status) {
  if (status === "READ") {
    return "notification-read";
  }
  if (status === "UNREAD") {
    return "notification-unread";
  }
  return "notification-all";
}

async function registerPassenger() {
  const username = elements.registerUsername.value.trim();
  const password = elements.registerPassword.value;
  const confirmPassword = elements.registerConfirmPassword.value;
  elements.registerError.textContent = "";
  if (password !== confirmPassword) {
    elements.registerError.textContent = "两次输入的密码不一致";
    return;
  }
  try {
    const auth = await passengerRequest("/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username,
        password,
        confirmPassword,
        displayName: elements.registerDisplayName.value.trim() || username,
      }),
    }, false);
    if (auth.role !== "USER") {
      throw new Error("注册账号角色异常，请联系管理员");
    }
    passengerState.auth = auth;
    passengerState.authExpiredNotified = false;
    passengerState.showOnboarding = false;
    localStorage.setItem(PASSENGER_AUTH_KEY, JSON.stringify(auth));
    renderAuthState();
    showToast("注册成功，已进入乘客购票服务");
    await refreshPassengerData();
    await loadAvailablePassengerTrains();
    showOnboardingChoice();
  } catch (error) {
    elements.registerError.textContent = error.message || "注册失败";
    showToast(error.message || "注册失败");
  }
}

function switchPassengerAuthMode(mode) {
  const isRegister = mode === "register";
  elements.loginForm.hidden = isRegister;
  elements.registerForm.hidden = !isRegister;
  elements.showLogin.classList.toggle("active", !isRegister);
  elements.showRegister.classList.toggle("active", isRegister);
  elements.registerError.textContent = "";
}

function logoutPassenger() {
  passengerState.auth = null;
  passengerState.profile = null;
  passengerState.summary = null;
  passengerState.showOnboarding = false;
  localStorage.removeItem(PASSENGER_AUTH_KEY);
  sessionStorage.removeItem(PASSENGER_AUTH_KEY);
  renderAuthState();
  renderLoggedOutPlaceholders();
  showToast("已退出乘客端");
}

function showOnboardingChoice() {
  if (!elements.onboardingModal) {
    return;
  }
  elements.onboardingModal.classList.add("show");
  elements.onboardingModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("modal-open");
}

function closeOnboardingChoice() {
  if (!elements.onboardingModal) {
    return;
  }
  elements.onboardingModal.classList.remove("show");
  elements.onboardingModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("modal-open");
}

function startPassengerOnboarding() {
  passengerState.showOnboarding = true;
  setPassengerOnboardingDismissed(false);
  closeOnboardingChoice();
  renderPassengerOnboarding();
  activateSection("passenger-onboarding");
}

function skipPassengerOnboarding() {
  passengerState.showOnboarding = false;
  setPassengerOnboardingDismissed(true);
  closeOnboardingChoice();
  renderPassengerOnboarding();
  activateSection("passenger-search");
}

function dismissPassengerOnboarding() {
  passengerState.showOnboarding = false;
  setPassengerOnboardingDismissed(true);
  renderPassengerOnboarding();
  showToast("首单引导已关闭，后续可直接在查票和订单中操作");
  activateSection("passenger-summary");
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
    if (elements.apiStatus && elements.apiStatusText) {
      elements.apiStatus.className = "status-dot ok";
      elements.apiStatusText.textContent = `${health.service || "后端服务"} 已连接`;
    }
  } catch (error) {
    if (elements.apiStatus && elements.apiStatusText) {
      elements.apiStatus.className = "status-dot fail";
      elements.apiStatusText.textContent = "后端未连接";
    }
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
    loadPassengerProfile(),
    loadPassengerSummary(),
    loadPassengerTravelers(),
    loadPassengerOrders(),
    loadPassengerTickets(),
    loadPassengerPayments(),
    loadPassengerRefunds(),
    loadPassengerChanges(),
    loadPassengerTransactionSummary(),
    loadPassengerNotifications(),
    loadPassengerNotificationSummary(),
  ]);
}

async function refreshPassengerFlow(options = {}) {
  if (!ensureSignedIn() || passengerState.refreshingFlow) {
    return;
  }
  passengerState.refreshingFlow = true;
  document.body.classList.add("passenger-flow-refreshing");
  try {
    passengerState.orders.page = 0;
    passengerState.tickets.page = 0;
    passengerState.payments.page = 0;
    passengerState.refunds.page = 0;
    passengerState.changes.page = 0;
    passengerState.notifications.page = 0;
    await refreshPassengerData();
    const detailOrderId = options.detailOrderId || passengerState.activeDetailOrderId;
    if (detailOrderId) {
      await openPassengerOrderDetail(detailOrderId, { preserveScroll: true });
    }
  } finally {
    passengerState.refreshingFlow = false;
    window.setTimeout(() => document.body.classList.remove("passenger-flow-refreshing"), 240);
  }
}

async function loadPassengerProfile() {
  try {
    const profile = await passengerRequest("/passenger/profile");
    passengerState.profile = profile;
    renderPassengerProfile(profile);
    renderPassengerOnboarding();
  } catch (error) {
    showToast(error.message || "无法加载账号资料");
  }
}

function renderPassengerProfile(profile) {
  const displayName = profile.displayName || profile.username || "-";
  const username = profile.username ? `@${profile.username}` : "-";
  const role = roleText(profile.role || "USER");
  const defaultTraveler = profile.defaultTravelerName || "未设置";
  const orderCount = profile.orderCount || 0;
  const activeTicketCount = profile.activeTicketCount || 0;
  elements.profileRole.textContent = role;
  elements.profileDisplay.textContent = displayName;
  elements.profileUsername.textContent = username;
  elements.profileDefaultTraveler.textContent = defaultTraveler;
  elements.profileOrderCount.textContent = orderCount;
  elements.profileTicketCount.textContent = activeTicketCount;
  if (elements.profileDetailDisplay) {
    elements.profileDetailDisplay.textContent = displayName;
  }
  if (elements.profileDetailUsername) {
    elements.profileDetailUsername.textContent = username;
  }
  if (elements.profileDetailRole) {
    elements.profileDetailRole.textContent = role;
  }
  if (elements.profileDetailTraveler) {
    elements.profileDetailTraveler.textContent = defaultTraveler;
  }
  if (elements.profileDetailOrders) {
    elements.profileDetailOrders.textContent = orderCount;
  }
  if (elements.profileDetailTickets) {
    elements.profileDetailTickets.textContent = activeTicketCount;
  }
  elements.profileDisplayName.value = profile.displayName || "";
  if (profile.avatarAvailable) {
    loadPassengerAvatar();
  } else {
    renderPassengerAvatar(null);
  }
}

async function updatePassengerAvatar(event) {
  if (!ensureSignedIn()) {
    return;
  }
  const file = event.target.files && event.target.files[0];
  if (!file) {
    return;
  }
  elements.profileError.textContent = "";
  setPassengerAvatarStatus("");
  if (!file.type || !file.type.startsWith("image/")) {
    setPassengerAvatarStatus("请选择图片文件");
    showToast("请选择图片文件作为头像");
    event.target.value = "";
    return;
  }
  const formData = new FormData();
  formData.append("avatar", file);
  elements.avatarUploadButton.disabled = true;
  setPassengerAvatarStatus("正在上传头像...");
  try {
    const profile = await passengerRequest("/passenger/profile/avatar", {
      method: "POST",
      body: formData,
    });
    passengerState.profile = profile;
    renderPassengerProfile(profile);
    setPassengerAvatarStatus("头像已更新");
    showToast("头像已更新");
  } catch (error) {
    setPassengerAvatarStatus(error.message || "头像上传失败");
    showToast(error.message || "头像上传失败");
  } finally {
    elements.avatarUploadButton.disabled = false;
    event.target.value = "";
  }
}

async function deletePassengerAvatar() {
  if (!ensureSignedIn()) {
    return;
  }
  elements.avatarRemoveButton.disabled = true;
  setPassengerAvatarStatus("");
  try {
    await passengerRequest("/passenger/profile/avatar", { method: "DELETE" });
    if (passengerState.profile) {
      passengerState.profile.avatarAvailable = false;
      passengerState.profile.avatarUpdatedAt = null;
    }
    renderPassengerAvatar(null);
    setPassengerAvatarStatus("头像已移除");
    showToast("头像已移除");
  } catch (error) {
    setPassengerAvatarStatus(error.message || "头像移除失败");
    showToast(error.message || "头像移除失败");
  } finally {
    elements.avatarRemoveButton.disabled = false;
  }
}

async function loadPassengerAvatar() {
  if (!passengerState.auth || !passengerState.auth.token) {
    renderPassengerAvatar(null);
    return;
  }
  const requestId = passengerState.avatarRequestId + 1;
  passengerState.avatarRequestId = requestId;
  const version = passengerState.profile && passengerState.profile.avatarUpdatedAt
    ? `?v=${encodeURIComponent(passengerState.profile.avatarUpdatedAt)}`
    : "";
  try {
    const blob = await passengerBlobRequest(`/passenger/profile/avatar${version}`);
    if (requestId !== passengerState.avatarRequestId) {
      return;
    }
    renderPassengerAvatar(blob);
  } catch (error) {
    if (error.status === 404) {
      renderPassengerAvatar(null);
      return;
    }
    setPassengerAvatarStatus(error.message || "头像加载失败");
  }
}

function renderPassengerAvatar(blob) {
  if (passengerState.avatarObjectUrl) {
    URL.revokeObjectURL(passengerState.avatarObjectUrl);
    passengerState.avatarObjectUrl = null;
  }
  if (!blob) {
    applyPassengerAvatarImage(elements.topAvatar, null);
    applyPassengerAvatarImage(elements.profileAvatar, null);
    syncPassengerAvatarControls(false);
    return;
  }
  const objectUrl = URL.createObjectURL(blob);
  passengerState.avatarObjectUrl = objectUrl;
  applyPassengerAvatarImage(elements.topAvatar, objectUrl);
  applyPassengerAvatarImage(elements.profileAvatar, objectUrl);
  syncPassengerAvatarControls(true);
}

function applyPassengerAvatarImage(container, objectUrl) {
  if (!container) {
    return;
  }
  container.classList.toggle("has-image", Boolean(objectUrl));
  container.innerHTML = "";
  if (objectUrl) {
    const img = document.createElement("img");
    img.src = objectUrl;
    img.alt = "乘客头像";
    container.appendChild(img);
    return;
  }
  if (container === elements.topAvatar) {
    container.appendChild(createPassengerAccountIcon());
    return;
  }
  container.textContent = passengerAvatarInitials();
}

function createPassengerAccountIcon() {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("class", "person-avatar-icon");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("aria-hidden", "true");
  const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
  path.setAttribute("d", "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM5 20a7 7 0 0 1 14 0");
  svg.appendChild(path);
  return svg;
}

function passengerAvatarInitials() {
  const source = (passengerState.profile && (passengerState.profile.displayName || passengerState.profile.username))
    || (passengerState.auth && (passengerState.auth.displayName || passengerState.auth.username))
    || "RT";
  const text = String(source).trim();
  return text ? text.slice(0, 2).toUpperCase() : "RT";
}

function syncPassengerAvatarControls(hasAvatar) {
  if (elements.avatarRemoveButton) {
    elements.avatarRemoveButton.hidden = !hasAvatar;
  }
}

function setPassengerAvatarStatus(message) {
  if (elements.avatarStatus) {
    elements.avatarStatus.textContent = message || "";
  }
}

async function savePassengerProfile() {
  if (!ensureSignedIn()) {
    return;
  }
  const displayName = elements.profileDisplayName.value.trim();
  elements.profileError.textContent = "";
  if (!displayName) {
    elements.profileError.textContent = "请填写昵称";
    return;
  }
  try {
    const auth = await passengerRequest("/passenger/profile", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName }),
    });
    passengerState.auth = auth;
    localStorage.setItem(PASSENGER_AUTH_KEY, JSON.stringify(auth));
    renderAuthState();
    await loadPassengerProfile();
    showToast("个人资料已更新");
  } catch (error) {
    elements.profileError.textContent = error.message || "保存资料失败";
  }
}

async function changePassengerPassword() {
  if (!ensureSignedIn()) {
    return;
  }
  const oldPassword = elements.oldPassword.value;
  const newPassword = elements.newPassword.value;
  const confirmPassword = elements.confirmPassword.value;
  elements.passwordError.textContent = "";
  if (newPassword !== confirmPassword) {
    elements.passwordError.textContent = "两次输入的新密码不一致";
    return;
  }
  try {
    await passengerRequest("/passenger/password", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ oldPassword, newPassword, confirmPassword }),
    });
    elements.oldPassword.value = "";
    elements.newPassword.value = "";
    elements.confirmPassword.value = "";
    showToast("密码已更新，请妥善保管新密码");
  } catch (error) {
    elements.passwordError.textContent = error.message || "更新密码失败";
  }
}

async function loadPassengerSummary() {
  try {
    const summary = await passengerRequest("/passenger/summary");
    passengerState.summary = summary;
    const latestOrders = summary.latestOrders || [];
    const upcomingTrips = summary.upcomingTrips || [];
    await ensureOrdersRouteCache([...latestOrders, ...upcomingTrips]);
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
    renderMiniOrders(elements.latestOrders, latestOrders, "暂无最近订单");
    renderMiniOrders(elements.upcomingTrips, upcomingTrips, "暂无即将出行");
    renderPassengerOnboarding();
  } catch (error) {
    showToast(error.message || "无法加载乘客概览");
  }
}

async function loadPassengerTransactionSummary() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const summary = await passengerRequest("/passenger/transactions/summary");
    await ensureOrdersRouteCache(summary.latestOrders || []);
    renderPassengerTransactionSummary(summary);
  } catch (error) {
    elements.transactionStats.innerHTML = recordEmpty(error.message || "无法加载行程提醒");
    elements.transactionTodos.innerHTML = emptyItem("暂时无法加载待办事项");
    elements.transactionOrders.innerHTML = emptyItem("暂无订单动态");
    elements.transactionChanges.innerHTML = emptyItem("暂无改签动态");
  }
}

function renderPassengerTransactionSummary(summary) {
  const stats = [
    { label: "待支付订单", value: summary.pendingPaymentOrderCount || 0, target: "passenger-orders" },
    { label: "有效电子票", value: summary.activeTicketCount || 0, target: "passenger-tickets" },
    { label: "待处理改签", value: summary.pendingChangeCount || 0, target: "passenger-changes" },
    { label: "退票车票", value: summary.refundedTicketCount || 0, target: "passenger-tickets" },
    { label: "退款处理中", value: summary.pendingRefundCount || 0, target: "passenger-refunds" },
    { label: "未读消息", value: summary.unreadNotificationCount || 0, target: "passenger-notifications" },
  ];
  elements.transactionStats.innerHTML = stats.map(item => `
    <button class="transaction-stat-card clickable-card" type="button" data-transaction-target="${item.target}">
      <span>${escapeHtml(item.label)}</span>
      <strong>${item.value}</strong>
    </button>
  `).join("");
  elements.transactionStats.querySelectorAll("[data-transaction-target]").forEach(button => {
    button.addEventListener("click", () => activateSection(button.dataset.transactionTarget));
  });
  renderPassengerTodos(summary.todoItems || []);
  renderMiniOrders(elements.transactionOrders, summary.latestOrders || [], "暂无订单动态");
  renderMiniChanges(elements.transactionChanges, summary.latestChanges || [], "暂无改签动态");
}

function renderPassengerTodos(todos) {
  if (!todos.length) {
    elements.transactionTodos.innerHTML = emptyItem("暂无待办事项，当前行程状态平稳");
    return;
  }
  elements.transactionTodos.innerHTML = todos.map(todo => `
    <article class="passenger-todo-card ${escapeHtml((todo.priority || "").toLowerCase())}">
      <div>
        <span class="todo-type">${todoTypeText(todo.type || todo.actionTarget)}</span>
        <strong>${escapeHtml(todo.title || "-")}</strong>
        <small>${escapeHtml(todo.description || "-")}</small>
      </div>
      <button class="secondary-button compact-button" type="button"
        data-todo-target="${escapeHtml(todo.actionTarget || "")}"
        data-todo-order="${todo.orderId || ""}"
        data-todo-change="${todo.changeId || ""}">
        处理
      </button>
    </article>
  `).join("");
  elements.transactionTodos.querySelectorAll("[data-todo-target]").forEach(button => {
    button.addEventListener("click", () => handlePassengerTodo(button));
  });
}

async function handlePassengerTodo(button) {
  const target = button.dataset.todoTarget;
  const orderId = button.dataset.todoOrder;
  const changeId = button.dataset.todoChange;
  if (target === "ORDER_DETAIL" && orderId) {
    await openPassengerOrderDetail(orderId);
    return;
  }
  if (target === "CHANGE_PAY" && changeId) {
    await payPassengerChange(changeId);
    return;
  }
  if (target === "REFUNDS") {
    elements.refundStatus.value = "PENDING";
    passengerState.refunds.page = 0;
    activateSection("passenger-refunds");
    await loadPassengerRefunds();
    return;
  }
  if (target === "NOTIFICATIONS") {
    elements.notificationStatus.value = "UNREAD";
    passengerState.notifications.page = 0;
    activateSection("passenger-notifications");
    await loadPassengerNotifications();
    return;
  }
  activateSection("passenger-transactions");
}

function renderMiniChanges(container, changes, emptyText) {
  if (!changes.length) {
    container.innerHTML = emptyItem(emptyText);
    return;
  }
  container.innerHTML = changes.map(change => `
    <article class="mini-order change-mini-card">
      <div>
        <strong>${escapeHtml(change.changeNo || "-")}</strong>
        <span>${escapeHtml(change.originalTrainNo || "-")} → ${escapeHtml(change.newTrainNo || "-")} / ${changeStatusText(change.status)}</span>
      </div>
      <small>${formatDateTime(change.completedAt || change.createdAt) || "-"}</small>
    </article>
  `).join("");
}

function renderMiniOrders(container, orders, emptyText) {
  if (container === elements.upcomingTrips) {
    renderUpcomingJourneyTrips(container, orders, emptyText);
    return;
  }
  if (!orders.length) {
    container.innerHTML = emptyItem(emptyText);
    return;
  }
  container.innerHTML = orders.map(order => {
    const routeText = passengerOrderRouteText(order);
    return `
      <button class="event-item passenger-mini-order journey-row-order" type="button" data-order-status-jump="${escapeHtml(order.status || "")}">
        <div class="journey-row-main">
          <span class="journey-row-train">${escapeHtml(order.trainNo || "-")}</span>
          <strong>${escapeHtml(routeText)}</strong>
          <small>${formatDate(order.travelDate) || "-"} · ${seatTypeText(order.seatType)} · ${escapeHtml(order.passengerName || "-")}</small>
        </div>
        <div class="journey-row-side">
          <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
          <strong class="money">¥${formatAmount(order.amount)}</strong>
        </div>
      </button>
    `;
  }).join("");
  container.querySelectorAll("[data-order-status-jump]").forEach(item => {
    item.addEventListener("click", () => {
      elements.orderStatus.value = item.dataset.orderStatusJump || "";
      activateSection("passenger-orders");
      loadPassengerOrders();
    });
  });
}

function renderUpcomingJourneyTrips(container, orders, emptyText) {
  if (!orders.length) {
    container.innerHTML = `
      <div class="journey-trip-empty">
        <strong>${escapeHtml(emptyText)}</strong>
        <span>完成支付后，即将出行的车票会显示在这里。</span>
      </div>
    `;
    return;
  }
  container.innerHTML = orders.slice(0, 3).map(order => {
    const trip = passengerOrderTripInfo(order);
    const duration = journeyDurationText(trip.departureTime, trip.arrivalTime);
    const canOperate = order.status === "PAID";
    return `
      <article class="journey-trip-card">
        <div class="journey-trip-meta">
          <span class="journey-train-badge" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false">
              <path d="M7 5.5h10a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z"></path>
              <path d="M8 9h8M8 13h8M8 18l-2 2M16 18l2 2"></path>
            </svg>
          </span>
          <strong>${escapeHtml(trip.trainNo)}</strong>
          <span>计划出发 ${escapeHtml(formatJourneyDateLabel(order.travelDate))}</span>
        </div>
        <div class="journey-trip-route">
          <div class="journey-trip-stop">
            <strong>${escapeHtml(trip.departureStation)}</strong>
            <span>${escapeHtml(trip.departureTime)}</span>
          </div>
          <div class="journey-trip-duration">
            <strong>${escapeHtml(duration)}</strong>
            <span class="journey-trip-track" aria-hidden="true"></span>
          </div>
          <div class="journey-trip-stop is-arrival">
            <strong>${escapeHtml(trip.arrivalStation)}</strong>
            <span>${escapeHtml(trip.arrivalTime)}</span>
          </div>
        </div>
        <div class="journey-trip-actions">
          ${canOperate ? `<button class="secondary-button compact-button" type="button" data-journey-change="${order.id}">改签</button>` : ""}
          ${canOperate ? `<button class="secondary-button compact-button" type="button" data-journey-refund="${order.id}">退票</button>` : ""}
          <button class="primary-button compact-button" type="button" data-journey-detail="${order.id}">查看详情</button>
        </div>
      </article>
    `;
  }).join("");
  container.querySelectorAll("[data-journey-detail]").forEach(button => {
    button.addEventListener("click", () => openPassengerOrderDetail(button.dataset.journeyDetail));
  });
  container.querySelectorAll("[data-journey-change]").forEach(button => {
    button.addEventListener("click", () => openChangeFromOrderId(button.dataset.journeyChange));
  });
  container.querySelectorAll("[data-journey-refund]").forEach(button => {
    button.addEventListener("click", () => refundPassengerOrder(button.dataset.journeyRefund));
  });
}

function passengerOrderTripInfo(order) {
  const train = findTrainForOrder(order) || {};
  const departureStation = order.departureStation || order.fromStation || order.departureStationName
    || order.fromStationName || train.departureStation || "-";
  const arrivalStation = order.arrivalStation || order.toStation || order.arrivalStationName
    || order.toStationName || train.arrivalStation || "-";
  return {
    trainNo: order.trainNo || train.trainNo || "-",
    departureStation,
    arrivalStation,
    departureTime: formatTime(order.departureTime || train.departureTime),
    arrivalTime: formatTime(order.arrivalTime || train.arrivalTime),
  };
}

function journeyDurationText(start, end) {
  const startMinutes = timeToMinutes(start);
  const endMinutes = timeToMinutes(end);
  if (startMinutes === null || endMinutes === null) {
    return "行程";
  }
  let diff = endMinutes - startMinutes;
  if (diff < 0) {
    diff += 24 * 60;
  }
  const hours = Math.floor(diff / 60);
  const minutes = diff % 60;
  if (!hours) {
    return `${minutes}分钟`;
  }
  return minutes ? `${hours}时${minutes}分` : `${hours}小时`;
}

function timeToMinutes(value) {
  if (!value || value === "-") {
    return null;
  }
  const parts = String(value).split(":").map(Number);
  if (parts.length < 2 || Number.isNaN(parts[0]) || Number.isNaN(parts[1])) {
    return null;
  }
  return parts[0] * 60 + parts[1];
}

function formatJourneyDateLabel(value) {
  const dateText = formatDate(value);
  if (!dateText || dateText === "-") {
    return "-";
  }
  const date = new Date(`${dateText}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return dateText;
  }
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const weekdays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
  return `${month}月${day}日 ${weekdays[date.getDay()]}`;
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
    renderPassengerOnboarding();
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

function renderPassengerOnboarding() {
  if (!passengerState.auth || !elements.onboardingSteps || !elements.onboardingSection) {
    if (elements.onboardingSection) {
      elements.onboardingSection.hidden = true;
    }
    syncPassengerOnboardingNav(true);
    return;
  }
  const dismissed = isPassengerOnboardingDismissed();
  elements.onboardingSection.hidden = !passengerState.showOnboarding || dismissed;
  syncPassengerOnboardingNav(elements.onboardingSection.hidden);
  if (elements.onboardingSection.hidden) {
    elements.onboardingSteps.innerHTML = "";
    elements.onboardingAction.dataset.target = "passenger-search";
    elements.onboardingAction.textContent = "开始第一段行程";
    return;
  }
  const summary = passengerState.summary || {};
  const profile = passengerState.profile || {};
  const totalOrders = Number(summary.pendingPaymentOrderCount || 0)
    + Number(summary.paidOrderCount || 0)
    + Number(summary.closedOrderCount || 0)
    + Number(summary.refundedOrderCount || 0);
  const hasPaid = Number(summary.paidOrderCount || 0) > 0
    || Number(summary.paymentCount || 0) > 0
    || Number(profile.activeTicketCount || 0) > 0;
  const steps = [
    {
      key: "profile",
      title: "账号已创建",
      desc: profile.username ? `当前账号 @${profile.username}` : "注册或登录乘客账号",
      done: Boolean(profile.username),
      target: "passenger-account",
    },
    {
      key: "traveler",
      title: "添加常用乘车人",
      desc: passengerState.travelers.length ? `已维护 ${passengerState.travelers.length} 位乘车人` : "先维护实名乘车人，购票时可直接选择",
      done: passengerState.travelers.length > 0,
      target: "passenger-travelers",
    },
    {
      key: "order",
      title: "查询车票并下单",
      desc: totalOrders > 0 ? `已有 ${totalOrders} 笔订单` : "选择车次后提交第一笔待支付订单",
      done: totalOrders > 0,
      target: "passenger-search",
    },
    {
      key: "pay",
      title: "完成支付",
      desc: hasPaid ? "已产生支付记录或有效电子票" : "在我的订单中支付待支付订单",
      done: hasPaid,
      target: "passenger-orders",
    },
    {
      key: "ticket",
      title: "查看电子票",
      desc: Number(profile.activeTicketCount || 0) > 0 ? `有效电子票 ${profile.activeTicketCount} 张` : "支付后到电子票夹查看票面",
      done: Number(profile.activeTicketCount || 0) > 0,
      target: "passenger-tickets",
    },
  ];
  const allDone = steps.every(step => step.done);
  if (allDone) {
    const wasActive = elements.onboardingSection.classList.contains("is-active");
    passengerState.showOnboarding = false;
    setPassengerOnboardingDismissed(true);
    elements.onboardingSection.hidden = true;
    elements.onboardingSteps.innerHTML = "";
    elements.onboardingAction.dataset.target = "passenger-search";
    elements.onboardingAction.textContent = "开始第一段行程";
    syncPassengerOnboardingNav(true);
    if (wasActive) {
      activateSection("passenger-summary", { silent: true });
    }
    return;
  }
  elements.onboardingSteps.innerHTML = steps.map((step, index) => `
    <button class="onboarding-step ${step.done ? "done" : ""}" type="button" data-onboarding-target="${step.target}">
      <span class="step-index">${step.done ? "✓" : index + 1}</span>
      <span><strong>${escapeHtml(step.title)}</strong><small>${escapeHtml(step.desc)}</small></span>
    </button>
  `).join("");
  elements.onboardingSteps.querySelectorAll("[data-onboarding-target]").forEach(button => {
    button.addEventListener("click", () => activateSection(button.dataset.onboardingTarget));
  });
  const next = steps.find(step => !step.done) || steps[steps.length - 1];
  elements.onboardingAction.dataset.target = allDone ? "dismiss-onboarding" : next.target;
  elements.onboardingAction.textContent = allDone ? "我已知晓" : next.title;
}

function syncPassengerOnboardingNav(hidden) {
  if (!elements.onboardingNav) {
    return;
  }
  const dismissed = passengerState.auth ? isPassengerOnboardingDismissed() : true;
  const shouldHide = Boolean(
    hidden
    || dismissed
    || !passengerState.showOnboarding
    || !elements.onboardingSection
    || elements.onboardingSection.hidden
  );
  elements.onboardingNav.hidden = shouldHide;
  elements.onboardingNav.setAttribute("aria-hidden", shouldHide ? "true" : "false");
  elements.onboardingNav.tabIndex = shouldHide ? -1 : 0;
  if (shouldHide) {
    elements.onboardingNav.style.setProperty("display", "none", "important");
  } else {
    elements.onboardingNav.style.removeProperty("display");
  }
  elements.onboardingNav.classList.toggle("active", false);
}

function passengerOnboardingKey() {
  const username = passengerState.auth && passengerState.auth.username;
  return `${PASSENGER_ONBOARDING_KEY_PREFIX}${username || "anonymous"}`;
}

function isPassengerOnboardingDismissed() {
  try {
    return localStorage.getItem(passengerOnboardingKey()) === "true";
  } catch (error) {
    return false;
  }
}

function setPassengerOnboardingDismissed(value) {
  try {
    localStorage.setItem(passengerOnboardingKey(), value ? "true" : "false");
  } catch (error) {
    // Ignore local storage failures; the guide can still be controlled for the current session.
  }
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
  cachePassengerTrains(trains);
  passengerState.trains = { ...passengerState.trains, items: trains, page: 0 };
  renderTrainPage();
}

function cachePassengerTrains(trains) {
  passengerState.trainByInventory = { ...passengerState.trainByInventory };
  trains.forEach(train => {
    passengerState.trainByInventory[String(train.inventoryId)] = train;
  });
}

async function ensureOrderRouteCache(order) {
  if (!order || !order.trainNo || hasOrderRouteInfo(order)) {
    return;
  }
  try {
    const params = new URLSearchParams();
    if (order.travelDate) {
      params.set("travelDate", order.travelDate);
    }
    params.set("page", "0");
    params.set("size", "100");
    const trains = await passengerRequest(`/trains/available?${params.toString()}`);
    if (Array.isArray(trains)) {
      cachePassengerTrains(trains);
    }
  } catch (error) {
    // Route cache is only a display enhancement for pending orders.
  }
}

async function ensureOrdersRouteCache(orders = []) {
  if (!Array.isArray(orders) || !orders.length) {
    return;
  }
  const pending = [];
  const seen = new Set();
  orders.forEach(order => {
    if (!order || !order.trainNo || hasOrderRouteInfo(order)) {
      return;
    }
    const key = `${order.trainNo || ""}::${order.travelDate || ""}`;
    if (seen.has(key)) {
      return;
    }
    seen.add(key);
    pending.push(order);
  });
  if (!pending.length) {
    return;
  }
  await Promise.all(pending.map(order => ensureOrderRouteCache(order)));
}

function hasOrderRouteInfo(order) {
  const trip = passengerOrderTripInfo(order || {});
  return trip.departureStation !== "-" && trip.arrivalStation !== "-";
}

function findTrainForOrder(order) {
  if (!order || !order.trainNo) {
    return null;
  }
  const trains = [
    ...(passengerState.trains && Array.isArray(passengerState.trains.items) ? passengerState.trains.items : []),
    ...Object.values(passengerState.trainByInventory || {}),
  ];
  return trains.find(train => train && train.trainNo === order.trainNo
    && (!order.travelDate || !train.travelDate || train.travelDate === order.travelDate)) || null;
}

function buildTrainGroupKey(train) {
  return [
    train.trainNo,
    train.departureStation,
    train.arrivalStation,
    train.travelDate,
    train.departureTime,
    train.arrivalTime,
  ].map(value => (value == null ? "" : String(value))).join("::");
}

function groupPassengerTrainOptions(items) {
  const groups = new Map();
  items.forEach(train => {
    if (!train) {
      return;
    }
    const groupKey = buildTrainGroupKey(train);
    const price = Number(train.price);
    const remainingSeats = Number(train.remainingSeats || 0);
    let group = groups.get(groupKey);
    if (!group) {
      group = {
        ...train,
        groupKey,
        inventoryOptions: [],
        minPrice: Number.isFinite(price) ? price : 0,
        minPriceInventory: train,
        remainingSeats: 0,
        totalRemainingSeats: 0,
      };
      groups.set(groupKey, group);
    }
    group.inventoryOptions.push(train);
    group.totalRemainingSeats += Number.isFinite(remainingSeats) ? remainingSeats : 0;
    const currentMinPrice = Number(group.minPriceInventory && group.minPriceInventory.price);
    if (Number.isFinite(price) && (!Number.isFinite(currentMinPrice) || price < currentMinPrice)) {
      group.minPrice = price;
      group.minPriceInventory = train;
    }
  });

  passengerState.trainGroupByKey = {};
  groups.forEach(group => {
    group.inventoryOptions.sort((left, right) => {
      const leftPrice = Number(left.price);
      const rightPrice = Number(right.price);
      if (Number.isFinite(leftPrice) && Number.isFinite(rightPrice) && leftPrice !== rightPrice) {
        return leftPrice - rightPrice;
      }
      return seatTypeSortValue(left.seatType) - seatTypeSortValue(right.seatType);
    });
    group.defaultInventory = group.minPriceInventory || group.inventoryOptions[0] || group;
    group.price = Number.isFinite(Number(group.defaultInventory.price)) ? Number(group.defaultInventory.price) : group.minPrice;
    group.remainingSeats = group.totalRemainingSeats;
    passengerState.trainGroupByKey[group.groupKey] = group;
  });

  return Array.from(groups.values());
}

function seatTypeSortValue(seatType) {
  const type = String(seatType || "").toUpperCase();
  if (type === "SECOND_CLASS") {
    return 1;
  }
  if (type === "FIRST_CLASS") {
    return 2;
  }
  return 9;
}

function renderTrainPage() {
  const { items, page, size } = passengerState.trains;
  if (!items.length) {
    renderTrainEmpty("当前线路暂无可售车次，可查看全部可购车次或选择热门线路。");
    return;
  }
  const displayItems = groupPassengerTrainOptions(items);
  const totalPages = Math.max(1, Math.ceil(displayItems.length / size));
  const safePage = Math.min(Math.max(page, 0), totalPages - 1);
  passengerState.trains.page = safePage;
  const start = safePage * size;
  const visibleTrains = displayItems.slice(start, start + size);
  const controls = `
    <div class="train-list-toolbar">
      <div>
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
      </div>
      <div class="ticket-card-time ticket-card-journey">
        <div class="ticket-station ticket-station-from">
          <strong>${escapeHtml(train.departureStation)}</strong>
          <span>${formatTime(train.departureTime)}</span>
        </div>
        <div class="ticket-rail-line">
          <span>${formatDate(train.travelDate)}</span>
        </div>
        <div class="ticket-station ticket-station-to">
          <strong>${escapeHtml(train.arrivalStation)}</strong>
          <span>${formatTime(train.arrivalTime)}</span>
        </div>
      </div>
      <div class="ticket-card-meta">
        <span class="ticket-min-price-label">最低价</span>
        <span class="${Number(train.remainingSeats || 0) <= 5 ? "inventory-low" : "inventory-ok"}">余票 ${train.remainingSeats}</span>
      </div>
      <div class="ticket-card-action">
        <span class="ticket-price">¥${formatAmount(train.price)} 起</span>
        <button class="primary-button compact-button ticket-buy-button" type="button" data-passenger-buy="${escapeHtml(train.groupKey)}">购票</button>
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

function getBuyInventoryOptions(train) {
  if (!train) {
    return [];
  }
  if (Array.isArray(train.inventoryOptions) && train.inventoryOptions.length) {
    return train.inventoryOptions;
  }
  return [train];
}

function getSelectedBuyInventory() {
  const train = passengerState.selectedTrain;
  const options = getBuyInventoryOptions(train);
  if (!options.length) {
    return null;
  }
  const selectedId = elements.buySeatType ? elements.buySeatType.value : "";
  return options.find(option => String(option.inventoryId) === String(selectedId))
    || passengerState.selectedInventory
    || train.defaultInventory
    || train.minPriceInventory
    || options[0];
}

function renderBuySeatOptions(train) {
  if (!elements.buySeatType) {
    return;
  }
  const options = getBuyInventoryOptions(train);
  const availableOptions = options.filter(option => Number(option.remainingSeats || 0) > 0);
  const defaultInventory = availableOptions[0] || train.defaultInventory || train.minPriceInventory || options[0] || null;
  elements.buySeatType.innerHTML = options.map(option => {
    const remainingSeats = Number(option.remainingSeats || 0);
    return `
      <option value="${option.inventoryId}" ${remainingSeats <= 0 ? "disabled" : ""}>
        ${seatTypeText(option.seatType)} / ¥${formatAmount(option.price)} / 余票 ${remainingSeats}
      </option>
    `;
  }).join("");
  if (defaultInventory) {
    elements.buySeatType.value = String(defaultInventory.inventoryId);
  }
  elements.buySeatType.disabled = options.length <= 1;
  passengerState.selectedInventory = defaultInventory;
}

function renderBuySummary(train, inventory) {
  const activeInventory = inventory || getSelectedBuyInventory() || train;
  const remainingSeats = Number(activeInventory && activeInventory.remainingSeats || 0);
  elements.buySummary.innerHTML = `
    <div class="buy-route">
      <strong>${escapeHtml(train.trainNo)}</strong>
      <span>${escapeHtml(train.departureStation)} → ${escapeHtml(train.arrivalStation)}</span>
    </div>
    <div class="buy-detail-grid">
      <div><span>乘车日期</span><strong>${formatDate(train.travelDate)}</strong></div>
      <div><span>发车时间</span><strong>${formatTime(train.departureTime)}</strong></div>
      <div><span>到达时间</span><strong>${formatTime(train.arrivalTime)}</strong></div>
      <div><span>席别</span><strong>${seatTypeText(activeInventory.seatType)}</strong></div>
      <div><span>票价</span><strong class="money">¥${formatAmount(activeInventory.price)}</strong></div>
      <div><span>剩余票数</span><strong>${remainingSeats}</strong></div>
    </div>
  `;
}

function updateSelectedBuySeat() {
  if (!passengerState.selectedTrain) {
    return;
  }
  passengerState.selectedInventory = getSelectedBuyInventory();
  renderBuySummary(passengerState.selectedTrain, passengerState.selectedInventory);
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
  const train = passengerState.trainGroupByKey[String(inventoryId)] || passengerState.trainByInventory[String(inventoryId)];
  if (!train) {
    showToast("未找到当前车次库存，请重新查询");
    return;
  }
  passengerState.selectedTrain = train;
  renderBuySeatOptions(train);
  renderBuySummary(train, passengerState.selectedInventory);
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
  passengerState.selectedInventory = null;
}

async function submitPassengerOrder() {
  const train = passengerState.selectedTrain;
  const inventory = getSelectedBuyInventory();
  if (!train) {
    return;
  }
  if (!inventory) {
    elements.buyError.textContent = "请选择可购席别";
    return;
  }
  if (Number(inventory.remainingSeats || 0) <= 0) {
    elements.buyError.textContent = "当前席别暂无余票，请选择其他席别";
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
      trainId: Number(inventory.trainId || train.trainId),
      inventoryId: Number(inventory.inventoryId),
    };
    if (selectedTravelerId) {
      body.travelerId = Number(selectedTravelerId);
    } else {
      body.passengerName = passengerName;
      body.passengerIdCard = passengerIdCard;
      body.passengerIdType = elements.buyIdType.value;
      body.passengerPhone = elements.buyPhone.value.trim();
    }
    const order = await passengerRequest("/passenger/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    closeBuyModal();
    showToast("订单已创建，库存已锁定，请及时支付");
    await refreshPassengerFlow({ detailOrderId: order && order.id });
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
    const orders = page.content || [];
    await ensureOrdersRouteCache(orders);
    renderPassengerOrders(orders);
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
  elements.orderCards.innerHTML = orders.map(order => {
    const routeText = passengerOrderRouteText(order);
    return `
    <article class="passenger-order-card">
      <div class="passenger-order-main">
        <div>
          <p class="eyebrow">${escapeHtml(order.orderNo)}</p>
          <h3>${escapeHtml(order.trainNo)} · ${escapeHtml(routeText)}</h3>
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
  `}).join("");
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
  elements.orderCards.querySelectorAll("[data-change-order]").forEach(button => {
    const order = orders.find(item => String(item.id) === String(button.dataset.changeOrder));
    button.addEventListener("click", () => openChangeModal(order));
  });
  elements.orderCards.querySelectorAll("[data-jump-refunds]").forEach(button => {
    button.addEventListener("click", () => {
      activateSection("passenger-refunds");
      loadPassengerRefunds();
    });
  });
}

function passengerOrderRouteText(order) {
  const train = findTrainForOrder(order) || {};
  const departureStation = order.departureStation || order.fromStation || order.departureStationName
    || order.fromStationName || train.departureStation || "";
  const arrivalStation = order.arrivalStation || order.toStation || order.arrivalStationName
    || order.toStationName || train.arrivalStation || "";
  return departureStation && arrivalStation
    ? `${departureStation} → ${arrivalStation}`
    : `${order.trainNo || "-"} · ${formatDate(order.travelDate) || "-"}`;
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

async function openPassengerOrderDetail(orderId, options = {}) {
  try {
    passengerState.activeDetailOrderId = orderId;
    const detail = await passengerRequest(`/passenger/orders/${orderId}/detail`);
    await ensureOrderRouteCache(detail.order);
    showPassengerOrderDetail(detail);
    if (!options.preserveScroll) {
      const body = document.querySelector("#passenger-order-detail-modal .order-detail-body");
      if (body) {
        body.scrollTop = 0;
      }
    }
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
  const ticketChanges = detail.ticketChanges || [];
  const notifications = detail.notifications || [];
  modal.querySelector(".order-detail-body").innerHTML = `
    <div class="detail-hero order-chain-hero">
      <div>
        <p class="eyebrow">订单详情</p>
        <h2>${escapeHtml(order.trainNo || "-")} / ${escapeHtml(order.orderNo || "-")}</h2>
        <span>${formatDate(order.travelDate)} · ${seatTypeText(order.seatType)} · ${escapeHtml(order.passengerName || "-")}</span>
      </div>
      <div class="detail-amount">
        <strong>¥${formatAmount(order.amount)}</strong>
        <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
      </div>
    </div>
    <section class="detail-section ticket-detail-priority-section">
      <h3>车票与行程</h3>
      ${ticket ? renderTicketDetail(ticket) : renderOrderItineraryFallback(order)}
    </section>
    ${renderOrderDetailSummary(order, ticket, payments, refunds, ticketChanges)}
    ${renderOrderDetailActions(order, ticket, refunds, ticketChanges)}
    <div class="detail-center-layout passenger-order-center">
      <div class="detail-center-main">
        <section class="detail-section transaction-chain-section">
          <h3>订单进度</h3>
          ${renderFullOrderTimeline(order, ticket, payments, refunds, ticketChanges, notifications)}
        </section>
      </div>
      <aside class="detail-center-side">
        <section class="detail-section">
          <h3>支付记录</h3>
          ${renderDetailRecords(payments, payment => renderPaymentDetailRecord(payment), "暂无支付记录")}
        </section>
        <section class="detail-section">
          <h3>退款记录</h3>
          ${renderDetailRecords(refunds, refund => renderRefundDetailRecord(refund), "暂无退款记录")}
        </section>
        <section class="detail-section">
          <h3>改签记录</h3>
          ${renderDetailRecords(ticketChanges, change => renderChangeDetailRecord(change), "暂无改签记录")}
        </section>
        <section class="detail-section">
          <h3>消息提醒</h3>
          ${renderDetailRecords(notifications.slice(0, 4), notification => renderNotificationDetailRecord(notification), "暂无消息提醒")}
        </section>
      </aside>
    </div>
  `;
  bindOrderDetailActions(modal);
  openDetailModal(modal);
}

function renderTicketDetail(ticket) {
  return `
    <div class="ticket-itinerary ticket-detail-card ${ticketStatusClass(ticket.status)}">
      <div class="ticket-detail-route">
        <strong>${escapeHtml(ticket.departureStation)}</strong>
        <span></span>
        <strong>${escapeHtml(ticket.arrivalStation)}</strong>
      </div>
      <div class="ticket-detail-grid">
        <div><span>车次</span><strong>${escapeHtml(ticket.trainNo || "-")}</strong></div>
        <div><span>乘车日期</span><strong>${formatDate(ticket.travelDate) || "-"}</strong></div>
        <div><span>出发 / 到达</span><strong>${formatTime(ticket.departureTime)} - ${formatTime(ticket.arrivalTime)}</strong></div>
        <div><span>席别</span><strong>${seatTypeText(ticket.seatType)}</strong></div>
        <div><span>乘车人</span><strong>${escapeHtml(ticket.passengerName)} / ${idTypeText(ticket.passengerIdType)}</strong></div>
        <div><span>证件</span><strong>${escapeHtml(ticket.passengerIdCardMasked || "-")}</strong></div>
        <div><span>手机号</span><strong>${escapeHtml(ticket.passengerPhoneMasked || "-")}</strong></div>
        <div><span>出票时间</span><strong>${formatDateTime(ticket.issuedAt) || "-"}</strong></div>
      </div>
      <p class="ticket-state-inline ${ticketStatusClass(ticket.status)}">${escapeHtml(ticketStateMessage(ticket))}</p>
      ${ticket.invalidatedAt ? `<div class="ticket-detail-note">票面失效时间：${formatDateTime(ticket.invalidatedAt)}</div>` : ""}
    </div>
  `;
}

function renderOrderDetailSummary(order, ticket, payments, refunds, ticketChanges = []) {
  const successPayment = payments.find(payment => payment.status === "SUCCESS");
  const latestPayment = payments[0];
  const latestRefund = refunds[0];
  const latestChange = ticketChanges[0];
  const cards = [
    {
      className: "",
      label: "订单状态",
      value: statusText(order.status),
      note: order.updatedAt ? `更新于 ${formatDateTime(order.updatedAt)}` : "跟随订单实时更新",
    },
    {
      className: ticketStatusClass(ticket && ticket.status),
      label: "电子票",
      value: ticket ? ticketStatusText(ticket.status) : "未出票",
      note: ticket ? ticket.ticketNo : "支付成功后自动生成",
    },
    {
      className: "",
      label: "支付状态",
      value: successPayment ? "已支付" : (latestPayment ? paymentStatusText(latestPayment.status) : "暂无支付"),
      note: successPayment ? successPayment.paymentNo : "待支付订单可继续完成支付",
    },
  ];
  if (latestRefund) {
    cards.push({
      className: refundStatusClass(latestRefund.status),
      label: "退款状态",
      value: refundStatusText(latestRefund.status),
      note: latestRefund.refundNo,
    });
  }
  if (latestChange) {
    cards.push({
      className: changeStatusClass(latestChange.status),
      label: "改签状态",
      value: changeStatusText(latestChange.status),
      note: latestChange.changeNo,
    });
  }
  return `
    <section class="detail-status-grid" aria-label="订单状态总览">
      ${cards.map(card => `
        <article class="${card.className}">
          <span>${escapeHtml(card.label)}</span>
          <strong>${escapeHtml(card.value)}</strong>
          <small>${escapeHtml(card.note || "-")}</small>
        </article>
      `).join("")}
    </section>
  `;
}

function renderOrderDetailActions(order, ticket, refunds = [], ticketChanges = []) {
  const actions = [];
  const pendingChange = ticketChanges.find(change => change.status === "PENDING_PAYMENT");
  if (order.status === "PENDING_PAYMENT") {
    actions.push(`<button class="primary-button compact-button" type="button" data-detail-pay-order="${order.id}">立即支付</button>`);
    actions.push(`<button class="secondary-button compact-button" type="button" data-detail-close-order="${order.id}">取消订单</button>`);
  }
  if (order.status === "PAID") {
    actions.push(`<button class="secondary-button compact-button" type="button" data-detail-change-order="${order.id}">申请改签</button>`);
    actions.push(`<button class="secondary-button compact-button danger-soft" type="button" data-detail-refund-order="${order.id}">申请退票</button>`);
  }
  if (pendingChange) {
    actions.push(`<button class="primary-button compact-button" type="button" data-detail-pay-change="${pendingChange.id}" data-detail-pay-change-order="${order.id}">支付改签差额</button>`);
  }
  if (ticket) {
    actions.push(`<button class="secondary-button compact-button" type="button" data-detail-jump="passenger-tickets">查看电子票</button>`);
  }
  if (refunds.length) {
    actions.push(`<button class="secondary-button compact-button" type="button" data-detail-jump="passenger-refunds">查看退款</button>`);
  }
  actions.push(`<button class="secondary-button compact-button" type="button" data-detail-refresh-order="${order.id}">刷新详情</button>`);
  return `
    <section class="detail-action-strip" aria-label="订单操作">
      <div>
        <strong>${orderActionTitle(order.status)}</strong>
        <span>${orderActionHint(order.status)}</span>
      </div>
      <div class="inline-actions">${actions.join("")}</div>
    </section>
  `;
}

function renderOrderItineraryFallback(order) {
  const train = findTrainForOrder(order);
  const departureStation = order.departureStation || order.fromStation || order.departureStationName || order.fromStationName
    || (train && train.departureStation) || "-";
  const arrivalStation = order.arrivalStation || order.toStation || order.arrivalStationName || order.toStationName
    || (train && train.arrivalStation) || "-";
  return `
    <div class="ticket-itinerary ticket-detail-card muted-ticket-card order-flat-detail-card">
      <div class="ticket-detail-main">
        <div>
          <span>订单号</span>
          <strong>${escapeHtml(order.orderNo || "-")}</strong>
        </div>
        <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
      </div>
      <div class="ticket-detail-route">
        <strong>${escapeHtml(departureStation)}</strong>
        <span></span>
        <strong>${escapeHtml(arrivalStation)}</strong>
      </div>
      <div class="order-flat-detail-list">
        <div><span>金额</span><strong>¥${formatAmount(order.amount)}</strong></div>
        <div><span>席别</span><strong>${seatTypeText(order.seatType)}</strong></div>
        <div><span>乘车人</span><strong>${escapeHtml(order.passengerName || "-")}</strong></div>
        <div><span>证件</span><strong>${escapeHtml(order.passengerIdNoMasked || "-")}</strong></div>
        <div><span>支付截止</span><strong>${formatDateTime(order.paymentDeadlineAt) || "-"}</strong></div>
      </div>
      <div class="ticket-state-note">支付成功后会自动生成电子票，并同步到电子票夹。</div>
    </div>
  `;
}

function renderPaymentDetailRecord(payment) {
  return `
    <div class="detail-record chain-record ${paymentStatusClass(payment.status)}">
      <span>支付流水</span>
      <strong>${escapeHtml(payment.paymentNo)}</strong>
      <small>${paymentStatusText(payment.status)} · ¥${formatAmount(payment.amount)} · ${formatDateTime(payment.paidAt || payment.updatedAt || payment.createdAt) || "-"}</small>
      <small>渠道：${escapeHtml(payment.channelPaymentNo || payment.channel || "-")}</small>
    </div>
  `;
}

function renderRefundDetailRecord(refund) {
  return `
    <div class="detail-record chain-record ${refundStatusClass(refund.status)}">
      <span>退款流水</span>
      <strong>${escapeHtml(refund.refundNo)}</strong>
      <small>${refundStatusText(refund.status)} · ¥${formatAmount(refund.amount)} · ${formatDateTime(refund.refundedAt || refund.updatedAt || refund.createdAt) || "-"}</small>
      <small>关联支付：${escapeHtml(refund.paymentNo || "-")}</small>
    </div>
  `;
}

function renderChangeDetailRecord(change) {
  const diff = formatSignedAmount(change.priceDifference);
  return `
    <div class="detail-record chain-record ${changeStatusClass(change.status)}">
      <span>改签单</span>
      <strong>${escapeHtml(change.changeNo)}</strong>
      <small>${escapeHtml(change.originalTrainNo || "-")} → ${escapeHtml(change.newTrainNo || "-")} · ${changeStatusText(change.status)} · ${diff}</small>
      <small>${formatDateTime(change.completedAt || change.updatedAt || change.createdAt) || "-"}</small>
    </div>
  `;
}

function renderNotificationDetailRecord(notification) {
  return `
    <div class="detail-record chain-record ${notificationStatusClass(notification.status)}">
      <span>${notificationTypeText(notification.type)}</span>
      <strong>${escapeHtml(notificationTitleText(notification))}</strong>
      <small>${escapeHtml(notificationContentText(notification))}</small>
      <small>${formatDateTime(notification.createdAt) || "-"}</small>
    </div>
  `;
}

function bindOrderDetailActions(modal) {
  modal.querySelectorAll("[data-detail-pay-order]").forEach(button => {
    button.addEventListener("click", () => runOrderDetailAction(button.dataset.detailPayOrder, "pay"));
  });
  modal.querySelectorAll("[data-detail-close-order]").forEach(button => {
    button.addEventListener("click", () => runOrderDetailAction(button.dataset.detailCloseOrder, "close"));
  });
  modal.querySelectorAll("[data-detail-refund-order]").forEach(button => {
    button.addEventListener("click", () => runOrderDetailAction(button.dataset.detailRefundOrder, "refund"));
  });
  modal.querySelectorAll("[data-detail-change-order]").forEach(button => {
    button.addEventListener("click", () => {
      const orderId = button.dataset.detailChangeOrder;
      closeDetailModal(modal);
      openChangeFromOrderId(orderId);
    });
  });
  modal.querySelectorAll("[data-detail-pay-change]").forEach(button => {
    button.addEventListener("click", async () => {
      await payPassengerChange(button.dataset.detailPayChange, {
        stayOnDetail: true,
        orderId: button.dataset.detailPayChangeOrder,
      });
    });
  });
  modal.querySelectorAll("[data-detail-jump]").forEach(button => {
    button.addEventListener("click", () => {
      closeDetailModal(modal);
      activateSection(button.dataset.detailJump);
    });
  });
  modal.querySelectorAll("[data-detail-refresh-order]").forEach(button => {
    button.addEventListener("click", () => refreshPassengerFlow({ detailOrderId: button.dataset.detailRefreshOrder }));
  });
}

async function runOrderDetailAction(orderId, action) {
  const config = {
    pay: { path: `/passenger/orders/${orderId}/pay`, success: "支付成功，电子票已更新" },
    close: { path: `/passenger/orders/${orderId}/close`, success: "订单已取消，库存已释放" },
    refund: { path: `/passenger/orders/${orderId}/refund`, success: "退票成功，电子票已标记失效" },
  }[action];
  if (!config) {
    return;
  }
  try {
    await passengerRequest(config.path, { method: "POST" });
    showToast(config.success);
    await refreshPassengerFlow({ detailOrderId: orderId });
  } catch (error) {
    showToast(error.message || "订单操作失败");
  }
}

function renderTicketStateNote(ticket) {
  const status = ticket && ticket.status;
  return `<div class="ticket-state-note ${ticketStatusClass(status)}">${escapeHtml(ticketStateMessage(ticket))}</div>`;
}

function ticketStateMessage(ticket) {
  const status = ticket && ticket.status;
  const message = {
    ISSUED: "当前电子票有效。请以乘车日期、车次和站点信息为准，进站时核对本人证件。",
    REFUNDED: "该电子票已随退票失效，不能继续用于乘车。退款进度请查看退款流水。",
    CANCELLED: "该电子票已取消，不能继续用于乘车。",
  }[status] || "票面状态会随订单支付、取消和退票动作同步更新。";
  return message;
}

function orderActionTitle(status) {
  const map = {
    PENDING_PAYMENT: "待支付订单",
    PAID: "已出票订单",
    CLOSED: "订单已关闭",
    REFUNDED: "订单已退票",
  };
  return map[status] || "订单状态";
}

function orderActionHint(status) {
  const map = {
    PENDING_PAYMENT: "你可以继续支付或取消订单。",
    PAID: "电子票已生成，如行程变化可申请退票。",
    CLOSED: "该订单已关闭，库存已释放。",
    REFUNDED: "电子票已失效，请查看退款流水确认资金状态。",
  };
  return map[status] || "订单状态会随交易动作自动更新。";
}

function renderOrderTimeline(order, ticket, payments, refunds) {
  const successPayment = payments.find(payment => payment.status === "SUCCESS");
  const latestPayment = payments[0];
  const latestRefund = refunds[0];
  const hasRefundFlow = order.status === "REFUNDED" || refunds.length > 0;
  const steps = [
    {
      label: "创建订单",
      time: order.createdAt,
      done: Boolean(order.createdAt),
      detail: order.orderNo || "-",
    },
    {
      label: "完成支付",
      time: order.paidAt || (successPayment && successPayment.paidAt),
      done: ["PAID", "REFUNDED"].includes(order.status),
      detail: successPayment ? successPayment.paymentNo : (latestPayment ? paymentStatusText(latestPayment.status) : "等待支付"),
    },
    {
      label: "生成电子票",
      time: ticket && ticket.issuedAt,
      done: Boolean(ticket && ticket.issuedAt),
      detail: ticket ? ticket.ticketNo : "支付成功后生成",
    },
    ...(hasRefundFlow ? [{
      label: "退票处理",
      time: order.refundedAt,
      done: order.status === "REFUNDED",
      detail: order.status === "REFUNDED" ? "订单已退票" : "等待退票处理",
    },
    {
      label: "退款结果",
      time: latestRefund && (latestRefund.refundedAt || latestRefund.createdAt),
      done: Boolean(latestRefund && latestRefund.status === "SUCCESS"),
      detail: latestRefund ? `${refundStatusText(latestRefund.status)} / ${latestRefund.refundNo}` : "等待退款结果",
    }] : []),
  ];
  return `
    <ol class="order-progress-timeline">
      ${steps.map(step => `
        <li class="${step.done ? "done" : ""}">
          <span class="timeline-dot"></span>
          <div>
            <strong>${escapeHtml(step.label)}</strong>
            <small>${escapeHtml(step.detail || "-")}</small>
            <time>${formatDateTime(step.time) || "-"}</time>
          </div>
        </li>
      `).join("")}
    </ol>
  `;
}

function renderFullOrderTimeline(order, ticket, payments, refunds, ticketChanges = [], notifications = []) {
  const successPayment = payments.find(payment => payment.status === "SUCCESS");
  const latestPayment = payments[0];
  const latestRefund = refunds[0];
  const hasRefundFlow = order.status === "REFUNDED" || refunds.length > 0;
  const steps = [
    {
      label: "创建订单",
      time: order.createdAt,
      done: Boolean(order.createdAt),
      detail: order.orderNo || "-",
    },
    {
      label: "完成支付",
      time: order.paidAt || (successPayment && successPayment.paidAt),
      done: ["PAID", "REFUNDED", "CANCELLED"].includes(order.status),
      detail: successPayment ? successPayment.paymentNo : (latestPayment ? paymentStatusText(latestPayment.status) : "等待支付"),
    },
    {
      label: "生成电子票",
      time: ticket && ticket.issuedAt,
      done: Boolean(ticket && ticket.issuedAt),
      detail: ticket ? ticket.ticketNo : "支付成功后生成",
    },
    ...ticketChanges.map(change => ({
      label: "改签进度",
      time: change.completedAt || change.updatedAt || change.createdAt,
      done: change.status === "SUCCESS",
      detail: `${changeStatusText(change.status)} / ${change.changeNo || "-"} / ${change.originalTrainNo || "-"} → ${change.newTrainNo || "-"}`,
    })),
    ...(hasRefundFlow ? [{
      label: "退票处理",
      time: order.refundedAt,
      done: order.status === "REFUNDED",
      detail: order.status === "REFUNDED" ? "订单已退票" : "等待退票处理",
    },
    {
      label: "退款结果",
      time: latestRefund && (latestRefund.refundedAt || latestRefund.createdAt),
      done: Boolean(latestRefund && latestRefund.status === "SUCCESS"),
      detail: latestRefund ? `${refundStatusText(latestRefund.status)} / ${latestRefund.refundNo}` : "等待退款结果",
    }] : []),
  ].sort((leftStep, rightStep) => {
    const left = leftStep.time ? new Date(leftStep.time).getTime() : Number.MAX_SAFE_INTEGER;
    const right = rightStep.time ? new Date(rightStep.time).getTime() : Number.MAX_SAFE_INTEGER;
    return left - right;
  });
  return `
    <ol class="order-progress-timeline full-chain-timeline">
      ${steps.map(step => `
        <li class="${step.done ? "done" : ""}">
          <span class="timeline-dot"></span>
          <div>
            <strong>${escapeHtml(step.label)}</strong>
            <small>${escapeHtml(step.detail || "-")}</small>
            <time>${formatDateTime(step.time) || "-"}</time>
          </div>
        </li>
      `).join("")}
    </ol>
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
        <p class="eyebrow">订单详情</p>
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
  if (modal.id === "passenger-order-detail-modal") {
    passengerState.activeDetailOrderId = null;
  }
}

function renderOrderActions(order) {
  if (order.status === "PENDING_PAYMENT") {
    return `
      <button class="primary-button compact-button" type="button" data-pay-order="${order.id}">去支付</button>
      <button class="danger-button compact-button" type="button" data-close-order="${order.id}">取消订单</button>
    `;
  }
  if (order.status === "PAID") {
    return `
      <button class="secondary-button compact-button" type="button" data-change-order="${order.id}">申请改签</button>
      <button class="danger-button compact-button" type="button" data-refund-order="${order.id}">申请退票</button>
    `;
  }
  if (order.status === "REFUNDED") {
    return `<button class="secondary-button compact-button" type="button" data-jump-refunds>查看退款</button>`;
  }
  return `<span class="muted-text">无需操作</span>`;
}

async function loadPassengerTickets() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const params = new URLSearchParams();
    if (elements.ticketStatus.value) {
      params.set("status", elements.ticketStatus.value);
    }
    params.set("page", String(passengerState.tickets.page));
    params.set("size", String(passengerState.tickets.size));
    const page = await passengerRequest(`/passenger/tickets?${params.toString()}`);
    passengerState.tickets = { ...passengerState.tickets, ...page };
    renderPassengerTickets(page.content || []);
    renderPagination("tickets");
  } catch (error) {
    elements.ticketResults.innerHTML = recordEmpty(error.message || "无法加载我的电子票");
    renderPagination("tickets");
  }
}

function renderPassengerTickets(tickets) {
  if (!tickets.length) {
    elements.ticketResults.innerHTML = recordEmpty("暂无电子票，支付成功后会自动生成");
    return;
  }
  elements.ticketResults.innerHTML = tickets.map(ticket => `
    <article class="passenger-ticket-card ${ticketStatusClass(ticket.status)}">
      <div class="ticket-wallet-route">
        <div>
          <p class="eyebrow">${escapeHtml(ticket.ticketNo || "-")}</p>
          <h3>${escapeHtml(ticket.trainNo || "-")}</h3>
        </div>
        <span class="status ${ticketStatusClass(ticket.status)}">${ticketStatusText(ticket.status)}</span>
      </div>
      <div class="ticket-wallet-line">
        <strong>${escapeHtml(ticket.departureStation || "-")}</strong>
        <span></span>
        <strong>${escapeHtml(ticket.arrivalStation || "-")}</strong>
      </div>
      <div class="ticket-wallet-meta">
        <div><span>乘车日期</span><strong>${formatDate(ticket.travelDate) || "-"}</strong></div>
        <div><span>出发 / 到达</span><strong>${formatTime(ticket.departureTime) || "-"} - ${formatTime(ticket.arrivalTime) || "-"}</strong></div>
        <div><span>席别</span><strong>${seatTypeText(ticket.seatType)}</strong></div>
        <div><span>票价</span><strong class="money">¥${formatAmount(ticket.amount)}</strong></div>
      </div>
      <div class="ticket-wallet-passenger">
        <span>${escapeHtml(ticket.passengerName || "-")}</span>
        <small>${idTypeText(ticket.passengerIdType)} / ${escapeHtml(ticket.passengerIdCardMasked || "-")}</small>
      </div>
      ${renderTicketStateNote(ticket)}
      <div class="ticket-wallet-footer">
        <span>出票 ${formatDateTime(ticket.issuedAt) || "-"}</span>
        <button class="secondary-button compact-button" type="button" data-ticket-order-detail="${ticket.orderId}">查看订单详情</button>
      </div>
    </article>
  `).join("");
  elements.ticketResults.querySelectorAll("[data-ticket-order-detail]").forEach(button => {
    button.addEventListener("click", () => openPassengerOrderDetail(button.dataset.ticketOrderDetail));
  });
}

async function payPassengerOrder(orderId) {
  try {
    await passengerRequest(`/passenger/orders/${orderId}/pay`, { method: "POST" });
    showToast("支付成功，订单状态已更新");
    await refreshPassengerFlow({ detailOrderId: orderId });
  } catch (error) {
    showToast(error.message || "支付失败");
  }
}

async function closePassengerOrder(orderId) {
  try {
    await passengerRequest(`/passenger/orders/${orderId}/close`, { method: "POST" });
    showToast("订单已取消，库存已释放");
    await refreshPassengerFlow({ detailOrderId: orderId });
  } catch (error) {
    showToast(error.message || "取消订单失败");
  }
}

async function refundPassengerOrder(orderId) {
  try {
    await passengerRequest(`/passenger/orders/${orderId}/refund`, { method: "POST" });
    showToast("退票成功，已创建退款流水");
    await refreshPassengerFlow({ detailOrderId: orderId });
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

async function openChangeFromOrderId(orderId) {
  try {
    const detail = await passengerRequest(`/passenger/orders/${orderId}/detail`);
    openChangeModal(detail.order || { id: orderId });
  } catch (error) {
    showToast(error.message || "无法打开改签窗口");
  }
}

async function openChangeModal(order) {
  if (!ensureSignedIn()) {
    return;
  }
  if (!order || !order.id) {
    showToast("未找到可改签订单");
    return;
  }
  passengerState.selectedChangeOrder = order;
  passengerState.selectedChangeTrain = null;
  passengerState.changeCandidates = [];
  const date = formatDate(order.travelDate);
  elements.changeDate.value = date && date !== "-" ? date : new Date().toISOString().slice(0, 10);
  elements.changeReason.value = "";
  elements.changeError.textContent = "";
  elements.changeConfirm.disabled = true;
  elements.changeConfirm.textContent = "提交改签";
  elements.changeSummary.innerHTML = `
    <div class="buy-route">
      <strong>${escapeHtml(order.trainNo || "-")}</strong>
      <span>原订单 ${escapeHtml(order.orderNo || "-")}</span>
    </div>
    <div class="buy-detail-grid">
      <div><span>乘车日期</span><strong>${formatDate(order.travelDate)}</strong></div>
      <div><span>席别</span><strong>${seatTypeText(order.seatType)}</strong></div>
      <div><span>乘车人</span><strong>${escapeHtml(order.passengerName || "-")}</strong></div>
      <div><span>原票金额</span><strong class="money">¥${formatAmount(order.amount)}</strong></div>
    </div>
  `;
  elements.changeCandidates.innerHTML = recordEmpty("请选择日期后加载可改签车次");
  elements.changeModal.classList.add("show");
  elements.changeModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("modal-open");
  await loadChangeCandidates();
}

function closeChangeModal() {
  elements.changeModal.classList.remove("show");
  elements.changeModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("modal-open");
  passengerState.selectedChangeOrder = null;
  passengerState.selectedChangeTrain = null;
  passengerState.changeCandidates = [];
}

async function loadChangeCandidates() {
  if (!ensureSignedIn()) {
    return;
  }
  const order = passengerState.selectedChangeOrder;
  if (!order) {
    return;
  }
  try {
    elements.changeCandidates.innerHTML = recordEmpty("正在加载可改签车次");
    passengerState.selectedChangeTrain = null;
    elements.changeConfirm.disabled = true;
    const params = new URLSearchParams();
    if (elements.changeDate.value) {
      params.set("travelDate", elements.changeDate.value);
    }
    params.set("page", "0");
    params.set("size", "40");
    const trains = await passengerRequest(`/trains/available?${params.toString()}`);
    passengerState.changeCandidates = (trains || []).filter(train => !(
      String(train.trainNo) === String(order.trainNo)
      && formatDate(train.travelDate) === formatDate(order.travelDate)
      && String(train.seatType) === String(order.seatType)
    ));
    renderChangeCandidates(passengerState.changeCandidates);
  } catch (error) {
    elements.changeCandidates.innerHTML = recordEmpty(error.message || "无法加载可改签车次");
  }
}

function renderChangeCandidates(candidates) {
  if (!candidates.length) {
    elements.changeCandidates.innerHTML = recordEmpty("当前日期暂无可改签车次，可调整日期后重试");
    return;
  }
  const orderAmount = Number((passengerState.selectedChangeOrder && passengerState.selectedChangeOrder.amount) || 0);
  elements.changeCandidates.innerHTML = candidates.slice(0, 12).map(train => {
    const diff = Number(train.price || 0) - orderAmount;
    const diffText = diff > 0 ? `补差 ¥${formatAmount(diff)}` : (diff < 0 ? `退差 ¥${formatAmount(Math.abs(diff))}` : "无需补差");
    return `
      <button class="change-candidate-card" type="button" data-change-inventory="${train.inventoryId}">
        <div class="change-route">
          <strong>${escapeHtml(train.trainNo)}</strong>
          <span>${escapeHtml(train.departureStation)} → ${escapeHtml(train.arrivalStation)}</span>
        </div>
        <div class="change-candidate-meta">
          <span>${formatDate(train.travelDate)} ${formatTime(train.departureTime)} - ${formatTime(train.arrivalTime)}</span>
          <span>${seatTypeText(train.seatType)} · 余票 ${train.remainingSeats}</span>
        </div>
        <div class="change-candidate-price">
          <strong>¥${formatAmount(train.price)}</strong>
          <span>${diffText}</span>
        </div>
      </button>
    `;
  }).join("");
  elements.changeCandidates.querySelectorAll("[data-change-inventory]").forEach(button => {
    button.addEventListener("click", () => {
      const train = passengerState.changeCandidates.find(item => String(item.inventoryId) === String(button.dataset.changeInventory));
      passengerState.selectedChangeTrain = train || null;
      elements.changeCandidates.querySelectorAll(".change-candidate-card").forEach(card => {
        card.classList.toggle("selected", card === button);
      });
      elements.changeConfirm.disabled = !passengerState.selectedChangeTrain;
    });
  });
}

async function submitTicketChange() {
  const order = passengerState.selectedChangeOrder;
  const train = passengerState.selectedChangeTrain;
  if (!order || !train) {
    elements.changeError.textContent = "请先选择新的车次";
    return;
  }
  elements.changeError.textContent = "";
  elements.changeConfirm.disabled = true;
  elements.changeConfirm.textContent = "提交中...";
  try {
    const body = {
      trainId: train.trainId,
      inventoryId: train.inventoryId,
      requestId: generateRequestId("PAX-CHANGE"),
      reason: elements.changeReason.value.trim(),
    };
    const result = await passengerRequest(`/passenger/orders/${order.id}/change`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    closeChangeModal();
    passengerState.changes.page = 0;
    passengerState.tickets.page = 0;
    showToast(result.status === "PENDING_PAYMENT" ? "改签已提交，请完成新票支付" : "改签成功，电子票已更新");
    await refreshPassengerFlow({ detailOrderId: result.newOrderId || order.id });
  } catch (error) {
    elements.changeError.textContent = error.message || "提交改签失败";
    elements.changeConfirm.disabled = false;
    elements.changeConfirm.textContent = "提交改签";
  }
}

async function loadPassengerChanges() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const params = new URLSearchParams();
    if (elements.changeStatus.value) {
      params.set("status", elements.changeStatus.value);
    }
    params.set("page", String(passengerState.changes.page));
    params.set("size", String(passengerState.changes.size));
    const page = await passengerRequest(`/passenger/changes?${params.toString()}`);
    passengerState.changes = { ...passengerState.changes, ...page };
    renderPassengerChanges(page.content || []);
    renderPagination("changes");
  } catch (error) {
    elements.changeResults.innerHTML = recordEmpty(error.message || "无法加载改签记录");
    renderPagination("changes");
  }
}

function renderPassengerChanges(changes) {
  if (!changes.length) {
    elements.changeResults.innerHTML = recordEmpty("暂无改签记录。已支付订单可在订单中心发起改签。");
    return;
  }
  elements.changeResults.innerHTML = changes.map(change => `
    <article class="ticket-change-card ${changeStatusClass(change.status)}">
      <div class="record-title-row">
        <div>
          <span>改签单</span>
          <strong>${escapeHtml(change.changeNo)}</strong>
        </div>
        <span class="status ${changeStatusClass(change.status)}">${changeStatusText(change.status)}</span>
      </div>
      <div class="change-route-panel">
        <div>
          <span>原订单</span>
          <strong>${escapeHtml(change.originalOrderNo || "-")}</strong>
          <small>${escapeHtml(change.originalTrainNo || "-")} / ${escapeHtml(change.originalTicketNo || "-")}</small>
        </div>
        <div class="change-arrow">→</div>
        <div>
          <span>新订单</span>
          <strong>${escapeHtml(change.newOrderNo || "-")}</strong>
          <small>${escapeHtml(change.newTrainNo || "-")} / ${escapeHtml(change.newTicketNo || "-")}</small>
        </div>
      </div>
      <div class="record-detail-grid">
        <div><span>原票金额</span><strong class="money">¥${formatAmount(change.oldAmount)}</strong></div>
        <div><span>新票金额</span><strong class="money">¥${formatAmount(change.newAmount)}</strong></div>
        <div><span>差额</span><strong class="money">${formatSignedAmount(change.priceDifference)}</strong></div>
        <div><span>完成时间</span><strong>${formatDateTime(change.completedAt) || "-"}</strong></div>
      </div>
      <div class="record-actions">
        ${change.status === "PENDING_PAYMENT"
          ? `<button class="primary-button compact-button" type="button" data-pay-change="${change.id}">支付新票</button>`
          : `<span class="muted-text">${change.failureReason ? escapeHtml(change.failureReason) : "状态已同步"}</span>`}
        ${change.newOrderId ? `<button class="secondary-button compact-button" type="button" data-change-new-order="${change.newOrderId}">新订单详情</button>` : ""}
        ${change.originalOrderId ? `<button class="ghost-button compact-button" type="button" data-change-original-order="${change.originalOrderId}">原订单详情</button>` : ""}
      </div>
    </article>
  `).join("");
  elements.changeResults.querySelectorAll("[data-pay-change]").forEach(button => {
    button.addEventListener("click", () => payPassengerChange(button.dataset.payChange));
  });
  elements.changeResults.querySelectorAll("[data-change-new-order],[data-change-original-order]").forEach(button => {
    const orderId = button.dataset.changeNewOrder || button.dataset.changeOriginalOrder;
    button.addEventListener("click", () => openPassengerOrderDetail(orderId));
  });
}

async function payPassengerChange(changeId, options = {}) {
  try {
    const result = await passengerRequest(`/passenger/changes/${changeId}/pay`, { method: "POST" });
    showToast("改签补差支付完成，电子票已更新");
    await refreshPassengerFlow({ detailOrderId: result && result.newOrderId ? result.newOrderId : options.orderId });
    if (!result && !options.stayOnDetail) {
      activateSection("passenger-changes");
    }
  } catch (error) {
    showToast(error.message || "改签支付失败");
  }
}

async function loadPassengerNotifications() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const params = new URLSearchParams();
    if (elements.notificationStatus.value) {
      params.set("status", elements.notificationStatus.value);
    }
    params.set("page", String(passengerState.notifications.page));
    params.set("size", String(passengerState.notifications.size));
    const page = await passengerRequest(`/passenger/notifications?${params.toString()}`);
    passengerState.notifications = { ...passengerState.notifications, ...page };
    renderPassengerNotifications(page.content || []);
    renderPagination("notifications");
    await loadPassengerNotificationSummary();
  } catch (error) {
    elements.notificationResults.innerHTML = recordEmpty(error.message || "无法加载消息中心");
    renderPagination("notifications");
  }
}

async function loadPassengerNotificationSummary() {
  if (!ensureSignedIn()) {
    return;
  }
  try {
    const summary = await passengerRequest("/passenger/notifications/unread-count");
    const unread = Number(summary.unreadCount || 0);
    if (elements.notificationTotal) {
      elements.notificationTotal.textContent = summary.totalCount || 0;
    }
    if (elements.notificationUnread) {
      elements.notificationUnread.textContent = unread;
    }
    elements.navUnread.textContent = unread;
    elements.navUnread.classList.toggle("is-empty", unread === 0);
    renderPassengerNotificationGuide(summary);
  } catch (error) {
    if (elements.notificationTotal) {
      elements.notificationTotal.textContent = "0";
    }
    if (elements.notificationUnread) {
      elements.notificationUnread.textContent = "0";
    }
    elements.navUnread.textContent = "0";
    elements.navUnread.classList.add("is-empty");
    if (elements.notificationGuide) {
      elements.notificationGuide.innerHTML = "";
    }
  }
}

function renderPassengerNotificationGuide(summary) {
  if (!elements.notificationGuide) {
    return;
  }
  const unread = Number(summary.unreadCount || 0);
  const unreadByType = summary.unreadCountByType || {};
  const actionTypes = [
    "ORDER_CREATED",
    "TICKET_CHANGE_PENDING_PAYMENT",
    "ORDER_REFUNDED",
    "REFUND_FAILED",
    "TICKET_ISSUED",
  ];
  const chips = actionTypes
    .filter(type => Number(unreadByType[type] || 0) > 0)
    .map(type => `<span>${notificationTypeText(type)} ${Number(unreadByType[type] || 0)}</span>`)
    .join("");
  if (unread === 0) {
    elements.notificationGuide.innerHTML = `
      <div>
        <strong>当前没有未读提醒</strong>
        <span>支付、出票、改签和退款进度会自动同步到这里。</span>
      </div>
    `;
    return;
  }
  elements.notificationGuide.innerHTML = `
    <div>
      <strong>还有 ${unread} 条未读提醒</strong>
      <span>优先处理待支付、改签补差和退款异常，其他提醒可从订单详情查看完整进度。</span>
    </div>
    <div class="notification-guide-chips">${chips || "<span>查看最新提醒</span>"}</div>
  `;
}

function renderPassengerNotifications(notifications) {
  if (!notifications.length) {
    passengerState.selectedNotificationId = null;
    elements.notificationResults.innerHTML = recordEmpty("暂无消息提醒");
    return;
  }
  const selectedIndex = notifications.findIndex((notification, index) =>
    notificationIdentity(notification, index) === passengerState.selectedNotificationId
  );
  const activeIndex = selectedIndex >= 0 ? selectedIndex : 0;
  const selectedNotification = notifications[activeIndex];
  passengerState.selectedNotificationId = notificationIdentity(selectedNotification, activeIndex);
  elements.notificationResults.innerHTML = `
    <div class="passenger-notification-browser">
      <aside class="notification-outline-panel" aria-label="通知大纲">
        <div class="notification-outline-toolbar">
          <strong>按时间排序</strong>
          <span>本页 ${notifications.length} 条</span>
        </div>
        <div class="notification-outline-list">
          ${notifications.map((notification, index) => renderNotificationOutlineItem(notification, index)).join("")}
        </div>
      </aside>
      <article class="notification-detail-panel" aria-live="polite">
        ${renderNotificationDetail(selectedNotification)}
      </article>
    </div>
  `;
  elements.notificationResults.querySelectorAll("[data-notification-select]").forEach(button => {
    button.addEventListener("click", () => {
      passengerState.selectedNotificationId = button.dataset.notificationSelect;
      renderPassengerNotifications(notifications);
    });
  });
  elements.notificationResults.querySelectorAll("[data-notification-read]").forEach(button => {
    button.addEventListener("click", () => markPassengerNotificationRead(button.dataset.notificationRead));
  });
  elements.notificationResults.querySelectorAll("[data-notification-action]").forEach(button => {
    button.addEventListener("click", () => handlePassengerNotificationAction(button));
  });
}

function notificationIdentity(notification, index) {
  return String(notification && (notification.id || notification.notificationNo || notification.createdAt) || index);
}

function renderNotificationOutlineItem(notification, index) {
  const key = notificationIdentity(notification, index);
  const active = key === passengerState.selectedNotificationId;
  const unread = notification.status === "UNREAD";
  return `
    <button class="notification-outline-item ${active ? "active" : ""} ${unread ? "unread" : ""}" type="button"
      data-notification-select="${escapeHtml(key)}"
      aria-current="${active ? "true" : "false"}">
      <span class="notification-outline-main">
        <span class="notification-outline-kicker">${notificationTypeText(notification.type)}</span>
        <strong class="notification-outline-title">${escapeHtml(notificationTitleText(notification))}</strong>
        <span class="notification-outline-summary">${escapeHtml(compactNotificationText(notificationContentText(notification), 46))}</span>
      </span>
      <span class="notification-outline-meta">
        <span>${formatNotificationOutlineTime(notification.createdAt)}</span>
        ${unread ? `<span class="notification-outline-unread" aria-label="未读"></span>` : ""}
      </span>
    </button>
  `;
}

function renderNotificationDetail(notification) {
  return `
    <div class="notification-detail-head">
      <div>
        <h3>${escapeHtml(notificationTitleText(notification))}</h3>
        <div class="notification-detail-meta">
          <span>${notificationTypeText(notification.type)}</span>
          <span>${formatDateTime(notification.createdAt) || "-"}</span>
        </div>
      </div>
      <span class="status ${notificationStatusClass(notification.status)}">${notificationStatusText(notification.status)}</span>
    </div>
    <div class="notification-detail-body">
      <p class="notification-content">${escapeHtml(notificationContentText(notification))}</p>
      <div class="notification-detail-grid">
        <div><span>订单号</span><strong>${escapeHtml(notification.orderNo || "-")}</strong></div>
        <div><span>关联业务</span><strong>${businessTypeText(notification.businessType)} / ${businessIdText(notification.businessId)}</strong></div>
        <div><span>电子票</span><strong>${escapeHtml(notification.ticketNo || "-")}</strong></div>
        <div><span>创建时间</span><strong>${formatDateTime(notification.createdAt) || "-"}</strong></div>
      </div>
      ${notification.actionHint ? `<div class="notification-next-step"><span>下一步</span><strong>${escapeHtml(userFacingHint(notification.actionHint))}</strong></div>` : ""}
    </div>
    <div class="notification-detail-actions">
      ${notificationActionButton(notification)}
      ${notification.status === "UNREAD"
        ? `<button class="secondary-button compact-button" type="button" data-notification-read="${notification.id}">标为已读</button>`
        : `<span class="muted-text">已读 ${formatDateTime(notification.readAt) || ""}</span>`}
    </div>
  `;
}

function compactNotificationText(value, maxLength) {
  const text = String(value || "暂无消息内容").replace(/\s+/g, " ").trim();
  if (text.length <= maxLength) {
    return text;
  }
  return `${text.slice(0, maxLength)}...`;
}

function formatNotificationOutlineTime(value) {
  const text = formatDateTime(value);
  if (!text || text === "-") {
    return "-";
  }
  return text.replace(/^\d{4}-/, "");
}

function notificationActionButton(notification) {
  const label = userFacingHint(notification.actionLabel || "查看");
  const action = notification.actionType || "VIEW_MESSAGE";
  return `
    <button class="primary-button compact-button notification-action-button" type="button"
      data-notification-action="${escapeHtml(action)}"
      data-notification-id="${notification.id || ""}"
      data-notification-order="${notification.orderId || ""}"
      data-notification-business="${escapeHtml(notification.businessId || "")}"
      data-notification-type="${escapeHtml(notification.type || "")}">
      ${escapeHtml(label)}
    </button>
  `;
}

async function handlePassengerNotificationAction(button) {
  const action = button.dataset.notificationAction;
  const id = button.dataset.notificationId;
  const orderId = button.dataset.notificationOrder;
  const type = button.dataset.notificationType;
  if (id) {
    await markPassengerNotificationRead(id, { silent: true });
  }
  if ((action === "ORDER_DETAIL" || action === "ORDER_PAYMENT") && orderId) {
    if (action === "ORDER_PAYMENT") {
      elements.orderStatus.value = "PENDING_PAYMENT";
      passengerState.orders.page = 0;
      await loadPassengerOrders();
    }
    await openPassengerOrderDetail(orderId);
    return;
  }
  if (action === "TICKET_WALLET") {
    elements.ticketStatus.value = type === "TICKET_ISSUED" ? "ISSUED" : "";
    passengerState.tickets.page = 0;
    activateSection("passenger-tickets");
    await loadPassengerTickets();
    return;
  }
  if (action === "REFUND_RECORDS") {
    if (type === "REFUND_FAILED") {
      elements.refundStatus.value = "FAILED";
    } else if (type === "REFUND_SUCCEEDED") {
      elements.refundStatus.value = "SUCCESS";
    } else {
      elements.refundStatus.value = "PENDING";
    }
    passengerState.refunds.page = 0;
    activateSection("passenger-refunds");
    await loadPassengerRefunds();
    return;
  }
  if (action === "CHANGE_PAYMENT" || action === "CHANGE_DETAIL") {
    elements.changeStatus.value = action === "CHANGE_PAYMENT" ? "PENDING_PAYMENT" : "";
    passengerState.changes.page = 0;
    activateSection("passenger-changes");
    await loadPassengerChanges();
    return;
  }
  activateSection("passenger-notifications");
}

async function markPassengerNotificationRead(id, options = {}) {
  try {
    await passengerRequest(`/passenger/notifications/${id}/read`, { method: "POST" });
    await loadPassengerNotifications();
    await loadPassengerTransactionSummary();
    if (!options.silent) {
      showToast("消息已标为已读");
    }
  } catch (error) {
    showToast(error.message || "消息状态更新失败");
  }
}

async function markAllPassengerNotificationsRead() {
  try {
    await passengerRequest("/passenger/notifications/read-all", { method: "POST" });
    passengerState.notifications.page = 0;
    await loadPassengerNotifications();
    await loadPassengerTransactionSummary();
    showToast("全部消息已标为已读");
  } catch (error) {
    showToast(error.message || "全部已读失败");
  }
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
  } else if (type === "tickets") {
    await loadPassengerTickets();
  } else if (type === "payments") {
    await loadPassengerPayments();
  } else if (type === "refunds") {
    await loadPassengerRefunds();
  } else if (type === "changes") {
    await loadPassengerChanges();
  } else {
    await loadPassengerNotifications();
  }
}

function renderPagination(type) {
  const map = {
    orders: { state: passengerState.orders, info: elements.orderPageInfo, prev: elements.prevOrders, next: elements.nextOrders },
    tickets: { state: passengerState.tickets, info: elements.ticketPageInfo, prev: elements.prevTickets, next: elements.nextTickets },
    payments: { state: passengerState.payments, info: elements.paymentPageInfo, prev: elements.prevPayments, next: elements.nextPayments },
    refunds: { state: passengerState.refunds, info: elements.refundPageInfo, prev: elements.prevRefunds, next: elements.nextRefunds },
    changes: { state: passengerState.changes, info: elements.changePageInfo, prev: elements.prevChanges, next: elements.nextChanges },
    notifications: { state: passengerState.notifications, info: elements.notificationPageInfo, prev: elements.prevNotifications, next: elements.nextNotifications },
  };
  const target = map[type];
  const totalPages = Math.max(1, target.state.totalPages || 0);
  const currentPage = Math.min((target.state.page || 0) + 1, totalPages);
  target.info.textContent = `第 ${currentPage} / ${totalPages} 页，共 ${target.state.totalElements || 0} 条`;
  target.prev.disabled = Boolean(target.state.first);
  target.next.disabled = Boolean(target.state.last);
}

async function passengerBlobRequest(path, options = {}, withAuth = true) {
  const requestOptions = { ...options };
  requestOptions.headers = { ...(options.headers || {}) };
  requestOptions.cache = "no-store";
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
  if (response.status === 404 || response.status === 204) {
    return null;
  }
  if (!response.ok) {
    const data = await readBody(response);
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
    const requestError = new Error(data.message || `请求失败（${response.status}）`);
    requestError.status = response.status;
    throw requestError;
  }
  return response.blob();
}

function renderLoggedOutPlaceholders() {
  passengerState.profile = null;
  passengerState.summary = null;
  passengerState.showOnboarding = false;
  elements.metricTotal.textContent = "0";
  elements.metricPending.textContent = "0";
  elements.metricPaid.textContent = "0";
  elements.metricClosed.textContent = "0";
  elements.metricRefunded.textContent = "0";
  elements.metricPayments.textContent = "0";
  elements.metricRefunds.textContent = "0";
  elements.latestOrders.innerHTML = emptyItem("登录后查看最近订单");
  elements.upcomingTrips.innerHTML = emptyItem("登录后查看即将出行");
  if (elements.onboardingSection) {
    elements.onboardingSection.hidden = true;
  }
  syncPassengerOnboardingNav(true);
  elements.onboardingSteps.innerHTML = recordEmpty("注册后可选择是否开启首单引导");
  elements.onboardingAction.dataset.target = "passenger-search";
  elements.onboardingAction.textContent = "开始第一段行程";
  elements.profileRole.textContent = "USER";
  elements.profileDisplay.textContent = "-";
  elements.profileUsername.textContent = "-";
  elements.profileDefaultTraveler.textContent = "未设置";
  elements.profileOrderCount.textContent = "0";
  elements.profileTicketCount.textContent = "0";
  if (elements.profileDetailDisplay) {
    elements.profileDetailDisplay.textContent = "-";
  }
  if (elements.profileDetailUsername) {
    elements.profileDetailUsername.textContent = "-";
  }
  if (elements.profileDetailRole) {
    elements.profileDetailRole.textContent = "普通乘客";
  }
  if (elements.profileDetailTraveler) {
    elements.profileDetailTraveler.textContent = "未设置";
  }
  if (elements.profileDetailOrders) {
    elements.profileDetailOrders.textContent = "0";
  }
  if (elements.profileDetailTickets) {
    elements.profileDetailTickets.textContent = "0";
  }
  elements.profileDisplayName.value = "";
  elements.profileError.textContent = "";
  if (elements.avatarFile) {
    elements.avatarFile.value = "";
  }
  setPassengerAvatarStatus("");
  renderPassengerAvatar(null);
  elements.passwordError.textContent = "";
  passengerState.travelers = [];
  passengerState.travelerById = {};
  elements.travelerList.innerHTML = recordEmpty("登录后维护常用乘车人");
  renderBuyTravelerOptions();
  resetTravelerForm();
  elements.orderCards.innerHTML = emptyItem("登录后查看我的订单");
  elements.transactionStats.innerHTML = recordEmpty("登录后查看行程提醒");
  elements.transactionTodos.innerHTML = emptyItem("登录后查看待办事项");
  elements.transactionOrders.innerHTML = emptyItem("登录后查看订单动态");
  elements.transactionChanges.innerHTML = emptyItem("登录后查看改签动态");
  elements.changeResults.innerHTML = recordEmpty("登录后查看改签记录");
  elements.ticketResults.innerHTML = recordEmpty("登录后查看我的电子票");
  elements.paymentResults.innerHTML = recordEmpty("登录后查看支付流水");
  elements.refundResults.innerHTML = recordEmpty("登录后查看退款流水");
  elements.notificationResults.innerHTML = recordEmpty("登录后查看消息提醒");
  if (elements.notificationGuide) {
    elements.notificationGuide.innerHTML = "";
  }
  if (elements.notificationTotal) {
    elements.notificationTotal.textContent = "0";
  }
  if (elements.notificationUnread) {
    elements.notificationUnread.textContent = "0";
  }
  elements.navUnread.textContent = "0";
  elements.navUnread.classList.add("is-empty");
}

async function passengerRequest(path, options = {}, withAuth = true) {
  const requestOptions = { ...options };
  requestOptions.headers = { ...(options.headers || {}) };
  requestOptions.cache = "no-store";
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
  passengerState.showOnboarding = false;
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
  document.querySelectorAll("[data-topbar-jump]").forEach(button => {
    button.addEventListener("click", () => activateSection(button.dataset.topbarJump));
  });
  const quickSearch = document.querySelector(".passenger-top-search input");
  if (quickSearch) {
    quickSearch.addEventListener("keydown", event => {
      if (event.key !== "Enter") {
        return;
      }
      event.preventDefault();
      const keyword = quickSearch.value.trim();
      if (!keyword) {
        activateSection("passenger-search");
        return;
      }
      elements.orderStatus.value = "";
      activateSection("passenger-orders");
      showToast("已切换到订单页，可在订单详情中核对相关记录");
    });
  }
}

function setupPassengerScrollSpy() {
  const sections = Array.from(document.querySelectorAll(".passenger-section"));
  if (!sections.length) {
    updateActiveNav("passenger-search");
    return;
  }
  activateSection("passenger-search", { silent: true });
}

function updateActiveNav(sectionId) {
  const navSectionId = navSectionAlias(sectionId);
  syncPassengerOnboardingNav(!elements.onboardingSection || elements.onboardingSection.hidden);
  document.querySelectorAll(".passenger-nav a").forEach(link => {
    if (link.hidden) {
      link.classList.remove("active");
      return;
    }
    link.classList.toggle("active", link.getAttribute("href") === `#${navSectionId}`);
  });
}

function activateSection(sectionId, options = {}) {
  const requestedRootSectionId = navSectionAlias(sectionId);
  const rootSectionId = normalizePassengerSectionId(requestedRootSectionId);
  const section = document.querySelector(`#${rootSectionId}`);
  if (!section) {
    return;
  }
  document.querySelectorAll(".passenger-site-main > .passenger-section").forEach(item => {
    item.classList.toggle("is-active", item.id === rootSectionId);
  });
  updateActiveNav(rootSectionId);
  syncPassengerFeatureTabForSection(sectionId);
  const page = document.querySelector(".passenger-site-main");
  if (page) {
    const target = sectionId !== rootSectionId && requestedRootSectionId === rootSectionId
      ? document.querySelector(`#${sectionId}`)
      : null;
    const top = target && section.contains(target)
      ? Math.max(0, target.offsetTop - section.offsetTop - 18)
      : 0;
    page.scrollTo({ top, behavior: options.silent ? "auto" : "smooth" });
  }
  if (!options.silent) {
    section.classList.add("section-highlight");
    window.setTimeout(() => section.classList.remove("section-highlight"), 900);
  }
}

function navSectionAlias(sectionId) {
  const aliases = {
    "passenger-transactions": "passenger-summary",
    "passenger-travelers": "passenger-account",
    "passenger-changes": "passenger-orders",
    "passenger-payments": "passenger-orders",
    "passenger-refunds": "passenger-orders",
  };
  return aliases[sectionId] || sectionId;
}

function normalizePassengerSectionId(sectionId) {
  if (sectionId !== "passenger-onboarding") {
    return sectionId;
  }
  return isPassengerOnboardingAccessible() ? sectionId : "passenger-summary";
}

function isPassengerOnboardingAccessible() {
  if (!passengerState.auth || !elements.onboardingSection) {
    return false;
  }
  return Boolean(
    passengerState.showOnboarding
    && !isPassengerOnboardingDismissed()
    && !elements.onboardingSection.hidden
  );
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

function formatSignedAmount(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number) || number === 0) {
    return "¥0.00";
  }
  return `${number > 0 ? "+" : "-"}¥${formatAmount(Math.abs(number))}`;
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
  return map[value] || humanizeCode(value);
}

function statusText(value) {
  const map = {
    PENDING_PAYMENT: "待支付",
    PAID: "已支付",
    REFUNDED: "已退票",
    CLOSED: "已关闭",
    CANCELLED: "已取消",
  };
  return map[value] || humanizeCode(value);
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
  return map[value] || humanizeCode(value);
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
  return map[value] || humanizeCode(value);
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
  return map[value] || humanizeCode(value);
}

function changeStatusClass(value) {
  const map = {
    PENDING_PAYMENT: "pending",
    SUCCESS: "issued",
    FAILED: "closed",
    CANCELLED: "closed",
  };
  return map[value] || "";
}

function ticketStatusText(value) {
  const map = {
    ISSUED: "有效票",
    REFUNDED: "已退票",
    CANCELLED: "已取消",
  };
  return map[value] || humanizeCode(value);
}

function ticketStatusClass(value) {
  const map = {
    ISSUED: "issued",
    REFUNDED: "refunded",
    CANCELLED: "closed",
  };
  return map[value] || "";
}

function notificationTitleText(notification) {
  const rawTitle = String(notification && notification.title ? notification.title : "").trim();
  const map = {
    "order created": "下单成功",
    "payment confirmed": "支付成功",
    "ticket issued": "出票成功",
    "order closed": "订单已关闭",
    "order refunded": "退票成功",
    "refund succeeded": "退款成功",
    "refund failed": "退款失败",
    "ticket change created": "已发起改签",
    "ticket change pending payment": "改签待支付",
    "ticket change succeeded": "改签成功",
    "ticket change failed": "改签失败",
  };
  return map[rawTitle.toLowerCase()] || notificationTypeText(notification && notification.type) || rawTitle || "-";
}

function notificationContentText(notification) {
  const content = String(notification && notification.content ? notification.content : "").trim();
  if (!content) {
    return userFacingHint(notification && notification.actionHint);
  }
  const directHint = userFacingHint(content);
  if (directHint !== humanizeCode(content)) {
    return directHint;
  }
  const paymentMatch = content.match(/^Order\s+(\S+)\s+payment succeeded\.\s+Amount\s+([^,]+),\s+paymentNo\s+(\S+)\.?$/i);
  if (paymentMatch) {
    return `订单 ${paymentMatch[1]} 已支付成功，金额 ¥${paymentMatch[2]}，支付流水 ${paymentMatch[3]}。`;
  }
  const ticketMatch = content.match(/^Ticket\s+(\S+)\s+for train\s+(\S+)\s+(.+?)\s+to\s+(.+?)\s+on\s+([0-9-]+)\s+has been issued\.?$/i);
  if (ticketMatch) {
    return `电子票 ${ticketMatch[1]} 已出票，车次 ${ticketMatch[2]}，${ticketMatch[3]} 至 ${ticketMatch[4]}，乘车日期 ${ticketMatch[5]}。`;
  }
  const orderMatch = content.match(/^Order\s+(\S+)\s+is pending payment\.\s+Amount\s+([^,]+),\s+payment deadline\s+(.+)\.?$/i);
  if (orderMatch) {
    return `订单 ${orderMatch[1]} 已创建，金额 ¥${orderMatch[2]}，请在 ${orderMatch[3]} 前完成支付。`;
  }
  const type = notification && notification.type;
  const orderNo = notification && notification.orderNo ? notification.orderNo : "当前订单";
  const ticketNo = notification && notification.ticketNo ? notification.ticketNo : "电子票";
  const fallback = {
    ORDER_CREATED: `${orderNo} 已创建，请及时完成后续操作。`,
    PAYMENT_SUCCEEDED: `${orderNo} 已支付成功，电子票状态会同步更新。`,
    TICKET_ISSUED: `${ticketNo} 已出票，可在电子票夹查看。`,
    ORDER_CLOSED: `${orderNo} 已关闭。`,
    ORDER_REFUNDED: `${orderNo} 已退票，退款记录会同步更新。`,
    REFUND_SUCCEEDED: `${orderNo} 退款已成功。`,
    REFUND_FAILED: `${orderNo} 退款失败，请稍后查看或联系运营处理。`,
    TICKET_CHANGE_CREATED: `${orderNo} 已发起改签。`,
    TICKET_CHANGE_PENDING_PAYMENT: `${orderNo} 改签待支付差额。`,
    TICKET_CHANGE_SUCCEEDED: `${orderNo} 改签已完成。`,
    TICKET_CHANGE_FAILED: `${orderNo} 改签失败。`,
  };
  return fallback[type] || content;
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
    ORDER_PAYMENT: "订单支付",
    TICKET_WALLET: "电子票夹",
    REFUND_RECORDS: "退款记录",
    CHANGE_PAYMENT: "改签补差",
    CHANGE_DETAIL: "改签详情",
  };
  return map[value] || humanizeCode(value);
}

function notificationStatusText(value) {
  const map = {
    UNREAD: "未读",
    READ: "已读",
  };
  return map[value] || humanizeCode(value);
}

function todoTypeText(value) {
  const map = {
    ORDER: "订单待办",
    PAYMENT: "支付待办",
    REFUND: "退款待办",
    CHANGE: "改签待办",
    NOTIFICATION: "消息提醒",
    ORDER_DETAIL: "订单详情",
    CHANGE_PAY: "改签补差",
    REFUNDS: "退款记录",
    NOTIFICATIONS: "消息提醒",
  };
  return escapeHtml(map[value] || humanizeCode(value));
}

function businessTypeText(value) {
  const map = {
    ORDER: "订单",
    PAYMENT: "支付",
    REFUND: "退款",
    TICKET: "电子票",
    TICKET_CHANGE: "改签",
    NOTIFICATION: "消息",
    RISK: "风险",
  };
  return escapeHtml(map[value] || humanizeCode(value));
}

function businessIdText(value) {
  if (!value) {
    return "-";
  }
  const text = String(value);
  const prefixes = {
    "ORDER:": "订单：",
    "PAYMENT:": "支付：",
    "REFUND:": "退款：",
    "TICKET:": "电子票：",
    "TICKET_CHANGE:": "改签单：",
    "NOTIFICATION:": "消息：",
    "RISK:": "风险：",
  };
  const prefix = Object.keys(prefixes).find(item => text.startsWith(item));
  return escapeHtml(prefix ? `${prefixes[prefix]}${text.slice(prefix.length)}` : text);
}

function userFacingHint(value) {
  const map = {
    ORDER_DETAIL: "查看订单详情",
    ORDER_PAYMENT: "完成订单支付",
    CHANGE_PAYMENT: "支付改签差额",
    CHANGE_DETAIL: "查看改签详情",
    REFUNDS: "查看退款进度",
    REFUND_RECORDS: "查看退款记录",
    NOTIFICATIONS: "查看消息提醒",
    TICKET_WALLET: "查看电子票",
  };
  const phraseMap = {
    "view order": "查看订单",
    "view ticket": "查看电子票",
    "go to payment": "去支付",
    "view refund": "查看退款",
    "refund -> order_detail": "查看退款相关订单",
    "payment -> order_detail": "查看支付相关订单",
    "ticket_change -> order_detail": "查看改签相关订单",
    "payment succeeded. check the order and ticket timeline": "支付已完成，请查看订单详情和电子票。",
    "the e-ticket is ready in your ticket wallet": "电子票已生成，可在电子票夹查看。",
    "this order is waiting for payment": "订单待支付，请尽快完成支付。",
    "refund succeeded. check the refund record": "退款已成功，请查看退款记录。",
    "the order has been closed": "订单已关闭。",
    "order closed. check the order detail": "订单已关闭，请查看订单详情。",
    "ticket issued. check the ticket wallet": "电子票已生成，可在电子票夹查看。",
    "refund failed. check the refund record": "退款失败，请查看退款记录。",
  };
  const text = String(value || "").trim();
  const normalized = text.toLowerCase().replace(/\s+/g, " ").replace(/\.+$/g, "");
  return map[value] || phraseMap[normalized] || humanizeCode(value);
}

function humanizeCode(value) {
  if (!value) {
    return "-";
  }
  const map = {
    SUCCESS: "成功",
    FAILED: "失败",
    PENDING: "待处理",
    PROCESSING: "处理中",
    DONE: "已完成",
    UNREAD: "未读",
    READ: "已读",
    PAYMENT_SUCCEEDED: "支付成功",
    PAYMENT_FAILED: "支付失败",
    ORDER: "订单",
    PAYMENT: "支付",
    REFUND: "退款",
    TICKET: "电子票",
    TICKET_CHANGE: "改签",
    NOTIFICATION: "消息",
    REFUND_SUCCEEDED: "退款成功",
    REFUND_FAILED: "退款失败",
    TICKET_CHANGE_CREATED: "已发起改签",
    TICKET_CHANGE_PENDING_PAYMENT: "改签待支付",
    TICKET_CHANGE_SUCCEEDED: "改签成功",
    TICKET_CHANGE_FAILED: "改签失败",
  };
  return map[value] || String(value).replace(/_/g, " ").toLowerCase().replace(/\b\w/g, char => char.toUpperCase());
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
    USER: "普通乘客",
    ADMIN: "系统管理员",
    OPERATOR: "运营人员",
    RISK_OFFICER: "风控专员",
  };
  return map[value] || humanizeCode(value);
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

function renderMiniOrders(container, orders, emptyText) {
  if (container === elements.upcomingTrips) {
    renderUpcomingJourneyTrips(container, orders, emptyText);
    return;
  }
  if (container !== elements.latestOrders) {
    renderCompactOrderEvents(container, orders, emptyText);
    return;
  }
  if (!orders.length) {
    container.innerHTML = journeyEmptyState(emptyText, "最近创建的订单会显示在这里。");
    return;
  }
  container.classList.add("journey-order-list");
  container.innerHTML = orders.slice(0, 6).map(order => {
    const info = journeyOrderDisplayInfo(order);
    return `
      <button class="journey-order-row" type="button" data-order-status-jump="${escapeHtml(order.status || "")}">
        <span class="journey-order-train">${escapeHtml(info.trainNo)}</span>
        <span class="journey-order-route">
          <strong>${escapeHtml(info.routeText)}</strong>
          <small>${escapeHtml(info.date)} · ${escapeHtml(info.seat)} · ${escapeHtml(info.passenger)}</small>
        </span>
        <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
        <span class="journey-order-amount">¥${escapeHtml(info.amount)}</span>
      </button>
    `;
  }).join("");
  container.querySelectorAll("[data-order-status-jump]").forEach(item => {
    item.addEventListener("click", () => {
      elements.orderStatus.value = item.dataset.orderStatusJump || "";
      activateSection("passenger-orders");
      loadPassengerOrders();
    });
  });
}

function renderCompactOrderEvents(container, orders, emptyText) {
  if (!orders.length) {
    container.innerHTML = emptyItem(emptyText);
    return;
  }
  container.innerHTML = orders.slice(0, 5).map(order => {
    const info = journeyOrderDisplayInfo(order);
    return `
      <article class="mini-order">
        <div>
          <strong>${escapeHtml(info.trainNo)} · ${escapeHtml(info.routeText)}</strong>
          <span>${escapeHtml(info.date)} · ${escapeHtml(info.seat)} · ${escapeHtml(info.passenger)}</span>
        </div>
        <small>¥${escapeHtml(info.amount)}</small>
      </article>
    `;
  }).join("");
}

function renderUpcomingJourneyTrips(container, orders, emptyText) {
  container.classList.add("journey-trip-list");
  if (!orders.length) {
    container.innerHTML = journeyEmptyState(emptyText, "已支付且未出行的车票会显示在这里。");
    return;
  }
  container.innerHTML = orders.slice(0, 4).map(order => {
    const info = journeyOrderDisplayInfo(order);
    const trip = passengerOrderTripInfo(order);
    const duration = journeyDurationText(trip.departureTime, trip.arrivalTime);
    const canOperate = order.status === "PAID";
    return `
      <article class="journey-trip-card journey-trip-card-redesign">
        <div class="journey-trip-top">
          <span class="journey-train-badge" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false">
              <path d="M7 5.5h10a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2Z"></path>
              <path d="M8 9h8M8 13h8M8 18l-2 2M16 18l2 2"></path>
            </svg>
          </span>
          <span class="journey-trip-title">
            <strong>${escapeHtml(info.trainNo)}</strong>
            <small>${escapeHtml(info.dateLabel)} · ${escapeHtml(info.seat)} · ${escapeHtml(info.passenger)}</small>
          </span>
          <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
        </div>
        <div class="journey-trip-route">
          <span class="journey-trip-stop">
            <strong>${escapeHtml(trip.departureStation)}</strong>
            <small>${escapeHtml(trip.departureTime)} 出发</small>
          </span>
          <span class="journey-trip-duration">
            <span class="journey-trip-track" aria-hidden="true"></span>
            <strong>${escapeHtml(duration)}</strong>
          </span>
          <span class="journey-trip-stop is-arrival">
            <strong>${escapeHtml(trip.arrivalStation)}</strong>
            <small>${escapeHtml(trip.arrivalTime)} 到达</small>
          </span>
        </div>
        <div class="journey-trip-footer">
          <span>订单 ${escapeHtml(order.orderNo || order.id || "-")}</span>
          <div class="journey-trip-actions">
            ${canOperate ? `<button class="secondary-button compact-button" type="button" data-journey-change="${order.id}">改签</button>` : ""}
            ${canOperate ? `<button class="secondary-button compact-button" type="button" data-journey-refund="${order.id}">退票</button>` : ""}
            <button class="primary-button compact-button" type="button" data-journey-detail="${order.id}">详情</button>
          </div>
        </div>
      </article>
    `;
  }).join("");
  container.querySelectorAll("[data-journey-detail]").forEach(button => {
    button.addEventListener("click", () => openPassengerOrderDetail(button.dataset.journeyDetail));
  });
  container.querySelectorAll("[data-journey-change]").forEach(button => {
    button.addEventListener("click", () => openChangeFromOrderId(button.dataset.journeyChange));
  });
  container.querySelectorAll("[data-journey-refund]").forEach(button => {
    button.addEventListener("click", () => refundPassengerOrder(button.dataset.journeyRefund));
  });
}

function journeyOrderDisplayInfo(order) {
  const trip = passengerOrderTripInfo(order);
  const routeText = trip.departureStation !== "-" && trip.arrivalStation !== "-"
    ? `${trip.departureStation} → ${trip.arrivalStation}`
    : passengerOrderRouteText(order);
  const hasTravelTimes = trip.departureTime !== "-" && trip.arrivalTime !== "-";
  const dateLabel = formatJourneyDateLabel(order.travelDate);
  return {
    trainNo: trip.trainNo,
    routeText,
    date: formatDate(order.travelDate),
    dateLabel,
    seat: seatTypeText(order.seatType) || "-",
    passenger: order.passengerName || order.travelerName || "-",
    amount: formatAmount(order.amount),
    duration: hasTravelTimes ? journeyDurationText(trip.departureTime, trip.arrivalTime) : "时间待定",
    timeRange: hasTravelTimes ? `${trip.departureTime} - ${trip.arrivalTime}` : (dateLabel !== "-" ? dateLabel : "时间待定"),
    departureStation: trip.departureStation,
    arrivalStation: trip.arrivalStation,
  };
}

function passengerOrderTripInfo(order) {
  const train = findTrainForOrder(order) || {};
  const routeParts = splitJourneyRouteText(passengerOrderRouteText(order));
  const departureStation = firstJourneyValue(
    order.departureStation,
    order.fromStation,
    order.departureStationName,
    order.fromStationName,
    order.fromName,
    order.startStation,
    order.startStationName,
    order.originStation,
    train.departureStation,
    routeParts[0],
  );
  const arrivalStation = firstJourneyValue(
    order.arrivalStation,
    order.toStation,
    order.arrivalStationName,
    order.toStationName,
    order.toName,
    order.endStation,
    order.endStationName,
    order.destinationStation,
    train.arrivalStation,
    routeParts[1],
  );
  return {
    trainNo: firstJourneyValue(order.trainNo, train.trainNo),
    departureStation,
    arrivalStation,
    departureTime: formatTime(firstJourneyValue(
      order.departureTime,
      order.departTime,
      order.startTime,
      order.scheduledDepartureTime,
      train.departureTime,
    )),
    arrivalTime: formatTime(firstJourneyValue(
      order.arrivalTime,
      order.arriveTime,
      order.endTime,
      order.scheduledArrivalTime,
      train.arrivalTime,
    )),
  };
}

function firstJourneyValue(...values) {
  const value = values.find(item => {
    if (item == null) {
      return false;
    }
    const text = String(item).trim();
    return text && text !== "-";
  });
  return value == null ? "-" : String(value).trim();
}

function firstJourneyStation(...values) {
  const station = values.map(normalizeJourneyStation).find(value => value && value !== "-");
  return station || "-";
}

function normalizeJourneyStation(value) {
  if (value == null) {
    return "";
  }
  if (typeof value === "object") {
    return firstJourneyStation(
      value.name,
      value.stationName,
      value.displayName,
      value.label,
      value.code,
      value.stationCode,
    );
  }
  const text = String(value).trim();
  if (!text || text === "-") {
    return "";
  }
  const station = passengerState.stations.find(item => item
    && (String(item.code || "").trim() === text || String(item.name || "").trim() === text));
  return station && station.name ? station.name : text;
}

function splitJourneyRouteText(value) {
  const text = value ? String(value) : "";
  if (!text || text.includes(" / ") || text.includes(" · ")) {
    return [];
  }
  const separator = text.includes("→") ? "→" : (text.includes("->") ? "->" : "");
  if (!separator) {
    return [];
  }
  return text.split(separator).map(part => part.trim()).filter(Boolean);
}

function journeyDurationText(start, end) {
  const startMinutes = timeToMinutes(start);
  const endMinutes = timeToMinutes(end);
  if (startMinutes === null || endMinutes === null) {
    return "行程";
  }
  let diff = endMinutes - startMinutes;
  if (diff < 0) {
    diff += 24 * 60;
  }
  const hours = Math.floor(diff / 60);
  const minutes = diff % 60;
  if (!hours) {
    return `${minutes}分钟`;
  }
  return minutes ? `${hours}时${minutes}分` : `${hours}小时`;
}

function formatJourneyDateLabel(value) {
  const dateText = formatDate(value);
  if (!dateText || dateText === "-") {
    return "-";
  }
  const date = new Date(`${dateText}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return dateText;
  }
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const weekdays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
  return `${month}月${day}日 ${weekdays[date.getDay()]}`;
}

function journeyEmptyState(title, message) {
  return `
    <div class="journey-empty-state">
      <strong>${escapeHtml(title)}</strong>
      <span>${escapeHtml(message)}</span>
    </div>
  `;
}

function renderPassengerOrders(orders) {
  if (!orders.length) {
    elements.orderCards.innerHTML = emptyItem("暂无订单，可先查询车次并下单。");
    return;
  }
  elements.orderCards.innerHTML = orders.map(order => {
    const info = journeyOrderDisplayInfo(order);
    return `
      <button class="passenger-order-card passenger-order-card-compact" type="button" data-passenger-order-card="${escapeHtml(String(order.id || ""))}" aria-label="查看订单详情：${escapeHtml(info.trainNo)}，${escapeHtml(info.departureStation)}到${escapeHtml(info.arrivalStation)}">
        <span class="passenger-order-display-grid">
          <span class="passenger-order-train">${escapeHtml(info.trainNo)}</span>
          <span class="passenger-order-route">
            <span class="passenger-order-station passenger-order-station-departure">
              <strong>${escapeHtml(info.departureStation)}</strong>
            </span>
            <span class="passenger-order-arrow" aria-hidden="true"></span>
            <span class="passenger-order-station passenger-order-station-arrival">
              <strong>${escapeHtml(info.arrivalStation)}</strong>
            </span>
          </span>
          <span class="passenger-order-time">
            <strong>${escapeHtml(info.timeRange)}</strong>
            <em>${escapeHtml(info.travelDateLabel)}</em>
          </span>
          <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
          <span class="passenger-order-amount">¥${escapeHtml(info.amount)}</span>
        </span>
      </button>
    `;
  }).join("");
  elements.orderCards.querySelectorAll("[data-passenger-order-card]").forEach(card => {
    card.addEventListener("click", () => {
      if (card.dataset.passengerOrderCard) {
        openPassengerOrderDetail(card.dataset.passengerOrderCard);
      }
    });
  });
}

function renderMiniOrders(container, orders, emptyText) {
  if (container === elements.upcomingTrips) {
    renderUpcomingJourneyTrips(container, orders, emptyText);
    return;
  }
  if (container !== elements.latestOrders) {
    renderCompactOrderEvents(container, orders, emptyText);
    return;
  }
  if (!orders.length) {
    container.innerHTML = journeyEmptyState(emptyText, "最近创建的订单会显示在这里。");
    return;
  }
  container.classList.add("journey-order-list");
  container.innerHTML = orders.slice(0, 6).map(order => {
    const info = journeyOrderDisplayInfo(order);
    return `
      <button class="journey-order-row" type="button" data-order-status-jump="${escapeHtml(order.status || "")}">
        <span class="journey-order-train">${escapeHtml(info.trainNo)}</span>
        <span class="journey-order-station">
          <small>出发点</small>
          <strong>${escapeHtml(info.departureStation)}</strong>
        </span>
        <span class="journey-order-station">
          <small>到达点</small>
          <strong>${escapeHtml(info.arrivalStation)}</strong>
        </span>
        <span class="status ${orderStatusClass(order.status)}">${statusText(order.status)}</span>
        <span class="journey-order-amount">¥${escapeHtml(info.amount)}</span>
      </button>
    `;
  }).join("");
  container.querySelectorAll("[data-order-status-jump]").forEach(item => {
    item.addEventListener("click", () => {
      elements.orderStatus.value = item.dataset.orderStatusJump || "";
      activateSection("passenger-orders");
      loadPassengerOrders();
    });
  });
}

function renderCompactOrderEvents(container, orders, emptyText) {
  if (!orders.length) {
    container.innerHTML = emptyItem(emptyText);
    return;
  }
  container.innerHTML = orders.slice(0, 5).map(order => {
    const info = journeyOrderDisplayInfo(order);
    return `
      <article class="mini-order">
        <div>
          <strong>${escapeHtml(info.trainNo)} · ${escapeHtml(info.departureStation)} → ${escapeHtml(info.arrivalStation)}</strong>
          <span>${statusText(order.status)}</span>
        </div>
        <small>¥${escapeHtml(info.amount)}</small>
      </article>
    `;
  }).join("");
}

function journeyOrderDisplayInfo(order) {
  const trip = passengerOrderTripInfo(order);
  const departureStation = firstJourneyValue(trip.departureStation);
  const arrivalStation = firstJourneyValue(trip.arrivalStation);
  const hasTravelTimes = trip.departureTime !== "-" && trip.arrivalTime !== "-";
  const dateLabel = formatJourneyDateLabel(order.travelDate);
  const travelDateLabel = formatJourneyFullDateLabel(order.travelDate);
  return {
    trainNo: firstJourneyValue(trip.trainNo, order.trainNo),
    departureStation,
    arrivalStation,
    routeText: departureStation !== "-" && arrivalStation !== "-"
      ? `${departureStation} → ${arrivalStation}`
      : passengerOrderRouteText(order),
    date: formatDate(order.travelDate),
    dateLabel,
    seat: seatTypeText(order.seatType) || "-",
    passenger: order.passengerName || order.travelerName || "-",
    amount: formatAmount(order.amount),
    duration: hasTravelTimes ? journeyDurationText(trip.departureTime, trip.arrivalTime) : "时间待定",
    timeRange: hasTravelTimes ? `${trip.departureTime} - ${trip.arrivalTime}` : (dateLabel !== "-" ? dateLabel : "时间待定"),
    travelDateLabel,
  };
}

function passengerOrderRouteText(order) {
  const trip = passengerOrderTripInfo(order);
  return trip.departureStation !== "-" && trip.arrivalStation !== "-"
    ? `${trip.departureStation} → ${trip.arrivalStation}`
    : `${trip.trainNo || order.trainNo || "-"} · ${formatDate(order.travelDate) || "-"}`;
}

function passengerOrderTripInfo(order) {
  const train = findTrainForOrder(order) || {};
  const nestedTrain = order.train || order.trainInfo || order.trainResponse || {};
  const nestedTicket = order.ticket || order.ticketRecord || order.eTicket || {};
  const nestedInventoryTrain = order.inventory && order.inventory.train ? order.inventory.train : {};
  const routeParts = splitJourneyRouteText(
    firstJourneyValue(order.routeText, order.route, order.stationPair, ""),
  );
  const departureStation = firstJourneyStation(
    order.departureStation,
    order.fromStation,
    order.departureStationName,
    order.fromStationName,
    order.fromName,
    order.departureName,
    order.originName,
    order.startStation,
    order.startStationName,
    order.startName,
    order.originStation,
    order.originStationName,
    order.departureStationCode,
    order.fromStationCode,
    order.startStationCode,
    nestedTicket.departureStation,
    nestedTicket.fromStation,
    nestedTicket.departureStationName,
    nestedTicket.fromStationName,
    nestedTrain.departureStation,
    nestedTrain.fromStation,
    nestedTrain.departureStationName,
    nestedTrain.fromStationName,
    nestedInventoryTrain.departureStation,
    nestedInventoryTrain.fromStation,
    train.departureStation,
    train.fromStation,
    train.departureStationName,
    train.fromStationName,
    routeParts[0],
  );
  const arrivalStation = firstJourneyStation(
    order.arrivalStation,
    order.toStation,
    order.arrivalStationName,
    order.toStationName,
    order.toName,
    order.arrivalName,
    order.destinationName,
    order.endStation,
    order.endStationName,
    order.endName,
    order.destinationStation,
    order.destinationStationName,
    order.arrivalStationCode,
    order.toStationCode,
    order.endStationCode,
    nestedTicket.arrivalStation,
    nestedTicket.toStation,
    nestedTicket.arrivalStationName,
    nestedTicket.toStationName,
    nestedTrain.arrivalStation,
    nestedTrain.toStation,
    nestedTrain.arrivalStationName,
    nestedTrain.toStationName,
    nestedInventoryTrain.arrivalStation,
    nestedInventoryTrain.toStation,
    train.arrivalStation,
    train.toStation,
    train.arrivalStationName,
    train.toStationName,
    routeParts[1],
  );
  return {
    trainNo: firstJourneyValue(order.trainNo, train.trainNo),
    departureStation,
    arrivalStation,
    departureTime: formatTime(firstJourneyValue(
      order.departureTime,
      order.departTime,
      order.startTime,
      order.scheduledDepartureTime,
      nestedTicket.departureTime,
      nestedTicket.departTime,
      nestedTicket.startTime,
      nestedTrain.departureTime,
      nestedTrain.departTime,
      nestedTrain.startTime,
      nestedInventoryTrain.departureTime,
      nestedInventoryTrain.departTime,
      nestedInventoryTrain.startTime,
      train.departureTime,
    )),
    arrivalTime: formatTime(firstJourneyValue(
      order.arrivalTime,
      order.arriveTime,
      order.endTime,
      order.scheduledArrivalTime,
      nestedTicket.arrivalTime,
      nestedTicket.arriveTime,
      nestedTicket.endTime,
      nestedTrain.arrivalTime,
      nestedTrain.arriveTime,
      nestedTrain.endTime,
      nestedInventoryTrain.arrivalTime,
      nestedInventoryTrain.arriveTime,
      nestedInventoryTrain.endTime,
      train.arrivalTime,
    )),
  };
}

function splitJourneyRouteText(value) {
  const text = value ? String(value).trim() : "";
  if (!text) {
    return [];
  }
  const separator = text.includes("→") ? "→" : (text.includes("->") ? "->" : "");
  if (!separator) {
    return [];
  }
  return text.split(separator).map(part => part.trim()).filter(Boolean);
}

function journeyDurationText(start, end) {
  const startMinutes = timeToMinutes(start);
  const endMinutes = timeToMinutes(end);
  if (startMinutes === null || endMinutes === null) {
    return "行程";
  }
  let diff = endMinutes - startMinutes;
  if (diff < 0) {
    diff += 24 * 60;
  }
  const hours = Math.floor(diff / 60);
  const minutes = diff % 60;
  if (!hours) {
    return `${minutes}分钟`;
  }
  return minutes ? `${hours}时${minutes}分` : `${hours}小时`;
}

function formatJourneyDateLabel(value) {
  const dateText = formatDate(value);
  if (!dateText || dateText === "-") {
    return "-";
  }
  const date = new Date(`${dateText}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return dateText;
  }
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const weekdays = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
  return `${month}月${day}日 ${weekdays[date.getDay()]}`;
}

function formatJourneyFullDateLabel(value) {
  const dateText = formatDate(value);
  if (!dateText || dateText === "-") {
    return "日期待定";
  }
  const date = new Date(`${dateText}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return dateText;
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
}
