/**
 * Снимает ассеты со страницы-генератора (frontend/tools/mobile-assets.html) и раскладывает их
 * в ресурсы приложений. Нужны: запущенный Vite (`cd frontend && npx vite --port 5174`),
 * `cwebp` (brew install webp) и `npm install && npx playwright install chromium` в этом каталоге.
 *
 *   python tools/mobile-assets/combos.py > frontend/tools/sprites.json
 *   node tools/mobile-assets/export.mjs
 */
import { execFileSync } from "node:child_process";
import { copyFileSync, mkdirSync, mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { chromium } from "playwright";

const ROOT = resolve(import.meta.dirname, "../..");
const PAGE = process.env.ASSETS_URL ?? "http://localhost:5174/tools/mobile-assets.html";
const IOS = join(ROOT, "ios/VSMKit/Sources/VSMFeatures/Resources/Images");
const ANDROID = join(ROOT, "android/app/src/main/res/drawable-nodpi");
const TMP = mkdtempSync(join(tmpdir(), "vsm-assets-"));

// Масштаб снимка под размер на экране телефона: фон — весь экран @3x, иллюстрация карточки — @3x
const KINDS = {
  sprite: { scale: 1, quality: 90, alpha: true },
  background: { scale: 3, quality: 82, alpha: false },
  illustration: { scale: 3, quality: 90, alpha: false },
  hero: { scale: 2, quality: 90, alpha: true },
  icon: { scale: 3, quality: 92, alpha: true },
};

// Фото из датасета, которые экраны показывают как есть (вход, справочник)
const PHOTOS = {
  photo_train_city: "train-city.jpg",
  photo_service_classes: "service-classes.jpg",
  photo_service_classes_layout: "service-classes-layout.jpg",
  photo_rolling_stock: "rolling-stock.jpg",
};

mkdirSync(IOS, { recursive: true });
mkdirSync(ANDROID, { recursive: true });

function publish(name, png, { quality, alpha }) {
  const webp = join(TMP, `${name}.webp`);
  execFileSync("cwebp", ["-quiet", "-q", String(quality), ...(alpha ? ["-alpha_q", "100", "-exact"] : []), png, "-o", webp]);
  copyFileSync(webp, join(IOS, `${name}.webp`));
  copyFileSync(webp, join(ANDROID, `${name}.webp`));
}

const browser = await chromium.launch();
let total = 0;
for (const [kind, options] of Object.entries(KINDS)) {
  const context = await browser.newContext({ viewport: { width: 1400, height: 1200 }, deviceScaleFactor: options.scale });
  const page = await context.newPage();
  await page.goto(PAGE);
  await page.waitForSelector("[data-kind='sprite']");
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(500);
  for (const element of await page.locator(`[data-kind='${kind}']`).all()) {
    const name = await element.getAttribute("data-asset");
    const target = kind === "sprite" ? element.locator(".story-sprite__svg") : kind === "hero" ? element.locator("svg").first() : element;
    const png = join(TMP, `${name}.png`);
    await target.screenshot({ path: png, omitBackground: options.alpha });
    publish(name, png, options);
    total += 1;
  }
  await context.close();
}
await browser.close();

for (const [name, file] of Object.entries(PHOTOS)) {
  publish(name, join(ROOT, "frontend/public/media", file), { quality: 85, alpha: false });
  total += 1;
}
console.log(`Экспортировано ассетов: ${total} → ${IOS}, ${ANDROID}`);
