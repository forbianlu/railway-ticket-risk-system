(() => {
  const DEFAULT_DURATION = 2050;
  const REDUCED_DURATION = 700;
  const PREPARE_REVEAL_AT = 1575;

  function escapeHtml(value) {
    return String(value || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function runPrepareReveal(options) {
    if (typeof options.prepareReveal !== "function") {
      return;
    }
    try {
      options.prepareReveal();
    } catch (error) {
      console.error(error);
    }
  }

  function createOverlay(options) {
    const variant = options.variant || "passenger";
    const label = escapeHtml(options.label || "\u94c1\u8def\u5ba2\u8fd0\u7968\u52a1");
    const destination = escapeHtml(options.destination || "");
    const detail = destination ? `${destination}\u6b63\u5728\u8fdb\u5165` : "\u6b63\u5728\u8fdb\u5165";
    const overlay = document.createElement("div");
    overlay.className = `rt-login-transition rt-login-transition--${variant}`;
    overlay.setAttribute("aria-hidden", "true");
    overlay.innerHTML = `
      <div class="rt-blue-wash" aria-hidden="true">
        <span class="rt-blue-fill"></span>
        <span class="rt-wash-orb rt-wash-orb-a"></span>
        <span class="rt-wash-orb rt-wash-orb-b"></span>
        <span class="rt-wash-orb rt-wash-orb-c"></span>
      </div>
      <div class="rt-logo-stage" aria-hidden="true">
        <div class="rt-logo-shell" role="img" aria-label="RT">
          <svg class="rt-write-logo" viewBox="18 18 104 58" aria-hidden="true">
            <g class="rt-write-paths">
              <path class="rt-write-stroke rt-write-stroke-1" pathLength="100" d="M30 25 L30 67" />
              <path class="rt-write-stroke rt-write-stroke-2" pathLength="100" d="M30 25 H49 C59 25 65 29 65 36 C65 43 59 47 49 47 H30" />
              <path class="rt-write-stroke rt-write-stroke-3" pathLength="100" d="M40 47 L68 67" />
              <path class="rt-write-stroke rt-write-stroke-4" pathLength="100" d="M76 25 H112" />
              <path class="rt-write-stroke rt-write-stroke-5" pathLength="100" d="M94 25 V67" />
            </g>
          </svg>
        </div>
        <div class="rt-logo-caption">
          <strong>${label}</strong>
          <span>${detail}</span>
        </div>
      </div>
      <div class="rt-expander" aria-hidden="true">
        <div class="rt-cutout-layer"></div>
      </div>
    `;
    return overlay;
  }

  window.playRailwayLoginTransition = function playRailwayLoginTransition(options = {}) {
    if (document.body.classList.contains("rt-login-transitioning")) {
      return Promise.resolve();
    }

    const prefersReducedMotion = window.matchMedia
      && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const overlay = createOverlay(options);
    const duration = prefersReducedMotion ? REDUCED_DURATION : DEFAULT_DURATION;
    let prepared = false;
    let prepareTimer = null;

    const prepareReveal = () => {
      if (prepared) {
        return;
      }
      prepared = true;
      runPrepareReveal(options);
      overlay.classList.add("is-revealing");
    };

    document.body.classList.add("rt-login-transitioning");
    document.body.appendChild(overlay);

    return new Promise((resolve) => {
      prepareTimer = window.setTimeout(prepareReveal, prefersReducedMotion ? 0 : PREPARE_REVEAL_AT);
      window.setTimeout(() => {
        if (prepareTimer) {
          window.clearTimeout(prepareTimer);
        }
        prepareReveal();
        overlay.classList.add("is-leaving");
        window.setTimeout(() => {
          overlay.remove();
          document.body.classList.remove("rt-login-transitioning");
          resolve();
        }, prefersReducedMotion ? 120 : 300);
      }, duration);
    });
  };
})();
