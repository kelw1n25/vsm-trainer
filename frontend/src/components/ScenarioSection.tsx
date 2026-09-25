import type { ReactNode } from "react";
import { useStartScenario } from "../hooks";
import type { ScenarioSummary } from "../types";
import { ScenarioCard } from "./ScenarioCard";

interface Props {
  title: string;
  scenarios: ScenarioSummary[] | null;
  error: string | null;
  action?: ReactNode;
  /** Число в бейдже, если показана только часть сценариев. */
  total?: number;
}

export function ScenarioSection({ title, scenarios, error, action, total }: Props) {
  const { start, error: startError } = useStartScenario();
  return (
    <section className="section">
      <div className="section__head">
        <h2 className="section__title">
          {title}
          {scenarios && <span className="count-badge">{total ?? scenarios.length}</span>}
        </h2>
        {action}
      </div>
      {(error || startError) && <p className="error">{error ?? startError}</p>}
      {!scenarios && !error && <p className="muted">Загрузка…</p>}
      {scenarios?.length === 0 && <p className="muted">В этой категории пока нет сценариев.</p>}
      <div className="scenario-grid">
        {scenarios?.map((scenario, index) => (
          <ScenarioCard key={scenario.id} scenario={scenario} index={index} onStart={start} />
        ))}
      </div>
    </section>
  );
}
