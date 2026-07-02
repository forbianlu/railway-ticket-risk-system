(() => {
  const stage = document.querySelector("[data-portal-stage]");
  const cover = document.querySelector("[data-portal-cover]");
  const brandSpread = document.querySelector("[data-brand-spread]");
  const brandLeftCard = brandSpread ? brandSpread.querySelector('[data-brand-card="left"]') : null;
  const brandRightCard = brandSpread ? brandSpread.querySelector('[data-brand-card="right"]') : null;

  if (!stage || !cover) {
    return;
  }

  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  let ticking = false;

  const clamp = (value, min, max) => Math.min(Math.max(value, min), max);
  const easeInOut = (value) => value * value * (3 - 2 * value);

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
      return;
    }

    const rect = brandSpread.getBoundingClientRect();
    const range = Math.max(brandSpread.offsetHeight - window.innerHeight, 1);
    const raw = clamp(-rect.top / range, 0, 1);
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
  };

  const requestUpdate = () => {
    if (!ticking) {
      ticking = true;
      window.requestAnimationFrame(setProgress);
    }
  };

  window.addEventListener("scroll", requestUpdate, { passive: true });
  window.addEventListener("resize", requestUpdate);
  reduceMotion.addEventListener("change", requestUpdate);
  requestUpdate();
})();
