import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { api } from "../api";
import { DifficultyIndicator } from "../components/DifficultyIndicator";
import { ArrowRightIcon, ChevronLeftIcon, PinIcon } from "../components/icons";
import { ScenarioImage } from "../components/illustrations";
import { useScenarios, useStartScenario } from "../hooks";
import { categoryLabels, outcomeLabels } from "../labels";
import type { HistoryItem, RunStatus } from "../types";

const outcomeRank: Record<RunStatus, number> = { success: 3, partial: 2, failure: 1, in_progress: 0 };

/** Страница сценария: описание, что ждёт внутри, свои прошлые попытки. */
export function ScenarioPage() {
  const { scenarioId } = useParams() as { scenarioId: string };
  const { scenarios, error } = useScenarios();
  const { start, error: startError } = useStartScenario();
  const [attempts, setAttempts] = useState<HistoryItem[] | null>(null);

  useEffect(() => {
    api
      .profile()
      .then((profile) => setAttempts(profile.history.filter((item) => item.scenario_id === scenarioId)))
      .catch(() => setAttempts([]));
  }, [scenarioId]);

  if (error) return <p className="error">{error}</p>;
  if (!scenarios) return <p className="muted">Загрузка…</p>;
  const scenario = scenarios.find((s) => s.id === scenarioId);
  if (!scenario) {
    return (
      <div className="card empty">
        <h1>Сценарий не найден</h1>
        <Link to="/scenarios" className="button">
          К списку сценариев
        </Link>
      </div>
    );
  }

  const best = attempts?.reduce<HistoryItem | null>(
    (top, item) => (!top || outcomeRank[item.outcome] > outcomeRank[top.outcome] ? item : top),
    null,
  );

  return (
    <div className="stack">
      <Link to="/scenarios" className="back-link">
        <ChevronLeftIcon /> Все сценарии
      </Link>

      <section className="card scenario-detail">
        <div className="scenario-detail__body">
          <div className="tags">
            <span className="tag tag--category">{categoryLabels[scenario.category] ?? scenario.category}</span>
            {scenario.demo && <span className="tag">Демо</span>}
          </div>
          <h1 className="page-title">{scenario.title}</h1>
          <p className="route">
            <PinIcon />
            {scenario.route} · {scenario.service_class}
          </p>
          <DifficultyIndicator level={scenario.difficulty} />
          <p className="scenario-detail__text">{scenario.description}</p>
          <div className="kpis kpis--compact">
            <div className="kpi">
              <span className="kpi__label">Попыток</span>
              <strong className="kpi__value">{attempts?.length ?? "—"}</strong>
            </div>
            <div className="kpi">
              <span className="kpi__label">Лучший результат</span>
              <strong className="kpi__value">{best ? outcomeLabels[best.outcome] : "—"}</strong>
            </div>
          </div>
          {startError && <p className="error">{startError}</p>}
          <button className="button button--large" onClick={() => start(scenario.id)}>
            Начать сценарий <ArrowRightIcon />
          </button>
        </div>
        <div className="scenario-detail__image">
          <ScenarioImage scenarioId={scenario.id} category={scenario.category} />
        </div>
      </section>

      <section className="features">
        <div className="card feature">
          <strong>⏱ Решения под таймером</strong>
          <span className="muted">Время контролирует сервер: не успели — ситуация развивается без вас.</span>
        </div>
        <div className="card feature">
          <strong>⚖️ Две шкалы</strong>
          <span className="muted">Лояльность пассажира и рейтинг безопасности. Падение любой до нуля — провал.</span>
        </div>
        <div className="card feature">
          <strong>📋 Разбор после финала</strong>
          <span className="muted">Каждое решение, его последствия и лучший вариант в этой точке.</span>
        </div>
      </section>

      {attempts && attempts.length > 0 && (
        <section className="card">
          <h2>Ваши попытки</h2>
          <ul className="history">
            {attempts.map((item) => (
              <li key={item.run_id}>
                <span>{new Date(item.finished_at).toLocaleString("ru-RU")}</span>
                <span className={`outcome outcome--${item.outcome}`}>{outcomeLabels[item.outcome]}</span>
                <span>+{item.xp_earned} XP</span>
                <Link to={`/runs/${item.run_id}/debrief`}>Разбор</Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
