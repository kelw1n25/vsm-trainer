import { Link } from "react-router";
import { HeroCarousel } from "../components/HeroCarousel";
import { ChevronRightIcon, GridIcon } from "../components/icons";
import { ScenarioSection } from "../components/ScenarioSection";
import { useScenarios } from "../hooks";

export function HomePage() {
  const { scenarios, error } = useScenarios();
  return (
    <>
      <HeroCarousel />
      <ScenarioSection
        title="Сценарии"
        scenarios={scenarios}
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
