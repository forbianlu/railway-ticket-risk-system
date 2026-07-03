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

  function getRevealTextRoots(screen) {
    return [
      screen.querySelector(".login-back-link"),
      screen.querySelector(".login-visual"),
      screen.querySelector(".passenger-gateway-copy"),
    ].filter(Boolean);
  }

  function isHiddenElement(element) {
    if (!element || element.closest(".login-card")) {
      return true;
    }
    const style = window.getComputedStyle(element);
    return style.display === "none"
      || style.visibility === "hidden"
      || Number(style.opacity) <= 0.01;
  }

  function getCanvasFont(style, scale) {
    const fontSize = Math.max(1, Number.parseFloat(style.fontSize) * scale);
    const fontStyle = style.fontStyle || "normal";
    const fontVariant = style.fontVariant || "normal";
    const fontWeight = style.fontWeight || "400";
    const fontFamily = style.fontFamily || "sans-serif";
    return `${fontStyle} ${fontVariant} ${fontWeight} ${fontSize}px ${fontFamily}`;
  }

  function drawTextNode(ctx, node, screenRect, scale) {
    const value = node.nodeValue || "";
    if (!value.trim()) {
      return;
    }

    const parent = node.parentElement;
    if (isHiddenElement(parent)) {
      return;
    }

    const style = window.getComputedStyle(parent);
    const range = document.createRange();
    const fontSize = Math.max(1, Number.parseFloat(style.fontSize) * scale);
    ctx.font = getCanvasFont(style, scale);
    ctx.textAlign = "left";
    ctx.textBaseline = "top";
    ctx.fillStyle = "rgba(255, 255, 255, 0.96)";
    if ("fontKerning" in ctx) {
      ctx.fontKerning = style.fontKerning || "auto";
    }

    for (let index = 0; index < value.length; index += 1) {
      const character = value[index];
      if (!character || !character.trim()) {
        continue;
      }

      range.setStart(node, index);
      range.setEnd(node, index + 1);
      const rect = range.getBoundingClientRect();
      if (rect.width <= 0 || rect.height <= 0) {
        continue;
      }

      const x = (rect.left - screenRect.left) * scale;
      const y = (rect.top - screenRect.top) * scale + fontSize * 0.31;
      ctx.fillText(character, x, y);
    }

    range.detach();
  }

  function drawRevealText(ctx, screen, scale) {
    const roots = getRevealTextRoots(screen);
    if (!roots.length) {
      return;
    }

    const screenRect = screen.getBoundingClientRect();
    const walkerFilter = {
      acceptNode(node) {
        return node.nodeValue && node.nodeValue.trim()
          ? NodeFilter.FILTER_ACCEPT
          : NodeFilter.FILTER_REJECT;
      },
    };

    ctx.save();
    ctx.globalCompositeOperation = "source-atop";
    ctx.shadowColor = "rgba(15, 23, 42, 0.10)";
    ctx.shadowBlur = 0.4 * scale;
    ctx.shadowOffsetY = 0.4 * scale;

    roots.forEach((root) => {
      if (isHiddenElement(root)) {
        return;
      }
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, walkerFilter);
      let node = walker.nextNode();
      while (node) {
        drawTextNode(ctx, node, screenRect, scale);
        node = walker.nextNode();
      }
    });

    ctx.restore();
  }

  function setupLiquidReveal() {
    if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      return;
    }

    document.querySelectorAll(".login-screen").forEach((screen) => {
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
        const jitter = (8 + order * 1.1) * dpr;
        const forward = Math.min(8.6 * dpr, speed * 0.032);
        const dragBack = Math.min(2.4 * dpr, speed * 0.007);
        const side = (Math.random() - 0.5) * jitter;
        const radius = (76 + speedUnit * 102 + Math.random() * 22) * dpr;
        const life = 78 + Math.round(speedUnit * 58) + Math.round(Math.random() * 22);

        particles.push({
          x: x + Math.sin(angle) * side,
          y: y - Math.cos(angle) * side,
          vx: Math.cos(angle) * (forward - dragBack) + (Math.random() - 0.5) * 1.1 * dpr,
          vy: Math.sin(angle) * (forward - dragBack) + (Math.random() - 0.5) * 1.1 * dpr,
          radius,
          age: 0,
          life,
          maxLife: life,
          angle,
          stretch: 0.18 + speedUnit * 0.42,
          swirl: (Math.random() - 0.5) * 0.04,
        });

        if (particles.length > 128) {
          particles.splice(0, particles.length - 128);
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
          flowAngle += shortestAngleDelta(flowAngle, rawAngle) * 0.065;
        }

        momentum.x += (dx - momentum.x) * 0.11;
        momentum.y += (dy - momentum.y) * 0.11;
        const speed = Math.hypot(momentum.x, momentum.y);
        const step = Math.max(13 * dpr, Math.min(28 * dpr, 32 * dpr - speed * 0.018));
        const count = Math.max(1, Math.min(14, Math.floor(distance / step) + 1));

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

        const moving = active && time - lastMoveAt < 130;
        if (!moving) {
          active = false;
        }

        smooth.px = smooth.x;
        smooth.py = smooth.y;
        smooth.x += (pointer.x - smooth.x) * 0.105;
        smooth.y += (pointer.y - smooth.y) * 0.105;
        momentum.x *= moving ? 0.955 : 0.91;
        momentum.y *= moving ? 0.955 : 0.91;

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
          const bridgeLife = Math.min(previousLife, currentLife);
          const bridgeShrink = moving ? 1 : Math.max(0.08, Math.pow(bridgeLife, 0.58));
          const alpha = bridgeLife * (moving ? 0.34 : 0.29);
          if (alpha <= 0.01) {
            continue;
          }
          trailCtx.globalAlpha = alpha;
          trailCtx.strokeStyle = "#ffffff";
          trailCtx.lineWidth = Math.max(6 * dpr, Math.min(previous.radius, current.radius) * 0.56 * bridgeShrink);
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
          particle.vx *= 0.962;
          particle.vy *= 0.962;
          particle.stretch *= 0.965;
          particle.angle += particle.swirl * 0.55;

          const stillnessShrink = moving ? 1 : Math.max(0.08, Math.pow(lifeRatio, 0.58));
          const alpha = Math.pow(lifeRatio, moving ? 1.08 : 1.18) * 0.95;
          const radius = particle.radius * (0.92 + calm * 0.08) * stillnessShrink;
          const stretchX = radius * (1.04 + particle.stretch * lifeRatio);
          const stretchY = radius * (0.94 + calm * 0.08);

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
          const headRadius = (96 + headSpeed * 108) * dpr;
          trailCtx.save();
          trailCtx.translate(smooth.x, smooth.y);
          trailCtx.rotate(flowAngle);
          trailCtx.scale(headRadius * (1.12 + headSpeed * 0.24), headRadius * 0.96);
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
        maskCtx.filter = `blur(${Math.max(4.8, 7.2 * dpr)}px) contrast(174%)`;
        maskCtx.drawImage(trailCanvas, 0, 0);
        maskCtx.restore();

        ctx.clearRect(0, 0, width, height);
        ctx.save();
        ctx.filter = `blur(${Math.max(0.55, 0.85 * dpr)}px)`;
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

        drawRevealText(ctx, screen, dpr);

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
