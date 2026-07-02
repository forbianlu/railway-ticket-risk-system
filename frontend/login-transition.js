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
            <defs>
              <clipPath id="rtWriteClip">
                <rect x="18" y="18" width="104" height="49" />
              </clipPath>
            </defs>
            <g class="rt-write-paths" clip-path="url(#rtWriteClip)">
              <path class="rt-write-stroke rt-write-stroke-1" pathLength="100" d="M30 25 L30 67" />
              <path class="rt-write-stroke rt-write-stroke-2" pathLength="100" d="M23.6 25 H47 C56 25 62 29 62 36 C62 43 56 47 47 47 H30" />
              <path class="rt-write-stroke rt-write-stroke-3" pathLength="100" d="M40 47 L81 82" />
              <path class="rt-write-stroke rt-write-stroke-4" pathLength="100" d="M74 25 H116" />
              <path class="rt-write-stroke rt-write-stroke-5" pathLength="100" d="M95 25 V67" />
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

  const FORGOTTEN_TITLE_HTML = "<span>\u563f\u563f\u563f</span><span>\u5fd8\u8bb0\u5bc6\u7801\u4e86\u6211\u4eec\u4e5f\u65e0\u80fd\u4e3a\u529b\uff0c<br>\u529f\u80fd\u672a\u6dfb\u52a0</span>";

  function setupLiquidReveal() {
    if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      return;
    }

    document.querySelectorAll(".login-screen:not(.passenger-login)").forEach((screen) => {
      if (screen.dataset.liquidRevealReady === "true") {
        return;
      }

      screen.dataset.liquidRevealReady = "true";

      const canvas = document.createElement("canvas");
      canvas.className = "login-liquid-canvas";
      canvas.setAttribute("aria-hidden", "true");
      screen.appendChild(canvas);

      const ctx = canvas.getContext("2d", { alpha: true });
      if (!ctx) {
        canvas.remove();
        return;
      }

      const trailCanvas = document.createElement("canvas");
      const trailCtx = trailCanvas.getContext("2d", { alpha: true });
      const maskCanvas = document.createElement("canvas");
      const maskCtx = maskCanvas.getContext("2d", { alpha: true });
      if (!trailCtx || !maskCtx) {
        canvas.remove();
        return;
      }

      const card = screen.querySelector(".login-card");
      const pointer = { x: 0, y: 0, px: 0, py: 0 };
      const smooth = { x: 0, y: 0, px: 0, py: 0 };
      const lastSpawn = { x: 0, y: 0 };
      const momentum = { x: 0, y: 0 };
      const particles = [];
      let initialized = false;
      let active = false;
      let rafId = 0;
      let width = 1;
      let height = 1;
      let dpr = 1;
      let flowAngle = 0;
      let hasFlowAngle = false;
      let lastMoveAt = 0;

      function resizeCanvas() {
        const rect = screen.getBoundingClientRect();
        dpr = Math.min(window.devicePixelRatio || 1, 1.75);
        width = Math.max(1, Math.round(rect.width * dpr));
        height = Math.max(1, Math.round(rect.height * dpr));

        [canvas, trailCanvas, maskCanvas].forEach((item) => {
          item.width = width;
          item.height = height;
        });

        canvas.style.width = `${rect.width}px`;
        canvas.style.height = `${rect.height}px`;
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        trailCtx.setTransform(1, 0, 0, 1, 0, 0);
        maskCtx.setTransform(1, 0, 0, 1, 0, 0);
        particles.length = 0;
        ctx.clearRect(0, 0, width, height);
        trailCtx.clearRect(0, 0, width, height);
        maskCtx.clearRect(0, 0, width, height);
      }

      function setInitialPosition() {
        const rect = screen.getBoundingClientRect();
        pointer.x = rect.width * 0.34 * dpr;
        pointer.y = rect.height * 0.50 * dpr;
        pointer.px = pointer.x;
        pointer.py = pointer.y;
        smooth.x = pointer.x;
        smooth.y = pointer.y;
        smooth.px = smooth.x;
        smooth.py = smooth.y;
        lastSpawn.x = pointer.x;
        lastSpawn.y = pointer.y;
        initialized = true;
      }

      function shortestAngleDelta(from, to) {
        let delta = to - from;
        while (delta > Math.PI) {
          delta -= Math.PI * 2;
        }
        while (delta < -Math.PI) {
          delta += Math.PI * 2;
        }
        return delta;
      }

      function addParticle(x, y, speed, angle, order) {
        const speedUnit = Math.min(1, speed / (210 * dpr));
        const jitter = (6 + order * 0.8) * dpr;
        const reverse = Math.min(4.8 * dpr, speed * 0.018);
        const side = (Math.random() - 0.5) * jitter;
        const radius = (54 + speedUnit * 72 + Math.random() * 18) * dpr;
        const life = 34 + Math.round(speedUnit * 30) + Math.round(Math.random() * 10);

        particles.push({
          x: x + Math.sin(angle) * side,
          y: y - Math.cos(angle) * side,
          vx: -Math.cos(angle) * reverse + (Math.random() - 0.5) * 1.2 * dpr,
          vy: -Math.sin(angle) * reverse + (Math.random() - 0.5) * 1.2 * dpr,
          radius,
          age: 0,
          life,
          maxLife: life,
          angle,
          stretch: 0.45 + speedUnit * 1.35,
          swirl: (Math.random() - 0.5) * 0.04,
        });

        if (particles.length > 90) {
          particles.splice(0, particles.length - 90);
        }
      }

      function spawnTrail(fromX, fromY, toX, toY) {
        const dx = toX - fromX;
        const dy = toY - fromY;
        const distance = Math.hypot(dx, dy);
        if (distance < 0.7 * dpr) {
          return;
        }

        const rawAngle = Math.atan2(dy, dx);
        if (!hasFlowAngle) {
          flowAngle = rawAngle;
          hasFlowAngle = true;
        } else {
          flowAngle += shortestAngleDelta(flowAngle, rawAngle) * 0.11;
        }

        momentum.x += (dx - momentum.x) * 0.16;
        momentum.y += (dy - momentum.y) * 0.16;
        const speed = Math.hypot(momentum.x, momentum.y);
        const step = Math.max(11 * dpr, Math.min(24 * dpr, 28 * dpr - speed * 0.025));
        const count = Math.max(1, Math.min(12, Math.floor(distance / step) + 1));

        for (let index = 0; index < count; index += 1) {
          const t = (index + 1) / count;
          addParticle(
            fromX + dx * t,
            fromY + dy * t,
            speed,
            flowAngle,
            index
          );
        }
      }

      function stopReveal() {
        active = false;
        requestFrame();
      }

      function requestFrame() {
        if (!rafId) {
          rafId = window.requestAnimationFrame(render);
        }
      }

      function updatePointer(clientX, clientY, eventTarget) {
        if (card && eventTarget && card.contains(eventTarget)) {
          stopReveal();
          return;
        }

        const rect = screen.getBoundingClientRect();
        const nextX = clientX - rect.left;
        const nextY = clientY - rect.top;
        const inside = nextX >= 0 && nextY >= 0 && nextX <= rect.width && nextY <= rect.height;

        if (!inside) {
          stopReveal();
          return;
        }

        if (!initialized) {
          pointer.x = nextX * dpr;
          pointer.y = nextY * dpr;
          pointer.px = pointer.x;
          pointer.py = pointer.y;
          smooth.x = pointer.x;
          smooth.y = pointer.y;
          smooth.px = smooth.x;
          smooth.py = smooth.y;
          lastSpawn.x = pointer.x;
          lastSpawn.y = pointer.y;
          initialized = true;
          return;
        }

        pointer.px = pointer.x;
        pointer.py = pointer.y;
        pointer.x = nextX * dpr;
        pointer.y = nextY * dpr;
        spawnTrail(lastSpawn.x, lastSpawn.y, pointer.x, pointer.y);
        lastSpawn.x = pointer.x;
        lastSpawn.y = pointer.y;
        lastMoveAt = performance.now();
        active = true;
        requestFrame();
      }

      function render(time) {
        rafId = 0;

        if (!initialized) {
          setInitialPosition();
        }

        const moving = active && time - lastMoveAt < 90;
        if (!moving) {
          active = false;
        }

        smooth.px = smooth.x;
        smooth.py = smooth.y;
        smooth.x += (pointer.x - smooth.x) * 0.14;
        smooth.y += (pointer.y - smooth.y) * 0.14;
        momentum.x *= moving ? 0.93 : 0.86;
        momentum.y *= moving ? 0.93 : 0.86;

        trailCtx.clearRect(0, 0, width, height);

        trailCtx.save();
        trailCtx.globalCompositeOperation = "lighter";
        trailCtx.lineCap = "round";
        trailCtx.lineJoin = "round";
        for (let index = 1; index < particles.length; index += 1) {
          const previous = particles[index - 1];
          const current = particles[index];
          const distance = Math.hypot(current.x - previous.x, current.y - previous.y);
          if (distance > 155 * dpr) {
            continue;
          }
          const previousLife = Math.max(0, previous.life / previous.maxLife);
          const currentLife = Math.max(0, current.life / current.maxLife);
          const alpha = Math.min(previousLife, currentLife) * 0.34;
          if (alpha <= 0.01) {
            continue;
          }
          trailCtx.globalAlpha = alpha;
          trailCtx.strokeStyle = "#ffffff";
          trailCtx.lineWidth = Math.max(8 * dpr, Math.min(previous.radius, current.radius) * 0.42);
          trailCtx.beginPath();
          trailCtx.moveTo(previous.x, previous.y);
          trailCtx.quadraticCurveTo(
            (previous.x + current.x) / 2,
            (previous.y + current.y) / 2,
            current.x,
            current.y
          );
          trailCtx.stroke();
        }
        trailCtx.restore();

        for (let index = particles.length - 1; index >= 0; index -= 1) {
          const particle = particles[index];
          particle.age += 1;
          particle.life -= 1;
          if (particle.life <= 0) {
            particles.splice(index, 1);
            continue;
          }

          const lifeRatio = Math.max(0, particle.life / particle.maxLife);
          const calm = 1 - lifeRatio;
          particle.x += particle.vx + Math.sin(time * 0.002 + index) * particle.swirl * particle.radius;
          particle.y += particle.vy + Math.cos(time * 0.0017 + index * 0.7) * particle.swirl * particle.radius;
          particle.vx *= 0.94;
          particle.vy *= 0.94;
          particle.stretch *= 0.955;
          particle.angle += particle.swirl * 0.55;

          const alpha = Math.pow(lifeRatio, 1.18) * 0.95;
          const radius = particle.radius * (0.82 + calm * 0.22);
          const stretchX = radius * (1 + particle.stretch * lifeRatio);
          const stretchY = radius * (0.64 + calm * 0.22);

          trailCtx.save();
          trailCtx.translate(particle.x, particle.y);
          trailCtx.rotate(particle.angle);
          trailCtx.scale(stretchX, stretchY);
          const gradient = trailCtx.createRadialGradient(0, 0, 0, 0, 0, 1);
          gradient.addColorStop(0, `rgba(255, 255, 255, ${alpha})`);
          gradient.addColorStop(0.48, `rgba(255, 255, 255, ${alpha * 0.74})`);
          gradient.addColorStop(0.74, `rgba(255, 255, 255, ${alpha * 0.18})`);
          gradient.addColorStop(1, "rgba(255, 255, 255, 0)");
          trailCtx.fillStyle = gradient;
          trailCtx.beginPath();
          trailCtx.arc(0, 0, 1, 0, Math.PI * 2);
          trailCtx.fill();
          trailCtx.restore();
        }

        if (moving) {
          const headSpeed = Math.min(1, Math.hypot(momentum.x, momentum.y) / (160 * dpr));
          const headRadius = (76 + headSpeed * 76) * dpr;
          trailCtx.save();
          trailCtx.translate(smooth.x, smooth.y);
          trailCtx.rotate(flowAngle);
          trailCtx.scale(headRadius * (1.18 + headSpeed * 0.55), headRadius * 0.78);
          const headGradient = trailCtx.createRadialGradient(0, 0, 0, 0, 0, 1);
          headGradient.addColorStop(0, "rgba(255, 255, 255, 0.94)");
          headGradient.addColorStop(0.52, "rgba(255, 255, 255, 0.66)");
          headGradient.addColorStop(0.84, "rgba(255, 255, 255, 0.18)");
          headGradient.addColorStop(1, "rgba(255, 255, 255, 0)");
          trailCtx.fillStyle = headGradient;
          trailCtx.beginPath();
          trailCtx.arc(0, 0, 1, 0, Math.PI * 2);
          trailCtx.fill();
          trailCtx.restore();
        }

        maskCtx.clearRect(0, 0, width, height);
        maskCtx.save();
        maskCtx.filter = `blur(${Math.max(6, 9 * dpr)}px) contrast(180%)`;
        maskCtx.drawImage(trailCanvas, 0, 0);
        maskCtx.restore();

        ctx.clearRect(0, 0, width, height);
        ctx.save();
        ctx.filter = `blur(${Math.max(0.8, 1.2 * dpr)}px)`;
        ctx.drawImage(maskCanvas, 0, 0);
        ctx.globalCompositeOperation = "source-in";
        const fill = ctx.createLinearGradient(0, 0, width, height);
        fill.addColorStop(0, "#1d4ed8");
        fill.addColorStop(0.48, "#2563eb");
        fill.addColorStop(1, "#60a5fa");
        ctx.fillStyle = fill;
        ctx.fillRect(0, 0, width, height);
        ctx.restore();

        ctx.save();
        ctx.globalCompositeOperation = "source-atop";
        const sheen = ctx.createRadialGradient(
          smooth.x,
          smooth.y,
          0,
          smooth.x,
          smooth.y,
          260 * dpr
        );
        sheen.addColorStop(0, moving ? "rgba(255, 255, 255, 0.16)" : "rgba(255, 255, 255, 0.06)");
        sheen.addColorStop(0.52, moving ? "rgba(191, 219, 254, 0.12)" : "rgba(191, 219, 254, 0.04)");
        sheen.addColorStop(1, "rgba(255, 255, 255, 0)");
        ctx.fillStyle = sheen;
        ctx.fillRect(0, 0, width, height);
        ctx.restore();

        if (active || particles.length) {
          requestFrame();
        } else {
          ctx.clearRect(0, 0, width, height);
          trailCtx.clearRect(0, 0, width, height);
          maskCtx.clearRect(0, 0, width, height);
        }
      }

      resizeCanvas();
      screen.addEventListener("pointermove", (event) => {
        updatePointer(event.clientX, event.clientY, event.target);
      });
      screen.addEventListener("pointerleave", stopReveal);
      screen.addEventListener("pointercancel", stopReveal);
      window.addEventListener("resize", () => {
        resizeCanvas();
        setInitialPosition();
        requestFrame();
      });
      window.addEventListener("blur", stopReveal);
    });
  }

  function setupForgotPasswordHint() {
    document.querySelectorAll("[data-forgot-password-trigger]").forEach((button) => {
      button.addEventListener("click", () => {
        const loginScreen = button.closest(".login-screen");
        const titles = loginScreen ? loginScreen.querySelectorAll("[data-login-title]") : [];
        if (!titles.length) {
          return;
        }
        titles.forEach((title) => {
          title.classList.add("login-title-forgotten");
          title.innerHTML = FORGOTTEN_TITLE_HTML;
        });
      });
    });
  }

  setupLiquidReveal();
  setupForgotPasswordHint();
})();
