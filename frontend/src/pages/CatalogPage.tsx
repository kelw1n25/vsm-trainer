import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { api, ApiError } from "../api";
import { categoryLabels } from "../labels";
import type { ScenarioSummary } from "../types";

export function CatalogPage() {
  const navigate = useNavigate();
  const [scenarios, setScenarios] = useState<ScenarioSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .scenarios()
      .then(setScenarios)
      .catch((e) => setError(e instanceof ApiError ? e.message : "Не удалось загрузить сценарии"));
  }, []);

  async function start(scenarioId: string) {
    try {
      const run = await api.startRun(scenarioId);
      navigate(`/runs/${run.id}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Не удалось начать сценарий");
    }
  }

  return (
    <>
      <h1>Сценарии</h1>
      {error && <p className="error">{error}</p>}
      {scenarios === null && !error && <p className="muted">Загрузка…</p>}
      <div className="grid">
        {scenarios?.map((scenario) => (
          <article key={scenario.id} className="card scenario-card">
            <div className="scenario-card__tags">
              <span className={`tag tag--${scenario.category}`}>{categoryLabels[scenario.category]}</span>
              {scenario.demo && <span className="tag tag--demo">Демо</span>}
            </div>
            <h2>{scenario.title}</h2>
            <p className="muted">
              {scenario.route} · {scenario.service_class}
            </p>
            <p className="muted">Сложность: {"●".repeat(scenario.difficulty) + "○".repeat(3 - scenario.difficulty)}</p>
            <button className="button" onClick={() => start(scenario.id)}>
              Начать
            </button>
          </article>
        ))}
      </div>
    </>
  );
}
