(() => {
  const stage = document.querySelector("[data-portal-stage]");
  const cover = document.querySelector("[data-portal-cover]");

  if (!stage || !cover) {
    return;
  }

  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  let ticking = false;

  const clamp = (value, min, max) => Math.min(Math.max(value, min), max);
  const easeInOut = (value) => value * value * (3 - 2 * value);

  const setProgress = () => {
    ticking = false;

    if (reduceMotion.matches) {
      stage.style.setProperty("--portal-intro-opacity", "1");
      stage.style.setProperty("--portal-intro-y", "0px");
      cover.style.setProperty("--portal-cover-scale", "1");
      cover.style.setProperty("--portal-cover-x", "0px");
      cover.style.setProperty("--portal-cover-y", "0px");
      cover.style.setProperty("--portal-cover-radius", "0px");
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
