import type { ReactNode } from "react";
import { useStartScenario } from "../hooks";
import type { ScenarioSummary } from "../types";
import { ScenarioCard } from "./ScenarioCard";

interface Props {
  title: string;
  scenarios: ScenarioSummary[] | null;
  error: string | null;
  action?: ReactNode;
}

export function ScenarioSection({ title, scenarios, error, action }: Props) {
  const { start, error: startError } = useStartScenario();
  return (
    <section className="section">
      <div className="section__head">
        <h2 className="section__title">
          {title}
          {scenarios && <span className="count-badge">{scenarios.length}</span>}
        </h2>
        {action}
      </div>
      {(error || startError) && <p className="error">{error ?? startError}</p>}
      {!scenarios && !error && <p className="muted">Загрузка…</p>}
      {scenarios?.length === 0 && <p className="muted">В этой категории пока нет сценариев.</p>}
      <div className="scenario-grid">
        {scenarios?.map((scenario) => (
          <ScenarioCard key={scenario.id} scenario={scenario} onStart={start} />
        ))}
      </div>
    </section>
  );
}
