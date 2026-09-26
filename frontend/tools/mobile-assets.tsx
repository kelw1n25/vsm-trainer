/**
 * Генератор ассетов для iOS и Android — не часть сайта, в сборку не попадает.
 *
 * Мобильные приложения показывают тех же персонажей, фоны и иллюстрации, что и сайт. Чтобы не перерисовывать
 * их вручную (и не разойтись с сайтом), страница рендерит настоящие компоненты сайта, а скрипт
 * tools/mobile-assets/export.mjs снимает каждый элемент [data-asset] в картинку.
 * Анимации выключены атрибутом data-reduce-motion: кадр всегда статичный.
 */
import { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "../src/styles.css";
import { StarIcon } from "../src/components/icons";
import { HeroTrain, ScenarioImage } from "../src/components/illustrations";
import { Sprite } from "../src/story/StoryStage";
import { StoryBackground } from "../src/story/StoryStage";
import type { Expression, Look, SceneCharacter } from "../src/types";

interface SpriteSpec {
  name: string;
  uniform: boolean;
  pose: SceneCharacter["pose"];
  expression: Expression;
  hand: SceneCharacter["hand"];
  item: SceneCharacter["item"];
  right: boolean;
}

const UNIFORM: Look = { outfit: "uniform", top: "#1F2E57", bottom: "#18233F", hair: "#2B2320", hair_style: "short", child: false };
const PASSENGER: Look = { outfit: "casual", top: "#56657F", bottom: "#34405A", hair: "#2B2320", hair_style: "short", child: false };
const BACKGROUNDS = ["salon", "business", "salon_evening", "salon_night", "platform", "vestibule", "bistro", "staff_room"];
const SCENARIOS = [
  "boarding-ticket", "pet-and-bicycle", "drunk-passenger", "business-seat-conflict", "noisy-night", "passenger-unwell",
  "panic-attack", "smoke-vestibule", "unattended-item", "lost-child", "business-catering", "first-class-comfort",
  "train-delay", "wheelchair-boarding",
];

// Спрайт — в размер кадра, без позиционирования сцены, тени и затемнения: их приложения рисуют сами
const css = `
  body { margin: 0; background: transparent; }
  body::before, body::after { display: none; }
  .asset { position: relative; display: inline-block; margin: 8px; }
  .asset .story-sprite { position: static; transform: none; height: 1040px; width: auto; filter: none; }
  .asset .story-sprite__svg { filter: none; }
  .asset-bg { width: 430px; height: 932px; overflow: hidden; }
  .asset-illustration { width: 320px; height: 300px; }
  .asset-illustration .scenario-image { height: 100%; }
  .asset-hero { width: 1000px; height: 300px; }
  .asset-hero .hero__train { position: static; width: 100%; height: 100%; }
  .layer-static .hero__parallax, .layer-static .hero__train-drive { visibility: hidden; }
  .layer-parallax .hero__train > :not(.hero__parallax):not(defs) { visibility: hidden; }
  .layer-drive .hero__train > :not(.hero__train-drive):not(defs) { visibility: hidden; }
`;

function Assets() {
  const [sprites, setSprites] = useState<SpriteSpec[]>([]);
  useEffect(() => {
    fetch("/tools/sprites.json").then((response) => response.json()).then(setSprites);
  }, []);
  return (
    <>
      <style>{css}</style>
      {sprites.map((spec) => (
        <div key={spec.name} className="asset" data-asset={spec.name} data-kind="sprite">
          <Sprite
            character={{ id: "x", position: spec.right ? "right" : "left", expression: spec.expression, pose: spec.pose, hand: spec.hand, item: spec.item }}
            look={spec.uniform ? UNIFORM : PASSENGER}
            expression={spec.expression}
            state="active"
          />
        </div>
      ))}
      {BACKGROUNDS.map((name) => (
        <div key={name} className="asset asset-bg" data-asset={`bg_${name}`} data-kind="background">
          <StoryBackground name={name} />
        </div>
      ))}
      {SCENARIOS.map((id) => (
        <div key={id} className="asset asset-illustration" data-asset={`scenario_${id.replaceAll("-", "_")}`} data-kind="illustration">
          <ScenarioImage scenarioId={id} category="" />
        </div>
      ))}
      {["static", "parallax", "drive"].map((layer) => (
        <div key={layer} className={`asset asset-hero layer-${layer}`} data-asset={`hero_${layer}`} data-kind="hero">
          <HeroTrain />
        </div>
      ))}
      <div className="asset" data-asset="star" data-kind="icon">
        <StarIcon size={56} />
      </div>
    </>
  );
}

createRoot(document.getElementById("root")!).render(<Assets />);
