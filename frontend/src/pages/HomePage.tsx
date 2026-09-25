import { Link } from "react-router";
import { HeroCarousel } from "../components/HeroCarousel";
import { ChevronRightIcon, GridIcon } from "../components/icons";
import { ScenarioSection } from "../components/ScenarioSection";
import { useScenarios } from "../hooks";

// На главной — короткая подборка, полный список в разделе «Сценарии»
const PREVIEW_COUNT = 3;

export function HomePage() {
  const { scenarios, error } = useScenarios();
  return (
    <>
      <HeroCarousel />
      <ScenarioSection
        title="Сценарии"
        scenarios={scenarios?.slice(0, PREVIEW_COUNT) ?? null}
        total={scenarios?.length}
        error={error}
        action={
          <Link to="/scenarios" className="link-action">
            <GridIcon />
            Все сценарии
            <ChevronRightIcon />
          </Link>
        }
      />
    </>
  );
}
