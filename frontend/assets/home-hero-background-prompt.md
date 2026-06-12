# 首页 Hero 背景图生成提示词

## 中文提示词

现代高速铁路调度中心风格的抽象网站 Hero 背景，深青绿色和铁路蓝色调，远处有城市夜景轮廓，前景有高速列车光轨、轨道线、数据流、柔和 HUD 网格和运营监控光点，整体专业、可信、干净、高端科技感；无人像、无文字、无 logo；左侧保留充足文字空间，右侧可以有更丰富的轨道和数据视觉，适合铁路客运票务与风控运营平台首页，16:9。

## English Prompt

Abstract hero background for a modern high-speed railway operations center, deep teal green and railway blue palette, distant city night skyline silhouette, foreground high-speed train light trails, rail lines, data streams, subtle HUD grid, operational monitoring nodes, professional, trustworthy, clean, premium technology atmosphere, no people, no text, no logo, generous empty space on the left for website headline, richer rail and data visuals on the right, suitable for a railway ticketing and risk operations platform landing page, 16:9.

## Negative Prompt

people, faces, readable text, logo, watermark, cartoon style, fantasy, cyberpunk clutter, nightclub neon, purple AI SaaS gradient, excessive 3D objects, messy dashboard screenshots, low contrast, blurry typography, dirty yellow background, overexposed glow, crowded composition.

## Recommended Size

16:9, recommended 1920 x 1080 or 2560 x 1440.

## Usage

Generate the image and save it as:

```text
frontend/assets/home-hero-background.png
```

Then replace or layer it into the landing page background in `frontend/styles.css`. The current implementation already uses CSS gradients, grid lines and an inline SVG rail visualization as a temporary high-quality abstract background.
