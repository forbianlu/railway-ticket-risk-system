(() => {
  const stage = document.querySelector("[data-portal-stage]");
  const cover = document.querySelector("[data-portal-cover]");
  const brandSpread = document.querySelector("[data-brand-spread]");
  const brandLeftCard = brandSpread ? brandSpread.querySelector('[data-brand-card="left"]') : null;
  const brandRightCard = brandSpread ? brandSpread.querySelector('[data-brand-card="right"]') : null;
  const primaryButton = document.querySelector(".main-portal-primary");
  const capabilityStack = document.querySelector(".main-portal-card-stack");
  const capabilityCards = capabilityStack
    ? Array.from(capabilityStack.querySelectorAll(".main-portal-feature-card"))
    : [];
  const caseSections = Array.from(document.querySelectorAll("[data-case-reveal]"));

  if (!stage || !cover) {
    return;
  }

  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  let ticking = false;

  const clamp = (value, min, max) => Math.min(Math.max(value, min), max);
  const easeInOut = (value) => value * value * (3 - 2 * value);

  const setupPrimaryButtonLiquid = () => {
    if (!primaryButton || reduceMotion.matches || primaryButton.dataset.liquidReady === "true") {
      return;
    }

    primaryButton.dataset.liquidReady = "true";
    if (!primaryButton.querySelector(".main-portal-primary-label")) {
      const text = primaryButton.textContent.trim();
      const underLabel = document.createElement("span");
      const label = document.createElement("span");
      underLabel.className = "main-portal-primary-label main-portal-primary-label--under";
      underLabel.setAttribute("aria-hidden", "true");
      underLabel.textContent = text;
      label.className = "main-portal-primary-label main-portal-primary-label--top";
      label.textContent = text;
      primaryButton.textContent = "";
      primaryButton.appendChild(underLabel);
      primaryButton.appendChild(label);
    }

    const canvas = document.createElement("canvas");
    canvas.className = "main-portal-primary-liquid";
    canvas.setAttribute("aria-hidden", "true");
    primaryButton.insertBefore(canvas, primaryButton.firstChild);

    const ctx = canvas.getContext("2d", { alpha: true });
    if (!ctx) {
      canvas.remove();
      return;
    }

    const pointer = { x: 0, y: 0, px: 0, py: 0 };
    const particles = [];
    let initialized = false;
    let active = false;
    let rafId = 0;
    let width = 1;
    let height = 1;
    let dpr = 1;

    const resizeCanvas = () => {
      const rect = primaryButton.getBoundingClientRect();
      dpr = Math.min(window.devicePixelRatio || 1, 1.75);
      width = Math.max(1, Math.round(rect.width * dpr));
      height = Math.max(1, Math.round(rect.height * dpr));
      canvas.width = width;
      canvas.height = height;
      canvas.style.width = `${rect.width}px`;
      canvas.style.height = `${rect.height}px`;
      ctx.clearRect(0, 0, width, height);
      particles.length = 0;
    };

    const requestFrame = () => {
      if (!rafId) {
        rafId = window.requestAnimationFrame(render);
      }
    };

    const addParticle = (x, y, speed) => {
      const speedUnit = Math.min(1, speed / (80 * dpr));
      const radius = (54 + speedUnit * 54 + Math.random() * 18) * dpr;
      const life = 128 + Math.round(speedUnit * 72) + Math.round(Math.random() * 32);
      particles.push({
        x,
        y,
        vx: (Math.random() - 0.5) * (1.2 + speedUnit * 2.4) * dpr,
        vy: (Math.random() - 0.5) * (1.2 + speedUnit * 2.4) * dpr,
        radius,
        life,
        maxLife: life,
      });
      if (particles.length > 64) {
        particles.splice(0, particles.length - 64);
      }
    };

    const updatePointer = (event) => {
      const rect = primaryButton.getBoundingClientRect();
      const x = (event.clientX - rect.left) * dpr;
      const y = (event.clientY - rect.top) * dpr;

      if (!initialized) {
        pointer.x = x;
        pointer.y = y;
        pointer.px = x;
        pointer.py = y;
        initialized = true;
        addParticle(x, y, 0);
      } else {
        pointer.px = pointer.x;
        pointer.py = pointer.y;
        pointer.x = x;
        pointer.y = y;
        const distance = Math.hypot(pointer.x - pointer.px, pointer.y - pointer.py);
        const count = Math.max(1, Math.min(5, Math.floor(distance / (16 * dpr)) + 1));
        for (let index = 0; index < count; index += 1) {
          const t = (index + 1) / count;
          addParticle(
            pointer.px + (pointer.x - pointer.px) * t,
            pointer.py + (pointer.y - pointer.py) * t,
            distance
          );
        }
      }

      active = true;
      requestFrame();
    };

    function render() {
      rafId = 0;
      ctx.clearRect(0, 0, width, height);
      ctx.globalCompositeOperation = "source-over";

      for (let index = particles.length - 1; index >= 0; index -= 1) {
        const particle = particles[index];
        particle.life -= 1;
        if (particle.life <= 0) {
          particles.splice(index, 1);
          continue;
        }

        const lifeRatio = particle.life / particle.maxLife;
        particle.x += particle.vx;
        particle.y += particle.vy;
        particle.vx *= 0.94;
        particle.vy *= 0.94;

        const radius = particle.radius * (0.72 + (1 - lifeRatio) * 0.22);
        const alpha = Math.pow(lifeRatio, 0.9);
        const gradient = ctx.createRadialGradient(particle.x, particle.y, 0, particle.x, particle.y, radius);
        gradient.addColorStop(0, `rgba(96, 165, 250, ${0.68 * alpha})`);
        gradient.addColorStop(0.45, `rgba(37, 99, 235, ${0.72 * alpha})`);
        gradient.addColorStop(1, "rgba(37, 99, 235, 0)");
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(particle.x, particle.y, radius, 0, Math.PI * 2);
        ctx.fill();
      }

      if (!particles.length) {
        active = false;
      }

      if (active || particles.length) {
        requestFrame();
      } else {
        ctx.clearRect(0, 0, width, height);
      }
    }

    resizeCanvas();
    primaryButton.addEventListener("pointerenter", updatePointer);
    primaryButton.addEventListener("pointermove", updatePointer);
    primaryButton.addEventListener("pointerleave", () => {
      active = false;
      initialized = false;
      requestFrame();
    });
    primaryButton.addEventListener("pointercancel", () => {
      active = false;
      initialized = false;
      requestFrame();
    });
    window.addEventListener("resize", resizeCanvas);
  };

  const setBrandSpread = () => {
    if (!brandSpread || !brandLeftCard || !brandRightCard) {
      return;
    }

    if (reduceMotion.matches) {
      brandSpread.style.setProperty("--brand-left-x", "0px");
      brandSpread.style.setProperty("--brand-right-x", "0px");
      brandSpread.style.setProperty("--brand-card-y", "0px");
      brandSpread.style.setProperty("--brand-card-scale", "1");
      brandSpread.style.setProperty("--brand-card-opacity", "1");
      brandSpread.style.setProperty("--brand-copy-opacity", "1");
      brandSpread.style.setProperty("--brand-copy-y", "0px");
      brandSpread.classList.add("is-brand-spread-complete");
      return;
    }

    const rect = brandSpread.getBoundingClientRect();
    const cardStartOffset = Math.max(0, brandLeftCard.offsetTop);
    const cardEntryTop = rect.top + cardStartOffset;
    const entryRange = Math.max(window.innerHeight - cardStartOffset, 1);
    const raw = clamp((window.innerHeight - cardEntryTop) / entryRange, 0, 1);
    const progress = easeInOut(raw);
    const cardGap = Math.max(0, brandRightCard.offsetLeft - (brandLeftCard.offsetLeft + brandLeftCard.offsetWidth));
    const travel = cardGap / 2;
    const copyRaw = clamp((raw - 0.12) / 0.58, 0, 1);
    const copyProgress = easeInOut(copyRaw);
    const lift = window.innerWidth < 760 ? 24 : 18;

    brandSpread.style.setProperty("--brand-left-x", `${travel * (1 - progress)}px`);
    brandSpread.style.setProperty("--brand-right-x", `${travel * (progress - 1)}px`);
    brandSpread.style.setProperty("--brand-card-y", `${(1 - progress) * lift}px`);
    brandSpread.style.setProperty("--brand-card-scale", String(0.96 + progress * 0.04));
    brandSpread.style.setProperty("--brand-card-opacity", (0.88 + progress * 0.12).toFixed(3));
    brandSpread.style.setProperty("--brand-copy-opacity", (0.16 + copyProgress * 0.84).toFixed(3));
    brandSpread.style.setProperty("--brand-copy-y", `${(1 - copyProgress) * 30}px`);
    brandSpread.classList.toggle("is-brand-spread-complete", raw >= 0.995);
  };

  const clearCapabilityCardMotion = (card) => {
    card.style.removeProperty("--card-enter-x");
    card.style.removeProperty("--card-enter-y");
    card.style.removeProperty("--card-enter-rotate");
    card.style.removeProperty("--card-enter-scale");
    card.style.removeProperty("--card-enter-opacity");
  };

  const setCapabilityStack = () => {
    if (!capabilityStack || !capabilityCards.length) {
      return;
    }

    if (reduceMotion.matches) {
      capabilityStack.classList.remove("is-card-stack-entering");
      capabilityCards.forEach(clearCapabilityCardMotion);
      return;
    }

    const compact = window.innerWidth < 760;
    const entryTravel = compact
      ? Math.min(180, Math.max(112, window.innerWidth * 0.38))
      : Math.min(430, Math.max(230, window.innerWidth * 0.25));
    const verticalTravel = compact ? 28 : 42;
    const rotations = compact ? [12, -10, 9, -8] : [18, -16, 13, -12];
    const rect = capabilityStack.getBoundingClientRect();
    const startTop = window.innerHeight * 0.98;
    const settleTop = window.innerHeight * 0.5 - rect.height * 0.5;
    const entryRaw = clamp((startTop - rect.top) / Math.max(startTop - settleTop, 1), 0, 1);
    const exitTravel = entryTravel * 1.5;
    const exitStartTop = settleTop - rect.height * (compact ? 0.46 : 0.72);
    const exitRange = Math.max(rect.height * (compact ? 0.34 : 0.28), window.innerHeight * (compact ? 0.26 : 0.22), 1);
    const exitRaw = clamp((exitStartTop - rect.top) / exitRange, 0, 1);

    if (entryRaw >= 0.998 && exitRaw <= 0.002) {
      capabilityStack.classList.remove("is-card-stack-entering");
      capabilityCards.forEach(clearCapabilityCardMotion);
      return;
    }

    capabilityStack.classList.add("is-card-stack-entering");

    capabilityCards.forEach((card, index) => {
      const entryStagger = index * 0.035;
      const exitStagger = (capabilityCards.length - 1 - index) * 0.035;
      const entryProgress = easeInOut(clamp((entryRaw - entryStagger) / 0.82, 0, 1));
      const exitProgress = easeInOut(clamp((exitRaw - exitStagger) / 0.82, 0, 1));
      const remain = exitRaw > 0.002 ? exitProgress : 1 - entryProgress;
      const horizontalTravel = exitRaw > 0.002 ? exitTravel : entryTravel;
      const sideOffset = horizontalTravel + index * (compact ? 16 : 26);
      const yOffset = (index - 1.5) * verticalTravel * remain;

      card.style.setProperty("--card-enter-x", `${(sideOffset * remain).toFixed(2)}px`);
      card.style.setProperty("--card-enter-y", `${yOffset.toFixed(2)}px`);
      card.style.setProperty("--card-enter-rotate", `${(rotations[index] * remain).toFixed(2)}deg`);
      card.style.setProperty("--card-enter-scale", (1 - remain * 0.1).toFixed(3));
      card.style.setProperty("--card-enter-opacity", "1");
    });
  };

  const revealCaseSections = () => {
    document.body.classList.remove("is-case-reveal-ready");
    caseSections.forEach((section) => {
      section.classList.add("is-case-revealed", "is-case-settled");
    });
  };

  const setupCaseReveal = () => {
    if (!caseSections.length) {
      return;
    }

    if (reduceMotion.matches || !("IntersectionObserver" in window)) {
      revealCaseSections();
      return;
    }

    document.body.classList.add("is-case-reveal-ready");

    const revealCase = (section) => {
      const card = section.querySelector("[data-case-card]");
      section.classList.add("is-case-revealed");

      if (!card) {
        section.classList.add("is-case-settled");
        return;
      }

      const settle = () => section.classList.add("is-case-settled");
      card.addEventListener("animationend", settle, { once: true });
      window.setTimeout(settle, 980);
    };

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            revealCase(entry.target);
            observer.unobserve(entry.target);
          }
        });
      },
      {
        rootMargin: "0px 0px -24% 0px",
        threshold: 0.22,
      }
    );

    caseSections.forEach((section) => {
      if (section.getBoundingClientRect().top < window.innerHeight * 0.78) {
        revealCase(section);
        return;
      }

      observer.observe(section);
    });
  };

  const setProgress = () => {
    ticking = false;

    if (reduceMotion.matches) {
      stage.style.setProperty("--portal-intro-opacity", "1");
      stage.style.setProperty("--portal-intro-y", "0px");
      cover.style.setProperty("--portal-cover-scale", "1");
      cover.style.setProperty("--portal-cover-x", "0px");
      cover.style.setProperty("--portal-cover-y", "0px");
      cover.style.setProperty("--portal-cover-radius", "0px");
      setBrandSpread();
      setCapabilityStack();
      return;
    }

    const rect = stage.getBoundingClientRect();
    const range = Math.max(stage.offsetHeight - window.innerHeight, 1);
    const raw = clamp(-rect.top / range, 0, 1);
    const progress = easeInOut(raw);
    const width = window.innerWidth;
    const height = window.innerHeight;
    const compact = width < 760;
    const finalScale = compact ? 0.72 : width > 1500 ? 0.38 : 0.42;
    const finalX = compact ? 0 : width * -0.21;
    const finalY = compact ? height * 0.22 : height * 0.16;
    const intro = clamp((raw - 0.24) / 0.46, 0, 1);
    const introEase = easeInOut(intro);

    cover.style.setProperty("--portal-cover-scale", String(1 - (1 - finalScale) * progress));
    cover.style.setProperty("--portal-cover-x", `${finalX * progress}px`);
    cover.style.setProperty("--portal-cover-y", `${finalY * progress}px`);
    cover.style.setProperty("--portal-cover-radius", `${30 * progress}px`);
    stage.style.setProperty("--portal-intro-opacity", introEase.toFixed(3));
    stage.style.setProperty("--portal-intro-y", `${(1 - introEase) * 56}px`);
    setBrandSpread();
    setCapabilityStack();
  };

  const requestUpdate = () => {
    if (!ticking) {
      ticking = true;
      window.requestAnimationFrame(setProgress);
    }
  };

  window.addEventListener("scroll", requestUpdate, { passive: true });
  window.addEventListener("resize", requestUpdate);
  reduceMotion.addEventListener("change", () => {
    if (reduceMotion.matches) {
      revealCaseSections();
    }

    requestUpdate();
  });
  setupPrimaryButtonLiquid();
  setupCaseReveal();
  requestUpdate();
})();
