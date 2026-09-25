import { useState } from "react";
import { Link } from "react-router";
import { ChevronLeftIcon } from "../components/icons";
import { ScenarioSection } from "../components/ScenarioSection";
import { useScenarios } from "../hooks";
import { categoryLabels } from "../labels";

/** Все сценарии с фильтром по типу ситуации. */
export function ScenariosPage() {
  const { scenarios, error } = useScenarios();
  const [category, setCategory] = useState<string | null>(null);
  const categories = [...new Set(scenarios?.map((s) => s.category))];
  const visible = scenarios?.filter((s) => category === null || s.category === category) ?? null;

  return (
    <>
      <Link to="/" className="back-link">
        <ChevronLeftIcon /> На главную
      </Link>
      <div className="chips" role="tablist" aria-label="Тип ситуации">
        <button role="tab" aria-selected={category === null} className="chip" onClick={() => setCategory(null)}>
          Все
        </button>
        {categories.map((code) => (
          <button
            key={code}
            role="tab"
            aria-selected={category === code}
            className="chip"
            onClick={() => setCategory(code)}
          >
            {categoryLabels[code] ?? code}
          </button>
        ))}
      </div>
      <ScenarioSection title="Все сценарии" scenarios={visible} error={error} />
    </>
  );
}
