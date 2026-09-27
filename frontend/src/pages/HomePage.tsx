import { AboutVsm } from "../components/AboutVsm";
import { HeroCarousel } from "../components/HeroCarousel";

// Главная — приветствие и общая картина ВСМ; сценарии — в своём разделе
export function HomePage() {
  return (
    <>
      <HeroCarousel />
      <AboutVsm />
    </>
  );
}
